package dev.deva.devphone.line

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CallPhasesTest {
    private val allStates =
        listOf(
            "Idle",
            "IncomingReceived",
            "PushIncomingReceived",
            "OutgoingInit",
            "OutgoingProgress",
            "OutgoingRinging",
            "OutgoingEarlyMedia",
            "Connected",
            "StreamsRunning",
            "Pausing",
            "Paused",
            "Resuming",
            "Referred",
            "Error",
            "End",
            "PausedByRemote",
            "UpdatedByRemote",
            "IncomingEarlyMedia",
            "Updating",
            "Released",
            "EarlyUpdatedByRemote",
            "EarlyUpdating",
        )

    @Test fun `outgoing call walks dialing, ringing, active, ended`() {
        assertEquals(CallPhase.Dialing, CallPhases.fromLinphone("OutgoingInit", false))
        assertEquals(CallPhase.Ringing, CallPhases.fromLinphone("OutgoingRinging", false))
        assertEquals(CallPhase.Active, CallPhases.fromLinphone("Connected", false))
        assertEquals(CallPhase.Ended, CallPhases.fromLinphone("End", false))
    }

    @Test fun `incoming connected waits for media before showing active`() {
        assertEquals(CallPhase.Ringing, CallPhases.fromLinphone("IncomingReceived", true))
        assertEquals(CallPhase.Connecting, CallPhases.fromLinphone("Connected", true))
        assertEquals(CallPhase.Active, CallPhases.fromLinphone("StreamsRunning", true))
    }

    @Test fun `terminal states all end the call`() {
        listOf("End", "Error", "Released").forEach { assertEquals(it, CallPhase.Ended, CallPhases.fromLinphone(it, true)) }
    }

    @Test fun `bookkeeping states change nothing`() {
        listOf("Idle", "Referred", "EarlyUpdating", "EarlyUpdatedByRemote").forEach { assertNull(it, CallPhases.fromLinphone(it, false)) }
    }

    @Test fun `every state is classified without throwing`() {
        allStates.forEach { CallPhases.fromLinphone(it, true) }
    }

    @Test fun `failure reasons read like a phone`() {
        assertNull(CallPhases.reasonText("None"))
        assertEquals("Busy", CallPhases.reasonText("Busy"))
        assertEquals("No answer", CallPhases.reasonText("NotAnswered"))
        assertEquals("Network problem", CallPhases.reasonText("IOError"))
        assertEquals("Call failed", CallPhases.reasonText("SomethingNew"))
    }

    @Test fun `config identities follow IMS format`() {
        val c = LineConfig(number = "+919000000000", password = "x")
        assertEquals("sip:+919000000000@ap.ims.airtel.in", c.identity)
        assertEquals("+919000000000@ap.ims.airtel.in", c.authUser)
        assertEquals(true, c.isComplete)
        assertEquals(false, LineConfig().isComplete)
        assertEquals(false, LineConfig(number = "+919000000000").isComplete)
    }
}
