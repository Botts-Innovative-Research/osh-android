package org.sensorhub.android.ui.screens.systems

import android.content.Context
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.sensorhub.android.data.servers.ServerProfileItem
import org.sensorhub.android.data.servers.ServerProfileRepository
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class NodeRepository private constructor(context: Context) {
    private val profiles = ServerProfileRepository.getInstance(context.applicationContext)
    private val preferences = context.applicationContext.getSharedPreferences("systems_selection", Context.MODE_PRIVATE)
    private val mutableSelectedIds = MutableStateFlow(preferences.getStringSet("server_ids", emptySet())!!.toSet())
    val selectedIds = mutableSelectedIds.asStateFlow()
    private val mutableSavedServerOptions = MutableStateFlow<List<SystemsServerOption>>(emptyList())
    val savedServerOptions = mutableSavedServerOptions.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val jobs = mutableMapOf<String, Job>()
    private val mutableSelectedServerStates = MutableStateFlow<List<SystemsServerState>>(emptyList())
    val selectedServerStates = mutableSelectedServerStates.asStateFlow()

    /** Restore selected servers on entering Systems, without fetching again on recomposition. */
    fun open() {
        val saved = profiles.all
        mutableSavedServerOptions.value = saved.map { SystemsServerOption(it.id, it.serverName, it.endpointUrl) }
        val availableIds = saved.map { it.id }.toSet()
        // A deleted profile cannot be selected; profile enabled flags never affect this selection.
        val retained = mutableSelectedIds.value.intersect(availableIds)
        if (retained != mutableSelectedIds.value) saveSelection(retained)
        mutableSelectedServerStates.value.filter { it.id !in retained }.forEach { remove(it.id) }
        retained.forEach { id ->
            if (mutableSelectedServerStates.value.none { it.id == id }) refresh(id)
        }
        val savedById = saved.associateBy { it.id }
        mutableSelectedServerStates.value = mutableSelectedServerStates.value.map { state ->
            val profile = savedById[state.id]
            state.copy(
                name = profile?.serverName ?: state.name,
                endpointUrl = profile?.endpointUrl ?: state.endpointUrl
            )
        }
    }

    fun setSelected(id: String, selected: Boolean) {
        if (profiles.getById(id) == null || (id in mutableSelectedIds.value) == selected) return
        saveSelection(if (selected) mutableSelectedIds.value + id else mutableSelectedIds.value - id)
        if (selected) refresh(id) else remove(id)
    }

    private fun saveSelection(ids: Set<String>) {
        preferences.edit().putStringSet("server_ids", ids.toSet()).apply()
        mutableSelectedIds.value = ids
    }

    fun remove(id: String) {
        jobs.remove(id)?.cancel()
        mutableSelectedServerStates.value = mutableSelectedServerStates.value.filterNot { it.id == id }
    }

    fun refresh(id: String) {
        val profile = profiles.getById(id)?.takeIf { id in mutableSelectedIds.value } ?: return
        fetch(profile.copy(password = profiles.getPassword(id),
            clientId = profiles.getOAuthClientId(id),
            clientSecret = profiles.getOAuthClientSecret(id),
            tokenEndpoint = profiles.getOAuthTokenEndpoint(id)))
    }

    suspend fun getDatastream(serverId: String, datastreamId: String): JsonObject = withContext(Dispatchers.IO) {
        val profile = requireNotNull(profiles.getById(serverId)) { "Server profile no longer exists" }
        try {
            NodeResourceClient.create(profile.endpointUrl, profile.username,
                profiles.getPassword(serverId).toCharArray()).use { client ->
                client.getDatastream(datastreamId).awaitResult()
            }
        } finally {
        }
    }

    suspend fun getControlstream(serverId: String, controlstreamId: String): JsonObject = withContext(Dispatchers.IO) {
        val profile = requireNotNull(profiles.getById(serverId)) { "Server profile no longer exists" }
        NodeResourceClient.create(profile.endpointUrl, profile.username,
            profiles.getPassword(serverId).toCharArray()).use { client ->
            client.getControlstream(controlstreamId).awaitResult()
        }
    }

    private fun fetch(profile: ServerProfileItem) {
        jobs.remove(profile.id)?.cancel()
        val state = SystemsServerState(profile.id, profile.serverName, endpointUrl = profile.endpointUrl)
        mutableSelectedServerStates.value = mutableSelectedServerStates.value.filterNot { it.id == profile.id } + state
        jobs[profile.id] = scope.launch {
            var client: NodeResourceClient? = null
            try {
                client = NodeResourceClient.create(profile.endpointUrl, profile.username,
                    profile.password.toCharArray())
                val resourceClient = client
                supervisorScope {
                    ResourceKind.values().forEach { kind ->
                        launch {
                            val result = try {
                                val items = withContext(Dispatchers.IO) {
                                    val future = when (kind) {
                                        ResourceKind.SYSTEMS -> resourceClient.getSystems()
                                        ResourceKind.DATASTREAMS -> resourceClient.getDataStreams()
                                        ResourceKind.CONTROLSTREAMS -> resourceClient.getControlStreams()
                                    }
                                    future.awaitResult().map(SystemsResource::fromJson).distinctBy { it.id }
                                }
                                CollectionState(items, loading = false)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                CollectionState(loading = false, error = errorMessage(e))
                            }
                            ensureActive()
                            update(profile.id) { it.copy(collections = it.collections + (kind to result)) }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                update(profile.id) { it.copy(collections = ResourceKind.values().associateWith {
                    CollectionState(loading = false, error = errorMessage(e))
                }) }
            } finally {
                client?.close()
            }
        }
    }

    private fun update(id: String, transform: (SystemsServerState) -> SystemsServerState) {
        mutableSelectedServerStates.value = mutableSelectedServerStates.value.map { if (it.id == id) transform(it) else it }
    }

    private fun errorMessage(error: Throwable): String {
        var cause = error
        while ((cause is CompletionException || cause is ExecutionException) && cause.cause != null) cause = cause.cause!!
        val code = Regex("(?:HTTP error |OAuth HTTP )(\\d{3})").find(cause.message.orEmpty())?.groupValues?.get(1)
        return when (code) {
            "401" -> "Authentication failed (401). Check the server credentials."
            "403" -> "Access denied (403)."
            null -> "Unable to load resources. Check the connection and server settings."
            else -> "Server returned HTTP $code."
        }
    }

    companion object {
        @Volatile private var instance: NodeRepository? = null
        fun getInstance(context: Context): NodeRepository = instance ?: synchronized(this) {
            instance ?: NodeRepository(context).also { instance = it }
        }
    }
}

private suspend fun <T> CompletableFuture<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    whenComplete { value, error ->
        if (error == null) continuation.resume(value) else continuation.resumeWithException(error)
    }
    continuation.invokeOnCancellation { cancel(true) }
}
