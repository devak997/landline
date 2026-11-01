package dev.deva.devphone.line

object PhoneNumbers {
    private const val COUNTRY = "91"

    fun toDialString(raw: String): String? {
        val kept = raw.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
        if (kept.isEmpty()) return null
        if (kept.lastIndexOf('+') > 0) return null

        if (kept.any { it == '*' || it == '#' }) return kept.takeIf { !it.startsWith("+") }

        val digits = kept.removePrefix("+")
        if (kept.startsWith("+")) {
            return if (digits.length in 8..15) "+$digits" else null
        }
        return nationalToDial(digits)
    }

    private fun nationalToDial(digits: String): String? = when {
        digits.length in 3..5 -> digits
        isTenDigitMobile(digits) -> "+$COUNTRY$digits"
        isTrunkPrefixed(digits) -> "+$COUNTRY${digits.drop(1)}"
        digits.length == 12 && digits.startsWith(COUNTRY) -> "+$digits"
        digits.startsWith("00") && digits.length in 10..17 -> "+${digits.drop(2)}"
        isTollFree(digits) -> digits
        digits.length in 6..8 && digits[0] != '0' -> digits
        else -> null
    }

    private fun isTenDigitMobile(d: String) = d.length == 10 && d[0] in '6'..'9'

    private fun isTrunkPrefixed(d: String) = d.length == 11 && d[0] == '0' && d[1] != '0'

    private fun isTollFree(d: String) = (d.startsWith("1800") || d.startsWith("1860")) && d.length in 10..11

    fun fromUri(uri: String): String {
        val noScheme = uri.substringAfter("sip:").substringAfter("sips:").substringAfter("tel:")
        return noScheme.substringBefore('@').substringBefore(';').trim()
    }

    fun matchKey(number: String): String = number.filter(Char::isDigit).takeLast(10)

    fun pretty(number: String): String {
        val d = number.filter(Char::isDigit)
        val isIndian = number.startsWith("+91") && d.length == 12
        return when {
            isIndian && d[2] in '6'..'9' -> "+91 ${d.substring(2, 7)} ${d.substring(7)}"
            isIndian -> "+91 ${d.substring(2, 5)} ${d.substring(5, 8)} ${d.substring(8)}"
            d.length == 10 && d[0] in '6'..'9' -> "${d.substring(0, 5)} ${d.substring(5)}"
            else -> number
        }
    }
}
