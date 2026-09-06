package com.sahin.smfinddevice.security

/**
 * Normalizes phone numbers to a comparable E.164-like form so that the same
 * physical number typed or received in different formats (spaces, dashes,
 * a leading 0, a leading country code, etc.) is recognized as one entry.
 *
 * This is intentionally conservative: it does not attempt full libphonenumber-grade
 * parsing. [defaultCountryCallingCode] should be set from the device's SIM region
 * during setup (defaults to India, "91", matching the +91XXXXXXXXXX example in setup).
 */
class PhoneNumberNormalizer(private val defaultCountryCallingCode: String = "91") {

    /**
     * Returns a normalized "+<countrycode><number>" string, or null if the
     * input does not contain enough digits to plausibly be a phone number.
     */
    fun normalize(rawInput: String): String? {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return null

        val hasLeadingPlus = trimmed.startsWith("+")
        val digitsOnly = trimmed.filter { it.isDigit() }
        if (digitsOnly.length < 8) return null // too short to be a real number

        return when {
            hasLeadingPlus -> "+$digitsOnly"
            digitsOnly.startsWith("00") -> "+${digitsOnly.removePrefix("00")}"
            digitsOnly.length > 10 -> "+$digitsOnly" // already looks like it includes a country code
            digitsOnly.startsWith("0") && digitsOnly.length == 11 ->
                "+$defaultCountryCallingCode${digitsOnly.removePrefix("0")}"
            digitsOnly.length == 10 -> "+$defaultCountryCallingCode$digitsOnly"
            else -> "+$digitsOnly"
        }
    }

    /** True if two raw inputs normalize to the same number (used for duplicate prevention). */
    fun isSameNumber(a: String, b: String): Boolean {
        val na = normalize(a) ?: return false
        val nb = normalize(b) ?: return false
        return na == nb
    }

    /** Matches an inbound SMS sender address (which may arrive in varied formats) against a stored number. */
    fun matchesSender(senderAddress: String, storedE164: String): Boolean {
        val normalizedSender = normalize(senderAddress) ?: return false
        return normalizedSender == storedE164
    }
}
