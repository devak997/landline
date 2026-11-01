package dev.deva.devphone

import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.deva.devphone.line.CallPhase
import dev.deva.devphone.ui.screens.CallActions
import dev.deva.devphone.ui.screens.CallScreen
import dev.deva.devphone.ui.theme.DevPhoneTheme

class CallActivity : ComponentActivity() {
    private var proximity: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        handle(intent)
        setContent {
            DevPhoneTheme {
                val call by line.call.collectAsStateWithLifecycle()
                val current = call
                LaunchedEffect(current == null) { if (current == null) finishAndRemoveTask() }
                if (current != null) {
                    val earpiece = current.phase == CallPhase.Active && !current.speaker
                    DisposableEffect(earpiece) {
                        setProximity(earpiece)
                        onDispose { setProximity(false) }
                    }
                    CallScreen(
                        current,
                        CallActions(
                            onAnswer = { line.answer() },
                            onHangup = { line.hangup() },
                            onMute = { line.toggleMute() },
                            onSpeaker = { line.toggleSpeaker() },
                            onDtmf = { line.sendDtmf(it) },
                        ),
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        if (intent?.action == ACTION_ANSWER) line.answer()
    }

    private fun setProximity(on: Boolean) {
        if (on && proximity == null) {
            val pm = getSystemService(PowerManager::class.java)
            if (pm.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
                proximity =
                    pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "DevPhone:ear").apply { acquire(3 * 60 * 60 * 1000L) }
            }
        } else if (!on) {
            proximity?.takeIf { it.isHeld }?.release()
            proximity = null
        }
    }

    companion object {
        const val ACTION_ANSWER = "dev.deva.devphone.ANSWER"
    }
}
