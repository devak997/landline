package dev.deva.devphone.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.deva.devphone.line

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            ACTION_HANGUP -> context.line.hangup()
        }
    }

    companion object {
        const val ACTION_HANGUP = "dev.deva.devphone.HANGUP"
    }
}
