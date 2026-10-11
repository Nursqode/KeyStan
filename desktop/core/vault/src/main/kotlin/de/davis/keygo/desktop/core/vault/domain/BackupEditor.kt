/*
 * KeyStan Desktop - a Windows companion for the KeyGo Android password manager.
 * Copyright (C) 2026 Nursqode
 *
 * Part of KeyStan (https://github.com/Nursqode/KeyStan), a fork of KeyGo
 * (https://github.com/OffRange/KeyGo). Licensed under the GNU General Public License v3.0 or
 * later, like the project it belongs to; see LICENSE.
 */

package de.davis.keygo.desktop.core.vault.domain

import de.davis.keygo.desktop.core.backup.model.Backup
import de.davis.keygo.desktop.core.backup.model.Card
import de.davis.keygo.desktop.core.backup.model.Login
import de.davis.keygo.desktop.core.backup.model.Vault

/**
 * Points at one item inside a [Backup].
 *
 * The backup format has no item ids - order is the only identity a file carries - so an item is
 * addressed by its position. Positions are held against the *stored* lists, never a sorted view:
 * the list screens sort what they display, and an item selected there keeps pointing at the same
 * element afterwards.
 */
data class ItemRef(
    val vaultIndex: Int,
    val kind: ItemKind,
    val itemIndex: Int,
)

enum class ItemKind { LOGIN, CARD }

/** The item a [ItemRef] addresses, if it still exists. */
sealed interface Item {
    val title: String
    val notes: String?
    val tags: List<String>
    val pinned: Boolean

    data class OfLogin(val login: Login) : Item {
        override val title get() = login.title
        override val notes get() = login.notes
        override val tags get() = login.tags
        override val pinned get() = login.pinned
    }

    data class OfCard(val card: Card) : Item {
        override val title get() = card.title
        override val notes get() = card.notes
        override val tags get() = card.tags
        override val pinned get() = card.pinned
    }
}

/**
 * Edits a [Backup] as an immutable value.
 *
 * Every operation returns a new backup, so a failed save can never leave the in-memory copy and the
 * file disagreeing about what was applied: the UI holds one value and replaces it wholesale.
 * Out-of-range references are ignored rather than throwing, because they arise from ordinary races
 * (delete the item that a detail pane still shows) and are not programmer errors.
 */
object BackupEditor {

    fun itemAt(backup: Backup, ref: ItemRef): Item? {
        val vault = backup.vaults.getOrNull(ref.vaultIndex) ?: return null
        return when (ref.kind) {
            ItemKind.LOGIN -> vault.logins.getOrNull(ref.itemIndex)?.let(Item::OfLogin)
            ItemKind.CARD -> vault.cards.getOrNull(ref.itemIndex)?.let(Item::OfCard)
        }
    }

    fun addVault(backup: Backup, vault: Vault): Backup =
        backup.copy(vaults = backup.vaults + vault)

    fun updateVault(backup: Backup, index: Int, transform: (Vault) -> Vault): Backup =
        backup.copy(vaults = backup.vaults.mapIndexed { i, vault -> if (i == index) transform(vault) else vault })

    /**
     * Removes a vault and its items.
     *
     * A backup with no vaults is not a state the Android app can show, so the last vault is never
     * removed - the caller is expected to have disabled the action, and this is the guard behind it.
     */
    fun removeVault(backup: Backup, index: Int): Backup {
        if (backup.vaults.size <= 1) return backup
        return backup.copy(vaults = backup.vaults.filterIndexed { i, _ -> i != index })
    }

    fun addLogin(backup: Backup, vaultIndex: Int, login: Login): Backup =
        updateVault(backup, vaultIndex) { it.copy(logins = it.logins + login) }

    fun addCard(backup: Backup, vaultIndex: Int, card: Card): Backup =
        updateVault(backup, vaultIndex) { it.copy(cards = it.cards + card) }

    fun updateItem(backup: Backup, ref: ItemRef, transform: (Item) -> Item): Backup =
        updateVault(backup, ref.vaultIndex) { vault ->
            when (ref.kind) {
                ItemKind.LOGIN -> vault.copy(
                    logins = vault.logins.mapIndexed { i, login ->
                        if (i != ref.itemIndex) login
                        else (transform(Item.OfLogin(login)) as Item.OfLogin).login
                    },
                )

                ItemKind.CARD -> vault.copy(
                    cards = vault.cards.mapIndexed { i, card ->
                        if (i != ref.itemIndex) card
                        else (transform(Item.OfCard(card)) as Item.OfCard).card
                    },
                )
            }
        }

    fun removeItem(backup: Backup, ref: ItemRef): Backup =
        updateVault(backup, ref.vaultIndex) { vault ->
            when (ref.kind) {
                ItemKind.LOGIN -> vault.copy(logins = vault.logins.filterIndexed { i, _ -> i != ref.itemIndex })
                ItemKind.CARD -> vault.copy(cards = vault.cards.filterIndexed { i, _ -> i != ref.itemIndex })
            }
        }

    /** Moves an item to another vault, which is the one operation that changes its position. */
    fun moveItem(backup: Backup, ref: ItemRef, targetVaultIndex: Int): Backup {
        val item = itemAt(backup, ref) ?: return backup
        if (targetVaultIndex !in backup.vaults.indices || targetVaultIndex == ref.vaultIndex) return backup

        val without = removeItem(backup, ref)
        // Removing from an earlier vault shifts the target down by one.
        val adjustedTarget = if (ref.vaultIndex < targetVaultIndex) targetVaultIndex - 1 else targetVaultIndex
        return when (item) {
            is Item.OfLogin -> addLogin(without, adjustedTarget, item.login)
            is Item.OfCard -> addCard(without, adjustedTarget, item.card)
        }
    }

    /**
     * Copies every item of one vault into another, skipping items whose title already exists there.
     *
     * This is what an import does: backups arrive from another device holding the same vault names,
     * and re-importing one must not double every entry.
     */
    fun mergeVault(backup: Backup, sourceVaultIndex: Int, targetVaultIndex: Int): Backup {
        val source = backup.vaults.getOrNull(sourceVaultIndex) ?: return backup
        val target = backup.vaults.getOrNull(targetVaultIndex) ?: return backup

        val existingLogins = target.logins.map { it.title to it.username }.toSet()
        val existingCards = target.cards.map { it.title to it.number }.toSet()

        val newLogins = source.logins.filter { (it.title to it.username) !in existingLogins }
        val newCards = source.cards.filter { (it.title to it.number) !in existingCards }

        return updateVault(backup, targetVaultIndex) { vault ->
            vault.copy(logins = vault.logins + newLogins, cards = vault.cards + newCards)
        }
    }
}

/** Item counts for a vault, used by the vault list. */
data class VaultSummary(
    val index: Int,
    val name: String,
    val icon: String,
    val loginCount: Int,
    val cardCount: Int,
) {
    val totalCount: Int get() = loginCount + cardCount
}

fun Backup.summaries(): List<VaultSummary> = vaults.mapIndexed { index, vault ->
    VaultSummary(
        index = index,
        name = vault.name,
        icon = vault.icon,
        loginCount = vault.logins.size,
        cardCount = vault.cards.size,
    )
}
