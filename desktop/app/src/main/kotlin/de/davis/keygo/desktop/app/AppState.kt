/*
 * KeyStan Desktop - a Windows companion for the KeyGo Android password manager.
 * Copyright (C) 2026 Nursqode
 *
 * Part of KeyStan (https://github.com/Nursqode/KeyStan), a fork of KeyGo
 * (https://github.com/OffRange/KeyGo). Licensed under the GNU General Public License v3.0 or
 * later, like the project it belongs to; see LICENSE.
 */

package de.davis.keygo.desktop.app

import de.davis.keygo.desktop.core.backup.model.Card
import de.davis.keygo.desktop.core.backup.model.Login
import de.davis.keygo.desktop.core.vault.data.AppSettings
import de.davis.keygo.desktop.core.vault.domain.Item
import de.davis.keygo.desktop.core.vault.domain.ItemKind
import de.davis.keygo.desktop.core.vault.domain.ItemRef
import de.davis.keygo.desktop.core.vault.domain.VaultSummary

/** The pane the window is showing. */
enum class Screen { VAULT, GENERATOR, SETTINGS, IMPORT, EXPORT }

/** Which modal is open, if any. */
enum class AppDialog { NONE, NEW_VAULT, EDIT_ITEM, CONFIRM_DELETE_ITEM, CONFIRM_DELETE_VAULT, CHANGE_PASSPHRASE, RENAME_VAULT }

/** One line of the item list. */
data class ItemRow(
    val ref: ItemRef,
    val kind: ItemKind,
    val title: String,
    val subtitle: String,
    val pinned: Boolean,
    val tags: List<String>,
    /** Kept raw so the row's code can be recomputed on every tick without touching the vault. */
    val totpSecret: String?,
    val websites: List<String>,
)

/** Everything the window renders. One immutable value, replaced wholesale on every change. */
data class AppState(
    val locked: Boolean = true,
    val hasVaultFile: Boolean = false,
    val vaultFileName: String = "vault.ksb",
    val busy: Boolean = false,
    val busyLabel: String? = null,
    val screen: Screen = Screen.VAULT,
    val vaults: List<VaultSummary> = emptyList(),
    val selectedVaultIndex: Int = 0,
    val selectedItem: ItemRef? = null,
    val query: String = "",
    val rows: List<ItemRow> = emptyList(),
    val detail: Item? = null,
    val darkTheme: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val dialog: AppDialog = AppDialog.NONE,
    /** The item the editor dialog is open on, if it is. */
    val editing: ItemDraft? = null,
    val revealSecrets: Boolean = false,
    val status: String? = null,
    val lockError: String? = null,
    val totpNowSeconds: Long = 0,
) {
    val selectedVault: VaultSummary? get() = vaults.getOrNull(selectedVaultIndex)
}

/** The item the editor dialog is working on, so a half-typed edit survives a recomposition. */
data class ItemDraft(
    val ref: ItemRef?,
    val vaultIndex: Int,
    val kind: ItemKind,
    val login: Login = Login(),
    val card: Card = Card(),
)

/** Human-readable subtitles for list rows. */
object ItemFormatting {

    fun loginSubtitle(login: Login): String =
        login.username?.takeIf { it.isNotBlank() }
            ?: login.websites.firstOrNull()?.let(::host)
            ?: "Login"

    fun cardSubtitle(card: Card): String = maskedCardNumber(card.number)

    /** Groups a card number into fours, which is how it is printed on the card. */
    fun formatCardNumber(number: String): String =
        number.filter { !it.isWhitespace() }.chunked(4).joinToString(" ")

    /** The last four digits, which is all a list should show. */
    fun maskedCardNumber(number: String): String {
        val digits = number.filter { it.isDigit() }
        if (digits.isEmpty()) return "Card"
        return "•••• ${digits.takeLast(4)}"
    }

    fun expiry(card: Card): String? {
        val month = card.expirationMonth ?: return null
        val year = card.expirationYear ?: return null
        return "%02d/%02d".format(month, year % 100)
    }

    /** The host of a stored URL, for a subtitle that does not repeat "https://". */
    fun host(url: String): String = url
        .removePrefix("https://")
        .removePrefix("http://")
        .substringBefore('/')
        .takeIf { it.isNotBlank() }
        ?: url
}
