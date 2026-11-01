package dev.deva.devphone.ui

import dev.deva.devphone.ui.screens.duration
import dev.deva.devphone.ui.screens.groupOf
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class RecentsFormatTest {
    private val ist = ZoneId.of("Asia/Kolkata")

    private fun at(
        y: Int,
        m: Int,
        d: Int,
        h: Int,
    ) = ZonedDateTime.of(y, m, d, h, 0, 0, 0, ist).toInstant().toEpochMilli()

    @Test fun `day groups`() {
        val now = at(2026, 11, 1, 10)
        assertEquals("Today", groupOf(at(2026, 11, 1, 0), now, ist))
        assertEquals("Yesterday", groupOf(at(2026, 10, 31, 23), now, ist))
        assertEquals("This week", groupOf(at(2026, 10, 27, 9), now, ist))
        assertEquals("Earlier", groupOf(at(2026, 10, 20, 9), now, ist))
    }

    @Test fun `new year boundary`() = assertEquals("Yesterday", groupOf(at(2026, 12, 31, 22), at(2027, 1, 1, 8), ist))

    @Test fun `durations`() {
        assertEquals("0:07", duration(7))
        assertEquals("3:12", duration(192))
        assertEquals("1:01:01", duration(3661))
    }
}
