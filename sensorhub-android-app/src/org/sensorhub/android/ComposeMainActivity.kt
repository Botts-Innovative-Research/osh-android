package org.sensorhub.android

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.sensorhub.android.SensorHubService.LocalBinder
import org.sensorhub.android.ui.theme.OSHTheme
import org.sensorhub.impl.sensor.controller.ControllerDriver

class ComposeMainActivity : ComponentActivity() {
    private var boundService: SensorHubService? = null
    private var serviceBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            boundService = (service as LocalBinder).getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            boundService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OSHTheme {
                OSHApp()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        serviceBound = bindService(
            Intent(this, SensorHubService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        if (serviceBound) {
            unbindService(connection)
            serviceBound = false
        }
        boundService = null
        super.onStop()
    }

    private fun getControllerDriver(): ControllerDriver? {
        if (boundService == null || boundService!!.sensorhub == null) return null
        try {
            return boundService!!.sensorhub.getModuleRegistry()
                .getModuleByType<ControllerDriver?>(ControllerDriver::class.java)
        } catch (e: java.lang.Exception) {
            return null
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val controller = getControllerDriver()
        if (controller != null && controller.onKeyEvent(event)) return true
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        val controller = getControllerDriver()
        if (controller != null && controller.onMotionEvent(event)) return true
        return super.dispatchGenericMotionEvent(event)
    }

}
