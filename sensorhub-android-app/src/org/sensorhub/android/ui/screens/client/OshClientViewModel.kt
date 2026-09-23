package org.sensorhub.android.ui.screens.client

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import org.sensorhub.android.data.client.OshMapStore
import org.sensorhub.android.data.client.RemoteControlStream
import org.sensorhub.android.data.client.RemoteNodeState
import org.sensorhub.android.data.client.RemoteSystem
import org.sensorhub.android.data.client.RemoteVisualization
import org.sensorhub.android.data.servers.ServerProfileItem
import org.sensorhub.android.data.servers.ServerProfileRepository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class OshClientViewModel(application: Application) : AndroidViewModel(application) {
    private val profiles = ServerProfileRepository.getInstance(application)
    private val http = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    private val sockets = ConcurrentHashMap<String, WebSocket>()
    private val videoRenderers = ConcurrentHashMap<String, VideoStream>()

    private val _nodes = MutableStateFlow<List<RemoteNodeState>>(emptyList())
    val nodes: StateFlow<List<RemoteNodeState>> = _nodes.asStateFlow()

    private val _visibleProfileIds = MutableStateFlow<Set<String>>(emptySet())
    val visibleProfileIds: StateFlow<Set<String>> = _visibleProfileIds.asStateFlow()

    private val _enabledLocations = MutableStateFlow<Set<String>>(emptySet())
    val enabledLocations: StateFlow<Set<String>> = _enabledLocations.asStateFlow()

    private val _videoFrames = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val videoFrames: StateFlow<Map<String, Bitmap>> = _videoFrames.asStateFlow()

    private val _enabledVideos = MutableStateFlow<Set<String>>(emptySet())
    val enabledVideos: StateFlow<Set<String>> = _enabledVideos.asStateFlow()

    private val _videoErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val videoErrors: StateFlow<Map<String, String>> = _videoErrors.asStateFlow()

    init {
        refreshProfiles()
    }

    fun refreshProfiles() {
        val enabledProfiles = profiles.enabled
        val currentNodes = _nodes.value.associateBy { it.profileId }
        val enabledIds = enabledProfiles.mapTo(mutableSetOf()) { it.id }

        // Refreshing the profile list must not discard a completed discovery or
        // close a stream that is feeding the map while this screen is away.
        _nodes.value = enabledProfiles.map { profile ->
            currentNodes[profile.id]?.copy(
                name = profile.serverName,
                endpointUrl = profile.endpointUrl,
            ) ?: RemoteNodeState(profile.id, profile.serverName, profile.endpointUrl)
        }
        _visibleProfileIds.update { selected ->
            (selected intersect enabledIds).ifEmpty { enabledIds }
        }
    }

    fun setProfileVisible(profileId: String, visible: Boolean) {
        _visibleProfileIds.update { selected ->
            if (visible) selected + profileId else selected - profileId
        }
    }

    fun discover(profileId: String) {
        val profile = profiles.getById(profileId) ?: return
        updateNode(profileId) { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { discoverNode(profile) } }
                .onSuccess { systems ->
                    OshMapStore.replaceSystemLocations(profileId, systems)
                    updateNode(profileId) { it.copy(loading = false, systems = systems) }
                }
                .onFailure { error ->
                    updateNode(profileId) {
                        it.copy(loading = false, error = error.message ?: "Unable to connect")
                    }
                }
        }
    }

    fun setVideoEnabled(profileId: String, visualization: RemoteVisualization, enabled: Boolean) {
        if (!enabled) {
            sockets.remove(visualization.dataStreamId)?.close(1000, "disabled")
            _enabledVideos.update { it - visualization.dataStreamId }
            _videoErrors.update { it - visualization.dataStreamId }
            _videoFrames.update { it - visualization.dataStreamId }
            return
        }

        val profile = profiles.getById(profileId) ?: return
        _videoErrors.update { it - visualization.dataStreamId }
        val url = apiBase(profile.endpointUrl)
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://") +
                // A literal '+' in a query parameter is decoded as a space.
                // This must match platform-ui's SWE_BINARY_FORMAT exactly.
                "/datastreams/${visualization.dataStreamId}/observations?format=application/swe%2Bbinary"
        val request = authorizedRequest(url, profile).build()
        val socket = http.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                handleVideoMessage(text, visualization.dataStreamId)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                videoRenderers[visualization.dataStreamId]
                    ?.update(System.currentTimeMillis(), bytes.toByteArray())
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (sockets.remove(visualization.dataStreamId, webSocket)) {
                    _enabledVideos.update { it - visualization.dataStreamId }
                    _videoErrors.update {
                        it + (visualization.dataStreamId to (t.message ?: "Could not connect to the server."))
                    }
                }
            }
        })
        sockets[visualization.dataStreamId]?.cancel()
        sockets[visualization.dataStreamId] = socket
        _enabledVideos.update { it + visualization.dataStreamId }
    }

    fun videoRenderer(system: RemoteSystem, visualization: RemoteVisualization): VideoStream =
        videoRenderers.getOrPut(visualization.dataStreamId) {
            VideoStream(listOf(visualization.dataStreamId), system.name, 1280, 720)
        }

    fun stopSystemVideos(profileId: String, system: RemoteSystem) {
        system.visualizations
            .filter { it.kind == RemoteVisualization.Kind.VIDEO }
            .forEach { visualization ->
                setVideoEnabled(profileId, visualization, false)
                videoRenderers.remove(visualization.dataStreamId)?.disconnect()
            }
    }

    private fun handleVideoMessage(text: String, dataStreamId: String) {
        val frameBytes = runCatching { findImageFrame(JSONObject(text)) }.getOrNull() ?: return
        decodeJpeg(frameBytes)?.let { bitmap ->
            _videoFrames.update { it + (dataStreamId to bitmap) }
        }
    }

    private fun decodeJpeg(bytes: ByteArray): Bitmap? =
        runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()

    // Recursively hunt for a base64 string field whose decoded bytes start with
