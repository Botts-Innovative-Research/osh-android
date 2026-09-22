package org.sensorhub.android.ui.screens.systems

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.sensorhub.android.OkHttpClientWrapper
import org.sensorhub.impl.service.consys.client.ITokenHandler
import org.sensorhub.impl.service.consys.client.http.IHttpClient
import org.sensorhub.impl.service.consys.resource.ResourceFormat
import java.io.Closeable
import java.net.URI
import java.util.concurrent.CompletableFuture

class NodeResourceClient(
    endpoint: String,
    private val http: IHttpClient,
    private val pageSize: Int = 1000
) : Closeable {
    private val endpoint = URI(endpoint.trim().trimEnd('/') + "/").also {
        require(it.scheme in listOf("http", "https") && it.host != null &&
            it.userInfo == null && it.query == null && it.fragment == null) {
            "Expected an HTTP(S) CSAPI base URL without credential or query"
        }
    }

    init {
        require(pageSize in 1..10000) { "Page size must be between 1 and 10000" }
    }

    fun getSystems(limit: Int = pageSize): CompletableFuture<List<JsonObject>> {
        require(limit in 1..10000) { "Limit must be between 1 and 10000" }
        return getCollection("systems?limit=$limit")
    }

    fun getDataStreams(): CompletableFuture<List<JsonObject>> = getCollection("datastreams")

    fun getControlStreams(): CompletableFuture<List<JsonObject>> = getCollection("controlstreams")

    fun getDatastream(id: String): CompletableFuture<JsonObject> {
        require(id.isNotBlank() && id != "." && id != "..") { "Invalid datastream ID" }
        val encodedId = java.net.URLEncoder.encode(id, "UTF-8").replace("+", "%20")
        return http.sendGetRequest(endpoint.resolve("datastreams/$encodedId"), ResourceFormat.JSON) { body ->
            JsonParser.parseReader(body.reader(Charsets.UTF_8)).asJsonObject
        }
    }

    fun getControlstream(id: String): CompletableFuture<JsonObject> {
        require(id.isNotBlank() && id != "." && id != "..") { "Invalid controlstream ID" }
        val encodedId = java.net.URLEncoder.encode(id, "UTF-8").replace("+", "%20")
        return http.sendGetRequest(endpoint.resolve("controlstreams/$encodedId"), ResourceFormat.JSON) { body ->
            JsonParser.parseReader(body.reader(Charsets.UTF_8)).asJsonObject
        }
    }

    private fun getCollection(path: String): CompletableFuture<List<JsonObject>> = try {
        http.sendGetRequest(endpoint.resolve(path), ResourceFormat.JSON) { body ->
            val page = JsonParser.parseReader(body.reader(Charsets.UTF_8)).asJsonObject
            val items = page.get("items") ?: page.get("features")
            require(items != null && items.isJsonArray) { "Missing resource collection" }
            items.asJsonArray.map { it.asJsonObject }
        }
    } catch (e: Exception) {
        CompletableFuture<List<JsonObject>>().also { it.completeExceptionally(e) }
    }

    override fun close() {
        (http as? Closeable)?.close()
    }

    companion object {
        fun create(
            endpoint: String,
            username: String? = null,
            password: CharArray? = null,
            tokenHandler: ITokenHandler? = null
        ): NodeResourceClient {
            return NodeResourceClient(endpoint, OkHttpClientWrapper(username, password, tokenHandler))
        }
    }
}
