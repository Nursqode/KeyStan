/*
 * KeyStan Desktop - a Windows companion for the KeyGo Android password manager.
 * Copyright (C) 2026 Nursqode
 *
 * Part of KeyStan (https://github.com/Nursqode/KeyStan), a fork of KeyGo
 * (https://github.com/OffRange/KeyGo). Licensed under the GNU General Public License v3.0 or
 * later, like the project it belongs to; see LICENSE.
 */

package de.davis.keygo.desktop.core.vault.domain

import java.security.SecureRandom

/** Which character classes a generated password draws from. */
data class PasswordOptions(
    val length: Int = 20,
    val lowercase: Boolean = true,
    val uppercase: Boolean = true,
    val digits: Boolean = true,
    val symbols: Boolean = true,
    /**
     * Drops the characters that are hard to tell apart when a password is read aloud or copied by
     * eye (0/O, 1/l/I). Off by default on the phone as well, so a password generated here looks
     * like one generated there.
     */
    val avoidAmbiguous: Boolean = true,
    /** Excludes characters some sites reject outright in a password field. */
    val avoidProblematic: Boolean = false,
) {
    val characterClasses: Int
        get() = listOf(lowercase, uppercase, digits, symbols).count { it }
}

/**
 * Generates random passwords.
 *
 * Every index comes from [SecureRandom] rather than `Random`: a password is a secret, and a
 * predictable generator would make the whole vault's contents guessable from one sample.
 */
object PasswordGenerator {

    private val random = SecureRandom()

    private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val DIGITS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()-_=+[]{};:,.?/"

    private const val AMBIGUOUS = "0O1lI|`'\""
    private const val PROBLEMATIC = "<>&\"'\\/"

    /** A password drawn from [options], or the empty string when no class is selected. */
    fun generate(options: PasswordOptions): String {
        val pools = buildList {
            if (options.lowercase) add(filter(LOWERCASE, options))
            if (options.uppercase) add(filter(UPPERCASE, options))
            if (options.digits) add(filter(DIGITS, options))
            if (options.symbols) add(filter(SYMBOLS, options))
        }.filter { it.isNotEmpty() }

        if (pools.isEmpty()) return ""
        val length = options.length.coerceAtLeast(pools.size)

        // Draw one character from every selected class first, so that a password always contains
        // each class the user asked for instead of leaving it to chance.
        val required = pools.map { pool -> pool[random.nextInt(pool.length)] }.toMutableList()
        val everything = pools.joinToString("")
        while (required.size < length) required.add(everything[random.nextInt(everything.length)])

        // Shuffle so the guaranteed characters are not always in the same positions.
        for (i in required.indices.reversed()) {
            val j = random.nextInt(i + 1)
            val swap = required[i]
            required[i] = required[j]
            required[j] = swap
        }
        return required.joinToString("")
    }

    private fun filter(alphabet: String, options: PasswordOptions): String = alphabet.filter { char ->
        (!options.avoidAmbiguous || char !in AMBIGUOUS) &&
            (!options.avoidProblematic || char !in PROBLEMATIC)
    }
}

/** A rough strength readout for the UI, in bits of entropy. */
object PasswordStrength {

    fun entropyBits(password: String): Double {
        if (password.isEmpty()) return 0.0
        val pool = buildString {
            if (password.any { it.isLowerCase() }) append(LOWERCASE_ALPHABET)
            if (password.any { it.isUpperCase() }) append(UPPERCASE_ALPHABET)
            if (password.any { it.isDigit() }) append(DIGIT_ALPHABET)
            if (password.any { !it.isLetterOrDigit() }) append(SYMBOL_ALPHABET)
        }
        val distinct = password.toSet().size
        // Counting distinct characters as well as the alphabet keeps "aaaaaaaa" from reading as
        // strong just because it is long.
        val effectivePool = minOf(pool.length.toDouble(), distinct * 4.0 + pool.length / 4.0)
        if (effectivePool <= 1.0) return 0.0
        return password.length * (kotlin.math.ln(effectivePool) / kotlin.math.ln(2.0))
    }

    private const val LOWERCASE_ALPHABET = "abcdefghijklmnopqrstuvwxyz"
    private const val UPPERCASE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val DIGIT_ALPHABET = "0123456789"
    private const val SYMBOL_ALPHABET = "!@#$%^&*()-_=+[]{};:,.?/"
}
