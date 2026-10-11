/*
 * KeyStan Desktop - a Windows companion for the KeyGo Android password manager.
 * Copyright (C) 2026 Nursqode
 *
 * Part of KeyStan (https://github.com/Nursqode/KeyStan), a fork of KeyGo
 * (https://github.com/OffRange/KeyGo). Licensed under the GNU General Public License v3.0 or
 * later, like the project it belongs to; see LICENSE.
 */

package de.davis.keygo.desktop.core.ui.i18n

import de.davis.keygo.desktop.core.vault.data.AppLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class StringsTest {

    private val catalogs = listOf("English" to EnglishStrings, "Russian" to RussianStrings)

    @Test
    fun aLanguageChoiceSelectsItsCatalog() {
        assertEquals(EnglishStrings, stringsFor(AppLanguage.ENGLISH))
        assertEquals(RussianStrings, stringsFor(AppLanguage.RUSSIAN))
    }

    @Test
    fun followingTheSystemFollowsTheJvmLocale() {
        val original = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("ru-RU"))
            assertEquals(RussianStrings, stringsFor(AppLanguage.SYSTEM))

            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("en-GB"))
            assertEquals(EnglishStrings, stringsFor(AppLanguage.SYSTEM))

            // A language without a catalog falls back to English rather than to an empty window.
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("de-DE"))
            assertEquals(EnglishStrings, stringsFor(AppLanguage.SYSTEM))
        } finally {
            java.util.Locale.setDefault(original)
        }
    }

    @Test
    fun noStringIsBlank() {
        // Reflection over the interface is what makes this a completeness check rather than a
        // sample: a new string left empty in a translation fails here instead of reaching a screen.
        for ((name, catalog) in catalogs) {
            val methods = Strings::class.java.methods.filter { it.declaringClass == Strings::class.java }
            assertTrue(methods.size > 100, "expected the whole catalog, saw ${methods.size} members")

            for (method in methods) {
                val arguments: Array<Any> = method.parameterTypes
                    .map { type ->
                        when {
                            type == Int::class.javaPrimitiveType -> 2
                            type == String::class.java -> "x"
                            else -> fail("$name.${method.name} has an unsupported parameter: $type")
                        } as Any
                    }
                    .toTypedArray()

                val value = method.invoke(catalog, *arguments) as String
                assertTrue(value.isNotBlank(), "$name.${method.name} is blank")
            }
        }
    }

    @Test
    fun theRussianCatalogIsNotJustACopy() {
        assertTrue(EnglishStrings.about != RussianStrings.about)
        assertTrue(EnglishStrings.password != RussianStrings.password)
        assertTrue(EnglishStrings.lock != RussianStrings.lock)
    }

    @Test
    fun russianCountsUseTheRightPluralForm() {
        // One, few and many - plus the teens, which take the "many" form in Russian even though
        // their last digit is 1 to 4.
        assertTrue(RussianStrings.vaultCounts(1, 1).startsWith("1 логин · 1 карта"), RussianStrings.vaultCounts(1, 1))
        assertTrue(RussianStrings.vaultCounts(2, 3).startsWith("2 логина · 3 карты"), RussianStrings.vaultCounts(2, 3))
        assertTrue(RussianStrings.vaultCounts(5, 11).startsWith("5 логинов · 11 карт"), RussianStrings.vaultCounts(5, 11))
        assertTrue(RussianStrings.vaultCounts(21, 22).startsWith("21 логин · 22 карты"), RussianStrings.vaultCounts(21, 22))
        assertTrue(RussianStrings.vaultCounts(0, 0).startsWith("0 логинов · 0 карт"), RussianStrings.vaultCounts(0, 0))

        assertTrue(RussianStrings.itemCount(1).startsWith("1 запись"), RussianStrings.itemCount(1))
        assertTrue(RussianStrings.itemCount(4).startsWith("4 записи"), RussianStrings.itemCount(4))
        assertTrue(RussianStrings.itemCount(12).startsWith("12 записей"), RussianStrings.itemCount(12))
    }

    @Test
    fun englishCountsStayReadable() {
        assertEquals("1 logins · 2 cards", EnglishStrings.vaultCounts(1, 2))
        assertEquals("3 items", EnglishStrings.itemCount(3))
    }

    @Test
    fun everyVaultFailureHasText() {
        val errors = listOf(
            de.davis.keygo.desktop.core.vault.domain.VaultError.NoVaultFile,
            de.davis.keygo.desktop.core.vault.domain.VaultError.WrongPassphrase,
            de.davis.keygo.desktop.core.vault.domain.VaultError.WeakPassphrase,
            de.davis.keygo.desktop.core.vault.domain.VaultError.NeedsAccountKey,
            de.davis.keygo.desktop.core.vault.domain.VaultError.PassphraseMismatch,
            de.davis.keygo.desktop.core.vault.domain.VaultError.Unreadable("damaged"),
            de.davis.keygo.desktop.core.vault.domain.VaultError.Io("disk"),
        )

        for ((name, catalog) in catalogs) {
            for (error in errors) {
                val text = catalog.textFor(error)
                assertTrue(text.isNotBlank(), "$name has no text for $error")
                assertTrue(!text.contains("null"), "$name rendered $error as \"$text\"")
            }
        }
    }
}
