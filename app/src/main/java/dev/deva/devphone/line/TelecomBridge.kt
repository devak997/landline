package dev.deva.devphone.line

import android.content.Context
import android.net.Uri
import android.telecom.DisconnectCause
import android.util.Log
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.CallsManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class TelecomBridge(
    context: Context,
    private val scope: CoroutineScope,
) {
    interface Listener {
        fun onSystemAnswer()

        fun onSystemDisconnect()

        fun onEndpointChanged(type: Int)
    }

    var listener: Listener? = null

    private val manager = CallsManager(context)
    private var registered = false
    private var control: CompletableDeferred<CallControlScope>? = null
    private var endpoints: List<CallEndpointCompat> = emptyList()
    private var lastNonSpeaker: CallEndpointCompat? = null

    private fun ensureRegistered() {
        if (registered) return
        registered =
            runCatching { manager.registerAppWithTelecom(CallsManager.CAPABILITY_BASELINE) }
                .onFailure { Log.w(TAG, "Telecom registration failed", it) }
                .isSuccess
    }

    fun begin(
        displayName: String,
        number: String,
        incoming: Boolean,
    ) {
        ensureRegistered()
        if (!registered) return
        val deferred = CompletableDeferred<CallControlScope>()
        control = deferred
        val attributes =
            CallAttributesCompat(
                displayName = displayName,
                address = Uri.fromParts("tel", number, null),
                direction = if (incoming) CallAttributesCompat.DIRECTION_INCOMING else CallAttributesCompat.DIRECTION_OUTGOING,
                callType = CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
                callCapabilities = 0,
            )
        scope.launch {
            try {
                manager.addCall(
                    attributes,
                    onAnswer = { listener?.onSystemAnswer() },
                    onDisconnect = { listener?.onSystemDisconnect() },
                    onSetActive = {},
                    onSetInactive = {},
                ) {
                    deferred.complete(this)
                    launch {
                        currentCallEndpoint.collect { ep ->
                            if (ep.type != CallEndpointCompat.TYPE_SPEAKER) lastNonSpeaker = ep
                            listener?.onEndpointChanged(ep.type)
                        }
                    }
                    launch { availableEndpoints.collect { endpoints = it } }
                }
            } catch (e: Exception) {
                Log.w(TAG, "addCall failed; continuing without telecom", e)
                deferred.completeExceptionally(e)
            }
        }
    }

    fun answered() = withControl { answer(CallAttributesCompat.CALL_TYPE_AUDIO_CALL) }

    fun active() = withControl { setActive() }

    fun setSpeaker(on: Boolean): Boolean {
        val target =
            if (on) {
                endpoints.firstOrNull { it.type == CallEndpointCompat.TYPE_SPEAKER }
            } else {
                lastNonSpeaker ?: endpoints.firstOrNull { it.type != CallEndpointCompat.TYPE_SPEAKER }
            }
        if (control == null || target == null) return false
        withControl { requestEndpointChange(target) }
        return true
    }

    fun end(code: Int) {
        val d = control ?: return
        control = null
        endpoints = emptyList()
        lastNonSpeaker = null
        scope.launch {
            runCatching { withTimeout(3_000) { d.await().disconnect(DisconnectCause(code)) } }
        }
    }

    private fun withControl(block: suspend CallControlScope.() -> Unit) {
        val d = control ?: return
        scope.launch {
            runCatching { withTimeout(5_000) { d.await().block() } }
                .onFailure { Log.w(TAG, "telecom action failed", it) }
        }
    }

    private companion object {
        const val TAG = "DevPhone/Telecom"
    }
}
