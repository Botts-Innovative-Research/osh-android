package org.sensorhub.android.data.client

data class RemoteVisualization(
    val dataStreamId: String,
    val name: String,
    val kind: Kind,
) {
    enum class Kind { LOCATION, VIDEO, OTHER }
}

enum class StreamStatus { CONNECTING, RECEIVING, DISCONNECTED, PAUSED }

data class RemoteSystem(
    val id: String,
    val uid: String,
    val name: String,
    val description: String,
    val location: Pair<Double, Double>? = null,
    val visualizations: List<RemoteVisualization>,
    val ptz: RemoteControlStream? = null
)

data class RemoteNodeState(
    val profileId: String,
    val name: String,
    val endpointUrl: String,
    val loading: Boolean = false,
    val error: String? = null,
    val systems: List<RemoteSystem> = emptyList(),
)

data class RemoteTrack(
    val streamId: String,
    val systemId: String,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val trail: List<Pair<Double, Double>>,
)

data class SystemDetailUiState(
    val system: RemoteSystem? = null,
    val enabledLocations: Set<String> = emptySet(),
    val enabledVideos: Set<String> = emptySet(),
    val videoErrors: Map<String, String> = emptyMap(),
    val otherValues: Map<String, Map<String, String>> = emptyMap(),
    val streamStatuses: Map<String, StreamStatus> = emptyMap(),
    val tracks: Map<String, RemoteTrack> = emptyMap(),
)

data class RemoteControlStream(
    val controlStreamId: String,
    val supportsAbsolutePan: Boolean,
    val supportsAbsoluteTilt: Boolean,
    val supportsAbsoluteZoom: Boolean,
    val supportsRelativePan: Boolean,
    val supportsRelativeTilt: Boolean,
    val supportsRelativeZoom: Boolean,
    val supportsPresets: Boolean,
)
