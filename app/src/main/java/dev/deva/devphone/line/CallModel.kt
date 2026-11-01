package dev.deva.devphone.line

import android.net.Uri

sealed interface LineStatus {
    data object NeedsSetup : LineStatus

    data object Away : LineStatus

    data object Connecting : LineStatus

    data object Ready : LineStatus

    data class Failed(
        val reason: String,
    ) : LineStatus
}

enum class Direction { Incoming, Outgoing }

enum class CallPhase { Ringing, Dialing, Connecting, Active, Ended }

data class ActiveCall(
    val number: String,
    val name: String?,
    val direction: Direction,
    val phase: CallPhase,
    val connectedAt: Long? = null,
    val muted: Boolean = false,
    val speaker: Boolean = false,
    val endReason: String? = null,
    val photo: Uri? = null,
)

object CallPhases {
    fun fromLinphone(
        state: String,
        incoming: Boolean,
    ): CallPhase? = when (state) {
        "IncomingReceived", "PushIncomingReceived", "IncomingEarlyMedia" -> CallPhase.Ringing
        "OutgoingInit", "OutgoingProgress" -> CallPhase.Dialing
        "OutgoingRinging", "OutgoingEarlyMedia" -> CallPhase.Ringing
        "Connected" -> if (incoming) CallPhase.Connecting else CallPhase.Active
        "StreamsRunning", "Resuming", "UpdatedByRemote", "Updating", "Paused", "Pausing", "PausedByRemote" -> CallPhase.Active
        "End", "Error", "Released" -> CallPhase.Ended
        else -> null
    }

    fun reasonText(reason: String): String? = when (reason) {
        "None", "Ok" -> null
        "Busy" -> "Busy"
        "Declined" -> "Declined"
        "NotAnswered" -> "No answer"
        "NotFound" -> "Number not found"
        "TemporarilyUnavailable" -> "Unavailable"
        "IOError", "NoResponse", "ServerTimeout" -> "Network problem"
        "Forbidden", "Unauthorized" -> "Not allowed"
        "AddressIncomplete" -> "Incomplete number"
        else -> "Call failed"
    }
}