// the JPEG SOI marker (0xFFD8), same tree-search style as findCoordinates().
    private fun findImageFrame(value: Any?): ByteArray? {
        when (value) {
            is JSONObject -> {
                value.keys().forEach { key ->
                    val v = value.opt(key)
                    if (v is String) {
                        val decoded = runCatching { Base64.decode(v, Base64.DEFAULT) }.getOrNull()
                        if (decoded != null && decoded.size > 2 &&
                            decoded[0] == 0xFF.toByte() && decoded[1] == 0xD8.toByte()
                        ) return decoded
                    }
                }
                value.keys().forEach { key -> findImageFrame(value.opt(key))?.let { return it } }
            }
            is JSONArray -> for (i in 0 until value.length()) {
                findImageFrame(value.opt(i))?.let { return it }
            }
        }
        return null
    }

    private fun fetchVideoEncoding(
        dataStreamId: String,
        profile: ServerProfileItem
    ): String? {
        val schema = getJson(
            "${apiBase(profile.endpointUrl)}/datastreams/$dataStreamId/schema?obsFormat=application/swe+binary",
            profile,
        )
        val members = schema.optJSONObject("resultEncoding")?.optJSONArray("members") ?: return null
        for (index in 0 until members.length()) {
            members.optJSONObject(index)?.optString("compression")?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return null
    }

    fun setLocationEnabled(
        profileId: String,
        system: RemoteSystem,
        visualization: RemoteVisualization,
        enabled: Boolean,
    ) {
        if (!enabled) {
            sockets.remove(visualization.dataStreamId)?.close(1000, "disabled")
            _enabledLocations.update { it - visualization.dataStreamId }
            OshMapStore.remove(visualization.dataStreamId)
            return
        }

        val profile = profiles.getById(profileId) ?: return
        val url = apiBase(profile.endpointUrl)
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://") +
                "/datastreams/${visualization.dataStreamId}/observations?format=application/json"
        val request = authorizedRequest(url, profile).build()
        val socket = http.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                handleLocationMessage(text, visualization, system)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                handleLocationMessage(bytes.utf8(), visualization, system)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                sockets.remove(visualization.dataStreamId)
                _enabledLocations.update { it - visualization.dataStreamId }
            }
        })
        sockets[visualization.dataStreamId]?.cancel()
        sockets[visualization.dataStreamId] = socket
        _enabledLocations.update { it + visualization.dataStreamId }
    }


    private fun handleLocationMessage(
        text: String,
        visualization: RemoteVisualization,
        system: RemoteSystem,
    ) {
        findCoordinates(text)?.let { (lat, lon) ->
            OshMapStore.update(visualization.dataStreamId, system.id, system.name, lat, lon)
        }
    }

    private fun discoverNode(profile: ServerProfileItem): List<RemoteSystem> {
        val systemsJson = getJson("${apiBase(profile.endpointUrl)}/systems?limit=10000&f=geojson", profile)
        val datastreamsJson = getJson("${apiBase(profile.endpointUrl)}/datastreams?limit=10000", profile)
        val controlStreamsJson = getJson("${apiBase(profile.endpointUrl)}/controlstreams?limit=10000", profile)
        val visualizations = classifyDatastreams(datastreamsJson)
        val ptzBySystem = classifyControlStreams(controlStreamsJson)

        val items = systemsJson.optJSONArray("features") ?: systemsJson.optJSONArray("items") ?: JSONArray()
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val id = item.optString("id")
                if (id.isBlank()) continue
                val properties = item.optJSONObject("properties")
                val name = item.optString("label").ifBlank {
                    item.optString("name").ifBlank {
                        properties?.optString("name").orEmpty().ifBlank { id }
                    }
                }
                val description = item.optString("description").ifBlank {
                    properties?.optString("description").orEmpty()
                }
                val uid = item.optString("uid").ifBlank {
                    properties?.optString("uid").orEmpty()
                }
                add(
                    RemoteSystem(
                        id = id,
                        uid = uid,
                        name = name,
                        description = description,
                        location = pointFromGeoJson(item.optJSONObject("geometry")),
                        visualizations = visualizations[id].orEmpty(),
                        ptz = ptzBySystem[id],
                    )
                )
            }
        }
    }

    private fun classifyControlStreams(json: JSONObject): Map<String, RemoteControlStream> {
        val result = mutableMapOf<String, RemoteControlStream>()
        val items = json.optJSONArray("items") ?: return emptyMap()

        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val systemId = item.optString("system@id")
            val controlStreamId = item.optString("id")
            if (systemId.isBlank() || controlStreamId.isBlank()) continue

            val definitions = item.optJSONArray("controlledProperties")?.let { props ->
                (0 until props.length()).mapNotNull { props.optJSONObject(it)?.optString("definition") }
            }.orEmpty().toSet() // dedupe repeated entries

            val hasPtz = definitions.any {
                it.endsWith("/Pan") || it.endsWith("/Tilt") || it.endsWith("/ZoomFactor")
            }
            if (!hasPtz) continue

            result[systemId] = RemoteControlStream(
                controlStreamId = controlStreamId,
                supportsAbsolutePan = "http://sensorml.com/ont/swe/property/Pan" in definitions,
                supportsAbsoluteTilt = "http://sensorml.com/ont/swe/property/Tilt" in definitions,
                supportsAbsoluteZoom = "http://sensorml.com/ont/swe/property/ZoomFactor" in definitions,
                supportsRelativePan = "http://sensorml.com/ont/swe/property/RelativePan" in definitions,
                supportsRelativeTilt = "http://sensorml.com/ont/swe/property/RelativeTilt" in definitions,
                supportsRelativeZoom = "http://sensorml.com/ont/swe/property/RelativeZoomFactor" in definitions,
                supportsPresets = "http://sensorml.com/ont/swe/property/CameraPresetPositionName" in definitions,
            )
        }
        return result
    }

    /** GeoJSON coordinates are longitude, latitude; the app map uses latitude, longitude. */
    private fun pointFromGeoJson(geometry: JSONObject?): Pair<Double, Double>? {
        if (geometry?.optString("type") != "Point") return null
        val coordinates = geometry.optJSONArray("coordinates") ?: return null
        val longitude = coordinates.optDouble(0, Double.NaN)
        val latitude = coordinates.optDouble(1, Double.NaN)
        return if (latitude.isFinite() && longitude.isFinite()) latitude to longitude else null
    }

    private fun classifyDatastreams(json: JSONObject): Map<String, List<RemoteVisualization>> {
        val result = mutableMapOf<String, MutableList<RemoteVisualization>>()
        val items = json.optJSONArray("items") ?: return emptyMap()

        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val systemId = item.optString("system@id")
            val dataStreamId = item.optString("id")
            val name = item.optString("name")
            if (systemId.isBlank() || dataStreamId.isBlank()) continue

            val resultType = item.optString("resultType")
            val definitions = item.optJSONArray("observedProperties")?.let { props ->
                (0 until props.length()).mapNotNull { props.optJSONObject(it)?.optString("definition") }
            }.orEmpty()

            val kind = when {
                resultType == "coverage" && definitions.any { it.endsWith("/RasterImage") } ->
                    RemoteVisualization.Kind.VIDEO
                resultType == "vector" && definitions.any { it.endsWith("/LocationVector") || it.endsWith("SensorLocation") } ->
                    RemoteVisualization.Kind.LOCATION
                // fallback for servers that report lat/lon as two scalar fields instead of a vector
                definitions.any { it.endsWith("/GeodeticLatitude") } &&
                        definitions.any { it.endsWith("/Longitude") } ->
                    RemoteVisualization.Kind.LOCATION
                else -> null
            } ?: continue

            result.getOrPut(systemId) { mutableListOf() }.add(RemoteVisualization(dataStreamId, name, kind))
        }
        return result
    }

    fun sendPtzCommand(profileId: String, controlStream: RemoteControlStream, params: Map<String, Any>) {
        val profile = profiles.getById(profileId) ?: return
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { postCommand(controlStream.controlStreamId, params, profile) } }
                .onFailure { /* surface to a _commandError flow if you want UI feedback */ }
        }
    }

    private fun postCommand(controlStreamId: String, params: Map<String, Any>, profile: ServerProfileItem) {
        val url = "${apiBase(profile.endpointUrl)}/controlstreams/$controlStreamId/commands"
        // Commands are SWE Common records, not ConSys JSON resources.  For a
        // DataChoice PTZ stream this is, for example, {"rpan":-5.0}.  Use a
        // byte-array body so OkHttp does not append `charset=UTF-8`: ConSys
        // matches its request format against the exact SWE media type.
        val body = JSONObject(params).toString().toByteArray(Charsets.UTF_8)
            .toRequestBody("application/swe+json".toMediaType())
        val request = authorizedRequest(url, profile).post(body).build()
        http.newCall(request).execute().use {
            if (!it.isSuccessful) error("${it.code} ${it.message} sending PTZ command to $controlStreamId")
        }
    }

    private fun getJson(url: String, profile: ServerProfileItem): JSONObject {
        val response = http.newCall(authorizedRequest(url, profile).get().build()).execute()
        response.use {
            if (!it.isSuccessful) error("${it.code} ${it.message} from $url")
            return JSONObject(it.body?.string().orEmpty())
        }
    }

    private fun authorizedRequest(url: String, profile: ServerProfileItem): Request.Builder {
        val builder = Request.Builder().url(url)
        if (profile.username.isNotBlank()) {
            val password = profiles.getPassword(profile.id)
            val token = Base64.encodeToString(
                "${profile.username}:$password".toByteArray(),
                Base64.NO_WRAP,
            )
            builder.header("Authorization", "Basic $token")
        }
        return builder
    }

    private fun parseVisualizations(json: JSONObject): Map<String, List<RemoteVisualization>> {
        val result = mutableMapOf<String, MutableList<RemoteVisualization>>()
        val rows = json.optJSONArray("resultSet") ?: return emptyMap()
        for (index in 0 until rows.length()) {
            val row = rows.optJSONObject(index) ?: continue
            val systemId = row.optString("systemId")
            if (systemId.isBlank()) continue
            fun add(kind: RemoteVisualization.Kind, key: String) {
                val values = row.optJSONArray(key) ?: return
                for (itemIndex in 0 until values.length()) {
                    val id = values.optJSONObject(itemIndex)?.optString("dataStreamId").orEmpty()
                    val name = values.optJSONObject(itemIndex)?.optString("name").orEmpty()
                    if (id.isNotBlank()) result.getOrPut(systemId) { mutableListOf() }
                        .add(RemoteVisualization(id, name, kind))
                }
            }
            add(RemoteVisualization.Kind.LOCATION, "location")
            add(RemoteVisualization.Kind.VIDEO, "video")
        }
        return result
    }

    private fun findCoordinates(text: String): Pair<Double, Double>? = runCatching {
        findCoordinates(JSONObject(text))
    }.getOrNull()

    private fun findCoordinates(value: Any?): Pair<Double, Double>? {
        when (value) {
            is JSONObject -> {
                val latitude = number(value, "lat", "latitude")
                val longitude = number(value, "lon", "lng", "longitude")
                if (latitude != null && longitude != null) return latitude to longitude
                value.keys().forEach { key -> findCoordinates(value.opt(key))?.let { return it } }
            }
            is JSONArray -> for (index in 0 until value.length()) {
                findCoordinates(value.opt(index))?.let { return it }
            }
        }
        return null
    }

    private fun number(json: JSONObject, vararg keys: String): Double? {
        for (key in keys) {
            if (!json.has(key)) continue
            val value = json.opt(key)
            when (value) {
                is Number -> return value.toDouble()
                is String -> value.toDoubleOrNull()?.let { return it }
            }
        }
        return null
    }

    private fun apiBase(endpoint: String): String {
        val normalized = endpoint.trim().trimEnd('/')
        return if (normalized.endsWith("/api")) normalized else
            "${nodeBase(normalized)}/sensorhub/api"
    }

    private fun nodeBase(endpoint: String): String {
        val normalized = endpoint.trim().trimEnd('/')
        return normalized.substringBefore("/sensorhub/").removeSuffix("/sensorhub")
    }

    private fun updateNode(id: String, transform: (RemoteNodeState) -> RemoteNodeState) {
        _nodes.update { nodes -> nodes.map { if (it.profileId == id) transform(it) else it } }
    }

    override fun onCleared() {
        sockets.values.forEach { it.close(1000, "client closed") }
        sockets.clear()
        videoRenderers.values.forEach { it.disconnect() }
        videoRenderers.clear()
        http.dispatcher.executorService.shutdown()
        super.onCleared()
    }
}
