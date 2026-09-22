package org.sensorhub.android.ui.screens.systems

import com.google.gson.JsonObject

data class SystemsResource(
    val id: String,
    val name: String,
    val systemId: String?,
    val json: JsonObject,
    val systemUid: String? = null
) {
    companion object {
        fun fromJson(json: JsonObject): SystemsResource {
            fun JsonObject.text(key: String) = get(key)?.takeIf { it.isJsonPrimitive }?.asString
            val properties = json.get("properties")?.takeIf { it.isJsonObject }?.asJsonObject
            val id = requireNotNull(json.text("id")) { "Resource is missing its ID" }
            val systemUid = json.get("system@link")
                ?.takeIf { it.isJsonObject }?.asJsonObject?.text("uid")
                ?: properties?.text("uid")
            return SystemsResource(id, json.text("name") ?: properties?.text("name") ?: id,
                json.text("system@id") ?: json.get("system")?.takeIf { it.isJsonObject }?.asJsonObject?.text("id"),
                json, systemUid)
        }
    }

    fun hasResource(isDataStream: Boolean, propertyDefinition: String): Boolean {
        val key = if (isDataStream) "observedProperties" else "controlledProperties"
        return json.get(key)
            ?.takeIf { it.isJsonArray }
            ?.asJsonArray
            ?.any { property ->
                property.takeIf { it.isJsonObject }
                    ?.asJsonObject
                    ?.get("definition")
                    ?.takeIf { it.isJsonPrimitive }
                    ?.asString
                    .equals(propertyDefinition, ignoreCase = true)
            } == true
    }

    fun handleSchemaInfo(){

    }
}

enum class ResourceKind { SYSTEMS, DATASTREAMS, CONTROLSTREAMS }
data class CollectionState(
    val resources: List<SystemsResource> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null
)
data class SystemsServerState(
    val id: String,
    val name: String,
    val collections: Map<ResourceKind, CollectionState> = ResourceKind.values().associateWith { CollectionState() },
    val endpointUrl: String = ""
)

data class SystemsServerOption(val id: String, val name: String, val endpointUrl: String)
