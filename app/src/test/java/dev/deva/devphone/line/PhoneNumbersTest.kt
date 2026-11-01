package dev.deva.devphone.line

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumbersTest {
    private fun dial(s: String) = PhoneNumbers.toDialString(s)

    @Test fun `mobile in every common shape becomes E164`() {
        listOf("9876543210", "98765 43210", "098765-43210", "+91 98765 43210", "919876543210", "(987) 654-3210")
            .forEach { assertEquals(it, "+919876543210", dial(it)) }
    }

    @Test fun `STD landline with trunk zero`() = assertEquals("+914023456789", dial("040 2345 6789"))

    @Test fun `short and service codes pass through`() {
        assertEquals("198", dial("198"))
        assertEquals("112", dial("112"))
        assertEquals("*121#", dial("*121#"))
        assertEquals("18001234567", dial("1800 123 4567"))
    }

    @Test fun `international forms`() {
        assertEquals("+14155552671", dial("+1 415 555 2671"))
        assertEquals("+447911123456", dial("00 44 7911 123456"))
    }

    @Test fun `garbage is rejected`() {
        listOf("", "abc", "12", "+", "9+876543210", "12345678901234567890", "+12").forEach { assertNull(it, dial(it)) }
    }

    @Test fun `uri parsing`() {
        assertEquals("+919876543210", PhoneNumbers.fromUri("sip:+919876543210@ap.ims.airtel.in;user=phone"))
        assertEquals("+919876543210", PhoneNumbers.fromUri("tel:+919876543210"))
        assertEquals("1234", PhoneNumbers.fromUri("1234"))
    }

    @Test fun `match key ignores formatting and country code`() {
        assertEquals(PhoneNumbers.matchKey("+91 98765 43210"), PhoneNumbers.matchKey("098765-43210"))
    }

    @Test fun `pretty printing`() {
        assertEquals("+91 90000 00000", PhoneNumbers.pretty("+919000000000"))
        assertEquals("+91 402 345 6789", PhoneNumbers.pretty("+914023456789"))
        assertEquals("98765 43210", PhoneNumbers.pretty("9876543210"))
        assertEquals("198", PhoneNumbers.pretty("198"))
    }
}
