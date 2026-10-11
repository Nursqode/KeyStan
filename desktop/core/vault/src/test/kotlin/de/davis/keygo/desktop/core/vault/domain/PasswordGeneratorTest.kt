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
import kotlin.test.assertTrue

class PasswordGeneratorTest {

    @Test
    fun theRequestedLengthIsHonoured() {
        for (length in listOf(5, 12, 20, 64)) {
            val password = PasswordGenerator.generate(PasswordOptions(length = length))
            assertEquals(length, password.length)
        }
    }

    @Test
    fun everySelectedClassAppears() {
        // A 20-character password missing its symbols is a support ticket, not a nicety.
        repeat(50) {
            val password = PasswordGenerator.generate(PasswordOptions(length = 8))
            assertTrue(password.any { it.isLowerCase() }, "no lowercase in $password")
            assertTrue(password.any { it.isUpperCase() }, "no uppercase in $password")
            assertTrue(password.any { it.isDigit() }, "no digit in $password")
            assertTrue(password.any { !it.isLetterOrDigit() }, "no symbol in $password")
        }
    }

    @Test
    fun unselectedClassesAreAbsent() {
        repeat(20) {
            val password = PasswordGenerator.generate(
                PasswordOptions(length = 24, uppercase = false, symbols = false),
            )
            assertTrue(password.all { it.isLowerCase() || it.isDigit() }, password)
        }
    }

    @Test
    fun ambiguousCharactersCanBeKeptOut() {
        repeat(50) {
            val password = PasswordGenerator.generate(
                PasswordOptions(length = 32, avoidAmbiguous = true),
            )
            assertTrue(password.none { it in "0O1lI|`'\" " }, password)
        }
    }

    @Test
    fun aShorterLengthThanClassesStillYieldsEveryClass() {
        val password = PasswordGenerator.generate(PasswordOptions(length = 2))

        assertEquals(4, password.length)
        assertTrue(password.any { it.isLowerCase() })
        assertTrue(password.any { !it.isLetterOrDigit() })
    }

    @Test
    fun selectingNothingYieldsNothingRatherThanAFallbackSecret() {
        val password = PasswordGenerator.generate(
            PasswordOptions(lowercase = false, uppercase = false, digits = false, symbols = false),
        )

        assertEquals("", password)
    }

    @Test
    fun twoGeneratedPasswordsDiffer() {
        // A generator that returns the same value twice would still pass every test above.
        val first = PasswordGenerator.generate(PasswordOptions(length = 32))
        val second = PasswordGenerator.generate(PasswordOptions(length = 32))

        assertTrue(first != second)
    }

    @Test
    fun strengthGrowsWithLengthAndVariety() {
        val weak = PasswordStrength.entropyBits("aaaaaaaa")
        val medium = PasswordStrength.entropyBits("correcthorsebatterystaple")
        val strong = PasswordStrength.entropyBits(PasswordGenerator.generate(PasswordOptions(length = 24)))

        assertTrue(weak < medium, "weak=$weak medium=$medium")
        assertTrue(medium < strong, "medium=$medium strong=$strong")
        assertEquals(0.0, PasswordStrength.entropyBits(""))
    }
}
