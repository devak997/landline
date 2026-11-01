package dev.deva.devphone.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import dev.deva.devphone.line
import dev.deva.devphone.line.ActiveCall
import dev.deva.devphone.line.CallPhase
import dev.deva.devphone.line.HomeWatcher
import dev.deva.devphone.line.LineStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class LineService : Service() {
    private val scope = MainScope()
    private lateinit var watcher: HomeWatcher
    private var observer: Job? = null
    private var micTypeActive = false

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        val line = line
        promote(Notifications.status(this, line.status.value, line.configStore.config.value.number), withMic = false)
        watcher =
            HomeWatcher(
                this,
                scope,
                imsDomain = { line.configStore.config.value.domain },
                expectedProxies = {
                    line.configStore.config.value
                        .let { setOf(it.proxy, it.backupProxy).filter(String::isNotBlank).toSet() }
                },
            ) { line.onHomeChanged(it) }
        watcher.start()
        observer =
            scope.launch {
                combine(line.status, line.call) { s, c -> s to c }.collect { (status, call) -> render(status, call) }
            }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    private fun render(
        status: LineStatus,
        call: ActiveCall?,
    ) {
        if (call == null) {
            promote(Notifications.status(this, status, line.configStore.config.value.number), withMic = false)
        } else {
            val needsMic = call.phase != CallPhase.Ringing || call.direction == dev.deva.devphone.line.Direction.Outgoing
            promote(Notifications.call(this, call), withMic = needsMic && call.phase != CallPhase.Ended)
        }
    }

    private fun promote(
        notification: android.app.Notification,
        withMic: Boolean,
    ) {
        val type =
            if (withMic) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            }
        try {
            ServiceCompat.startForeground(this, Notifications.ID, notification, type)
            micTypeActive = withMic
        } catch (e: Exception) {
            Log.w("DevPhone/Service", "startForeground($type) refused", e)
            if (withMic) {
                runCatching {
                    ServiceCompat.startForeground(this, Notifications.ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL)
                }
            }
        }
    }

    override fun onDestroy() {
        watcher.stop()
        observer?.cancel()
        scope.cancel()
        line.onHomeChanged(false)
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            runCatching { context.startForegroundService(Intent(context, LineService::class.java)) }
                .onFailure { Log.w("DevPhone/Service", "could not start", it) }
        }
    }
}
