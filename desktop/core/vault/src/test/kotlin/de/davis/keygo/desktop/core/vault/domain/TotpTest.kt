/*
 * KeyStan Desktop - a Windows companion for the KeyGo Android password manager.
 * Copyright (C) 2026 Nursqode
 *
 * Part of KeyStan (https://github.com/Nursqode/KeyStan), a fork of KeyGo
 * (https://github.com/OffRange/KeyGo). Licensed under the GNU General Public License v3.0 or
 * later, like the project it belongs to; see LICENSE.
 */

package de.davis.keygo.desktop.core.vault.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * RFC 6238 Appendix B vectors.
 *
 * The phone generates these codes, and a user comparing them side by side is the whole point of the
 * feature: being one digit off would look like a broken account rather than a bug here.
 */
class TotpTest {

    private val sha1SecretBase32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
    private val sha1SecretAscii = "12345678901234567890".encodeToByteArray()
    private val sha256Secret = "12345678901234567890123456789012".encodeToByteArray()
    private val sha512Secret =
        "1234567890123456789012345678901234567890123456789012345678901234".encodeToByteArray()

    @Test
    fun base32DecodesToTheSecretItEncodes() {
        // The expected value is the ASCII the RFC used, so this checks the decoder rather than
        // re-checking itself.
        assertTrue(decodeBase32(sha1SecretBase32).contentEquals(sha1SecretAscii))
    }

    @Test
    fun base32AcceptsWhatUsersActuallyPaste() {
        assertTrue(decodeBase32("GEZD GNBV GY3T QOJQ GEZD GNBV GY3T QOJQ").contentEquals(sha1SecretAscii))
        assertTrue(decodeBase32("gezdgnbvgy3tqojqgezdgnbvgy3tqojq").contentEquals(sha1SecretAscii))
        assertTrue(decodeBase32("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ====").contentEquals(sha1SecretAscii))
        assertNull(decodeBase32("not base32!"))
        assertNull(decodeBase32(""))
    }

    @Test
    fun sha1Vectors() {
        assertEquals("94287082", code(sha1SecretAscii, 59, digits = 8))
        assertEquals("07081804", code(sha1SecretAscii, 1111111109, digits = 8))
        assertEquals("14050471", code(sha1SecretAscii, 1111111111, digits = 8))
        assertEquals("89005924", code(sha1SecretAscii, 1234567890, digits = 8))
        assertEquals("69279037", code(sha1SecretAscii, 2000000000, digits = 8))
        assertEquals("65353130", code(sha1SecretAscii, 20000000000, digits = 8))
    }

    @Test
    fun sha256Vectors() {
        assertEquals("46119246", code(sha256Secret, 59, digits = 8, algorithm = TotpAlgorithm.SHA256))
        assertEquals(
            "68084774",
            code(sha256Secret, 1111111109, digits = 8, algorithm = TotpAlgorithm.SHA256),
        )
    }

    @Test
    fun sha512Vectors() {
        assertEquals("90693936", code(sha512Secret, 59, digits = 8, algorithm = TotpAlgorithm.SHA512))
        assertEquals(
            "25091201",
            code(sha512Secret, 1111111109, digits = 8, algorithm = TotpAlgorithm.SHA512),
        )
    }

    @Test
    fun thePhoneDefaultsAreSixDigitsAndThirtySeconds() {
        assertEquals("287082", code(sha1SecretAscii, 59))
        assertEquals("287082", codeOrNull(sha1SecretBase32, 59))
    }

    @Test
    fun theCounterStepsEveryPeriod() {
        // Aligned to a period boundary: 1111111110 is divisible by 30, so +29 stays inside it.
        val first = code(sha1SecretAscii, 1111111110)
        assertEquals(first, code(sha1SecretAscii, 1111111110 + 29))
        assertTrue(first != code(sha1SecretAscii, 1111111110 + 30))
    }

    @Test
    fun remainingSecondsCountsDownWithinThePeriod() {
        assertEquals(30, remainingSeconds(0))
        assertEquals(1, remainingSeconds(29))
        assertEquals(30, remainingSeconds(30))
    }

    @Test
    fun anUnusableSecretYieldsNoCodeRatherThanAnException() {
        // A stored secret is user input from months ago; a screen showing the item has to render a
        // "no code" state, not crash.
        assertNull(codeOrNull("0189 not-valid!", 59))
        assertNull(codeOrNull("", 59))
    }

    @Test
    fun otpauthUrisCarryTheirOwnParameters() {
        val parameters = parse(
            "otpauth://totp/ACME:alice?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&issuer=ACME" +
                "&algorithm=SHA256&digits=8&period=60",
        )

        assertEquals("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", parameters?.secret)
        assertEquals(8, parameters?.digits)
        assertEquals(60, parameters?.period)
        assertEquals(TotpAlgorithm.SHA256, parameters?.algorithm)
    }

    @Test
    fun aBareSecretIsTreatedAsItsOwnSecret() {
        val parameters = parametersFor(sha1SecretBase32)

        assertEquals(sha1SecretBase32, parameters?.secret)
        assertEquals(TotpAlgorithm.SHA1, parameters?.algorithm)
    }

    private fun code(
        secret: ByteArray,
        timeSeconds: Long,
        digits: Int = Totp.DEFAULT_DIGITS,
        algorithm: TotpAlgorithm = Totp.DEFAULT_ALGORITHM,
    ) = Totp.code(secret, timeSeconds, digits, Totp.DEFAULT_PERIOD, algorithm)

    private fun decodeBase32(value: String) = Totp.decodeBase32(value)

    private fun codeOrNull(secret: String, timeSeconds: Long) = Totp.codeOrNull(secret, timeSeconds)

    private fun remainingSeconds(timeSeconds: Long) = Totp.remainingSeconds(timeSeconds)

    private fun parse(uri: String) = Totp.parse(uri)

    private fun parametersFor(secret: String) = Totp.parametersFor(secret)
}
