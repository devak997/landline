package dev.deva.devphone.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import dev.deva.devphone.CallActivity
import dev.deva.devphone.MainActivity
import dev.deva.devphone.R
import dev.deva.devphone.line.ActiveCall
import dev.deva.devphone.line.CallPhase
import dev.deva.devphone.line.Direction
import dev.deva.devphone.line.LineStatus
import dev.deva.devphone.line.PhoneNumbers

object Notifications {
    const val ID = 1
    private const val CH_STATUS = "line_status"
    private const val CH_INCOMING = "incoming_calls"
    private const val CH_ONGOING = "ongoing_calls"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(CH_STATUS, "Line status", NotificationManager.IMPORTANCE_MIN).apply {
                    description = "Quiet indicator that the landline is connected"
                    setShowBadge(false)
                },
                NotificationChannel(CH_INCOMING, "Incoming calls", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Rings and shows the full-screen call"
                    setSound(null, null)
                    enableVibration(false)
                },
                NotificationChannel(CH_ONGOING, "Ongoing call", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                },
            ),
        )
    }

    fun status(
        context: Context,
        status: LineStatus,
        number: String,
    ): Notification {
        val (title, text) =
            when (status) {
                LineStatus.Ready -> "Landline ready" to PhoneNumbers.pretty(number)
                LineStatus.Connecting -> "Connecting landline…" to "Registering with Airtel"
                LineStatus.Away -> "Landline paused" to "Not on home Wi-Fi · using no power"
                LineStatus.NeedsSetup -> "Landline needs setup" to "Open the app to add your password"
                is LineStatus.Failed -> "Landline offline" to status.reason
            }
        return NotificationCompat
            .Builder(context, CH_STATUS)
            .setSmallIcon(R.drawable.ic_stat_landline)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(activity(context, Intent(context, MainActivity::class.java), 0))
            .build()
    }

    fun call(
        context: Context,
        call: ActiveCall,
    ): Notification {
        val person =
            Person
                .Builder()
                .setName(call.name ?: PhoneNumbers.pretty(call.number))
                .setImportant(true)
                .build()
        val open = activity(context, Intent(context, CallActivity::class.java), 1)
        val hangup = broadcast(context, CallActionReceiver.ACTION_HANGUP, 2)
        val ringingIn = call.direction == Direction.Incoming && call.phase == CallPhase.Ringing
        val builder =
            NotificationCompat
                .Builder(context, if (ringingIn) CH_INCOMING else CH_ONGOING)
                .setSmallIcon(R.drawable.ic_stat_landline)
                .setContentIntent(open)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .addPerson(person)
        if (ringingIn) {
            val answer = activity(context, Intent(context, CallActivity::class.java).setAction(CallActivity.ACTION_ANSWER), 3)
            builder
                .setStyle(NotificationCompat.CallStyle.forIncomingCall(person, hangup, answer))
                .setFullScreenIntent(open, true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
        } else {
            builder.setStyle(NotificationCompat.CallStyle.forOngoingCall(person, hangup))
            call.connectedAt?.let { builder.setUsesChronometer(true).setWhen(it).setShowWhen(true) }
            builder.setContentText(
                when (call.phase) {
                    CallPhase.Dialing, CallPhase.Ringing -> "Calling…"
                    CallPhase.Connecting -> "Connecting…"
                    CallPhase.Active -> "Landline call"
                    CallPhase.Ended -> call.endReason ?: "Call ended"
                },
            )
        }
        return builder.build()
    }

    private fun activity(
        context: Context,
        intent: Intent,
        code: Int,
    ) = PendingIntent.getActivity(
        context,
        code,
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun broadcast(
        context: Context,
        action: String,
        code: Int,
    ) = PendingIntent.getBroadcast(
        context,
        code,
        Intent(context, CallActionReceiver::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}
