/*
 * KeyStan Desktop - a Windows companion for the KeyGo Android password manager.
 * Copyright (C) 2026 Nursqode
 *
 * Part of KeyStan (https://github.com/Nursqode/KeyStan), a fork of KeyGo
 * (https://github.com/OffRange/KeyGo). Licensed under the GNU General Public License v3.0 or
 * later, like the project it belongs to; see LICENSE.
 */

package de.davis.keygo.desktop.core.vault.domain

import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Hash an otpauth secret may be paired with. */
enum class TotpAlgorithm(val macName: String) {
    SHA1("HmacSHA1"),
    SHA256("HmacSHA256"),
    SHA512("HmacSHA512"),
}

/**
 * Time-based one-time passwords (RFC 6238), which is what a login's `totp_secret` holds.
 *
 * The Rust core generates these on Android; this is the desktop's own implementation because the
 * code has to appear in the UI on every tick and cannot cross a process boundary to do it. The
 * parameters match the Rust defaults - 6 digits, 30 seconds, SHA-1 - and any deviation is recorded
 * in the otpauth URI a login may carry.
 */
object Totp {

    const val DEFAULT_DIGITS = 6
    const val DEFAULT_PERIOD = 30
    val DEFAULT_ALGORITHM = TotpAlgorithm.SHA1

    /** The code for [timeSeconds], as a zero-padded decimal string. */
    fun code(
        secret: ByteArray,
        timeSeconds: Long,
        digits: Int = DEFAULT_DIGITS,
        period: Int = DEFAULT_PERIOD,
        algorithm: TotpAlgorithm = DEFAULT_ALGORITHM,
    ): String {
        require(digits in 6..10) { "TOTP digits must be between 6 and 10, was $digits" }
        require(period > 0) { "TOTP period must be positive, was $period" }
        require(secret.isNotEmpty()) { "TOTP secret must not be empty" }

        val counter = timeSeconds / period
        val message = ByteArray(8) { index -> (counter ushr (56 - 8 * index)).toByte() }

        val mac = Mac.getInstance(algorithm.macName)
        mac.init(SecretKeySpec(secret, algorithm.macName))
        val digest = mac.doFinal(message)

        // Dynamic truncation: the low nibble of the last byte picks the 4-byte window, whose top bit
        // is cleared so the result is positive regardless of the JVM's signed bytes.
        val offset = digest.last().toInt() and 0x0F
        val binary = ((digest[offset].toInt() and 0x7F) shl 24) or
            ((digest[offset + 1].toInt() and 0xFF) shl 16) or
            ((digest[offset + 2].toInt() and 0xFF) shl 8) or
            (digest[offset + 3].toInt() and 0xFF)

        val code = binary % POWERS_OF_TEN[digits]
        return code.toString().padStart(digits, '0')
    }

    /** Seconds until the current code is replaced. */
    fun remainingSeconds(timeSeconds: Long, period: Int = DEFAULT_PERIOD): Int =
        (period - (timeSeconds % period)).toInt()

    /**
     * The code for a base32 secret, as stored on a login.
     *
     * Returns null rather than throwing when the secret cannot be used: a stored secret is user
     * input, and a screen showing an item must render a "no code" state instead of crashing on a
     * typo the user made months ago.
     */
    fun codeOrNull(
        base32Secret: String,
        timeSeconds: Long,
        digits: Int = DEFAULT_DIGITS,
        period: Int = DEFAULT_PERIOD,
        algorithm: TotpAlgorithm = DEFAULT_ALGORITHM,
    ): String? {
        val secret = decodeBase32(base32Secret) ?: return null
        if (secret.isEmpty()) return null
        return runCatching { code(secret, timeSeconds, digits, period, algorithm) }.getOrNull()
    }

    /**
     * RFC 4648 base32, case-insensitive, tolerating the spaces users paste along with a secret.
     *
     * Returns null on any character outside the alphabet, and on a length that cannot be a whole
     * number of bytes.
     */
    fun decodeBase32(secret: String): ByteArray? {
        val normalized = secret.filterNot { it == ' ' || it == '-' }.uppercase().trimEnd('=')
        if (normalized.isEmpty()) return null

        val out = java.io.ByteArrayOutputStream()
        var buffer = 0
        var bits = 0
        for (char in normalized) {
            val value = ALPHABET.indexOf(char)
            if (value < 0) return null
            buffer = (buffer shl 5) or value
            bits += 5
            if (bits >= 8) {
                bits -= 8
                out.write((buffer ushr bits) and 0xFF)
            }
        }
        return out.toByteArray()
    }

    /**
     * Parses an `otpauth://totp/...` URI into its parameters.
     *
     * The Android app stores whatever the user scanned here, so a login's TOTP secret may be a bare
     * base32 string or a full URI carrying a non-default algorithm, digit count or period.
     */
    fun parse(otpauthUri: String): Parameters? {
        if (!otpauthUri.startsWith("otpauth://", ignoreCase = true)) return null

        val queryStart = otpauthUri.indexOf('?')
        if (queryStart < 0) return null

        val parameters = otpauthUri.substring(queryStart + 1)
            .split('&')
            .mapNotNull { part ->
                val separator = part.indexOf('=')
                if (separator <= 0) null
                else part.substring(0, separator) to part.substring(separator + 1)
            }
            .toMap()

        val secret = parameters["secret"] ?: return null
        return Parameters(
            secret = secret,
            digits = parameters["digits"]?.toIntOrNull() ?: DEFAULT_DIGITS,
            period = parameters["period"]?.toIntOrNull() ?: DEFAULT_PERIOD,
            algorithm = when (parameters["algorithm"]?.uppercase()) {
                "SHA256" -> TotpAlgorithm.SHA256
                "SHA512" -> TotpAlgorithm.SHA512
                else -> TotpAlgorithm.SHA1
            },
        )
    }

    /** A secret in whatever form a login stores it: a bare base32 string, or an otpauth URI. */
    fun parametersFor(storedSecret: String): Parameters? =
        parse(storedSecret) ?: Parameters(storedSecret)

    data class Parameters(
        val secret: String,
        val digits: Int = DEFAULT_DIGITS,
        val period: Int = DEFAULT_PERIOD,
        val algorithm: TotpAlgorithm = DEFAULT_ALGORITHM,
    )

    /** Exposed for tests: the raw bytes behind a base32 secret, or null when it is not decodable. */
    internal fun secretBytes(parameters: Parameters): ByteArray? = decodeBase32(parameters.secret)

    private val POWERS_OF_TEN = IntArray(11) { power ->
        var result = 1
        repeat(power) { result *= 10 }
        result
    }

    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    /** Base64 is used by the QR/URI form of some secrets; kept here so callers do not guess. */
    internal fun decodeBase64OrBase32(value: String): ByteArray? =
        decodeBase32(value) ?: runCatching { Base64.getDecoder().decode(value) }.getOrNull()
}
