package dev.deva.devphone.ui

import dev.deva.devphone.data.HistoryType
import dev.deva.devphone.line.ActiveCall
import dev.deva.devphone.line.CallPhase
import dev.deva.devphone.line.Direction
import dev.deva.devphone.line.LineConfig
import dev.deva.devphone.line.LineStatus
import dev.deva.devphone.ui.screens.HomeState
import dev.deva.devphone.ui.screens.RecentRow
import dev.deva.devphone.ui.screens.Setup
import dev.deva.devphone.ui.screens.Tab

object Fixtures {
    const val NOW = 1_793_520_000_000L
    private const val H = 3_600_000L

    val recents =
        listOf(
            RecentRow(1, "+919876543210", "Amma", HistoryType.Incoming, NOW - 1 * H, 412),
            RecentRow(2, "+914023456789", null, HistoryType.Outgoing, NOW - 3 * H, 95),
            RecentRow(3, "+919000012345", "Ravi Kumar", HistoryType.Missed, NOW - 20 * H, 0),
            RecentRow(4, "198", "Airtel care", HistoryType.Outgoing, NOW - 50 * H, 240),
            RecentRow(5, "+919845098450", "Priya", HistoryType.Rejected, NOW - 200 * H, 0),
        )

    fun home(
        tab: Tab = Tab.Keypad,
        status: LineStatus = LineStatus.Ready,
        dialed: String = "9876543210",
    ) = HomeState(status, "+919000000000", tab, dialed, if (dialed.isNotEmpty()) "Amma" else null, recents, true)

    val incoming = ActiveCall("+919876543210", "Amma", Direction.Incoming, CallPhase.Ringing)
    val active =
        ActiveCall(
            "+914023456789",
            null,
            Direction.Outgoing,
            CallPhase.Active,
            connectedAt = NOW - 192_000,
            muted = true,
            speaker = false,
        )

    val config = LineConfig(password = "secret")
    val setup = Setup(batteryUnrestricted = false, missingPermissions = listOf("call log"), engineVersion = "5.5.39")
}
