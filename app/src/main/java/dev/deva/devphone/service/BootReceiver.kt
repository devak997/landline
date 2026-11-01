package dev.deva.devphone.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.deva.devphone.line

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        if (context.line.configStore.config.value
                .let { it.enabled && it.isComplete }
        ) {
            LineService.start(context)
        }
    }
}
