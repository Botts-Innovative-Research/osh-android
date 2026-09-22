package org.sensorhub.android.ui.screens.client

import android.graphics.BitmapFactory
import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class PtzCommand(val item: String, val delta: Double) {
    PAN_LEFT("rpan", -5.0), PAN_RIGHT("rpan", 5.0),
    TILT_UP("rtilt", 5.0), TILT_DOWN("rtilt", -5.0),
    ZOOM_IN("rzoom", 1000.0), ZOOM_OUT("rzoom", -1000.0),
}

/** App-owned Android video renderer; it deliberately has no Platform UI dependency. */
class VideoStream(
    private val streamIds: Collection<String>,
    private val name: String?,
    private val width: Int,
    private val height: Int,
) {
    private var view: TextureView? = null
    private var surface: Surface? = null
    private var codec: MediaCodec? = null
    private var queuedFrames = 0L

    fun attach(textureView: TextureView) {
        view = textureView
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(st: android.graphics.SurfaceTexture, w: Int, h: Int) {
                surface?.release(); surface = Surface(st)
            }
            override fun onSurfaceTextureSizeChanged(st: android.graphics.SurfaceTexture, w: Int, h: Int) = Unit
            override fun onSurfaceTextureDestroyed(st: android.graphics.SurfaceTexture): Boolean {
                releaseCodec(); surface?.release(); surface = null; return true
            }
            override fun onSurfaceTextureUpdated(st: android.graphics.SurfaceTexture) = Unit
        }
        if (textureView.isAvailable) surface = Surface(textureView.surfaceTexture)
    }

    fun update(timestamp: Long, record: ByteArray) {
        val jpeg = record.indexOfJpeg()
        if (jpeg >= 0) {
            val bitmap = BitmapFactory.decodeByteArray(record, jpeg, record.size - jpeg) ?: return
            val target = surface ?: return
            runCatching { target.lockCanvas(null).also { canvas ->
                canvas.drawBitmap(bitmap, null, android.graphics.Rect(0, 0, canvas.width, canvas.height), null)
                target.unlockCanvasAndPost(canvas)
            } }
            return
        }
        val target = surface ?: return
        val decoder = codec ?: MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).also {
            it.configure(MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height), target, null, 0)
            it.start(); codec = it
        }
        runCatching {
            val input = decoder.dequeueInputBuffer(10_000)
            if (input >= 0) {
                decoder.getInputBuffer(input)?.apply { clear(); put(record, 12, record.size - 12) }
                decoder.queueInputBuffer(input, 0, record.size - 12, queuedFrames++ * 33_333L, 0)
            }
            val info = MediaCodec.BufferInfo()
            var output = decoder.dequeueOutputBuffer(info, 0)
            while (output >= 0) { decoder.releaseOutputBuffer(output, true); output = decoder.dequeueOutputBuffer(info, 0) }
        }.onFailure { releaseCodec() }
    }

    fun disconnect() { releaseCodec(); surface?.release(); surface = null; view = null }
    private fun releaseCodec() { runCatching { codec?.stop(); codec?.release() }; codec = null }
    private fun ByteArray.indexOfJpeg(): Int = indices.firstOrNull { it + 2 < size && this[it] == 0xFF.toByte() && this[it + 1] == 0xD8.toByte() && this[it + 2] == 0xFF.toByte() } ?: -1
}

@Composable
fun VideoScreen(videoSurface: @Composable (Modifier) -> Unit, onExit: () -> Unit, onPtz: (PtzCommand) -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        videoSurface(Modifier.fillMaxSize())
        IconButton(onClick = onExit, modifier = Modifier.align(Alignment.TopStart).padding(12.dp)) {
            Icon(Icons.Default.ArrowBack, "Close video", tint = Color.White)
        }
        Column(Modifier.align(Alignment.BottomCenter).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            PtzButton(Icons.Default.KeyboardArrowUp, PtzCommand.TILT_UP, onPtz)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PtzButton(Icons.Default.KeyboardArrowLeft, PtzCommand.PAN_LEFT, onPtz)
                PtzButton(Icons.Default.KeyboardArrowRight, PtzCommand.PAN_RIGHT, onPtz)
            }
            PtzButton(Icons.Default.KeyboardArrowDown, PtzCommand.TILT_DOWN, onPtz)
            Row { PtzButton(Icons.Default.ZoomOut, PtzCommand.ZOOM_OUT, onPtz); PtzButton(Icons.Default.ZoomIn, PtzCommand.ZOOM_IN, onPtz) }
        }
    }
}

@Composable private fun PtzButton(icon: ImageVector, command: PtzCommand, onPtz: (PtzCommand) -> Unit) {
    IconButton(onClick = { onPtz(command) }) { Icon(icon, command.name, tint = MaterialTheme.colorScheme.primary) }
}
