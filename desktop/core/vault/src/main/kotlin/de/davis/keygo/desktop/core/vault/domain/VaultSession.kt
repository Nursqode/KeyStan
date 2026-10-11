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
import de.davis.keygo.desktop.core.backup.model.Vault
import de.davis.keygo.desktop.core.vault.data.SealedBackupFile
import de.davis.keygo.desktop.core.vault.data.VaultFiles

/** Which part of the vault an export writes. */
sealed interface ExportScope {
    /** Every vault, which is what the Android app's restore expects. */
    data object Everything : ExportScope

    /** Only one vault, for handing a colleague or a second device a subset. */
    data class SingleVault(val index: Int) : ExportScope
}

/** How an imported file is applied to the local vault. */
sealed interface ImportMode {
    /**
     * Replaces the whole local vault with the file's contents.
     *
     * This is what Android's JSON restore does, and the only mode that reproduces a device exactly.
     */
    data object Restore : ImportMode

    /**
     * Adds the file's items to one local vault, skipping items that are already there.
     *
     * Non-destructive, and the safe default when the user is not sure: a merge can be redone, a
     * restore cannot be undone.
     */
    data class Merge(val targetVaultIndex: Int) : ImportMode
}

data class ImportSummary(val vaults: Int, val logins: Int, val cards: Int)

/**
 * The unlocked vault, and the only place the master passphrase lives while the app runs.
 *
 * It holds the passphrase so that every edit can be persisted immediately without asking again; the
 * moment the session locks, the passphrase is wiped. Nothing here touches the UI or the filesystem
 * directly - persistence goes through [VaultFiles], which is also where a failed save is turned into
 * a value instead of an exception.
 */
class VaultSession(
    private val files: VaultFiles,
    var vaultFile: SealedBackupFile,
) {

    private var passphrase: CharArray? = null

    /** The decrypted vault, or null while locked. */
    var backup: Backup? = null
        private set

    val isUnlocked: Boolean get() = backup != null

    /** True when a vault file already exists at the current path. */
    fun vaultFileExists(): Boolean = vaultFile.exists()

    /** What the existing file would need, for a lock screen that has to explain itself. */
    fun existingFileSource() = files.inspect(vaultFile.path)

    /** Creates a new, empty vault and locks in [passphrase] as its master passphrase. */
    fun createNew(newPassphrase: CharArray, vaultName: String = "Personal"): VaultResult<Backup> {
        if (!PassphrasePolicy.isAcceptable(newPassphrase))
            return VaultResult.Err(VaultError.WeakPassphrase)

        val fresh = Backup(vaults = listOf(Vault(name = vaultName)))
        return when (val result = files.create(vaultFile, newPassphrase, fresh)) {
            is VaultResult.Ok -> {
                adopt(newPassphrase, result.value)
                VaultResult.Ok(result.value)
            }

            is VaultResult.Err -> result
        }
    }

    /** Opens the vault at the current path. */
    fun unlock(candidate: CharArray): VaultResult<Backup> =
        when (val result = files.open(vaultFile, candidate)) {
            is VaultResult.Ok -> {
                adopt(candidate, result.value)
                VaultResult.Ok(result.value)
            }

            is VaultResult.Err -> result
        }

    /**
     * Applies [transform] and writes the result.
     *
     * The in-memory value is only replaced once the file has been written, so a save that fails
     * leaves the screen showing what is actually on disk.
     */
    fun mutate(transform: (Backup) -> Backup): VaultResult<Backup> {
        val current = backup ?: return VaultResult.Err(VaultError.WrongPassphrase)
        val updated = transform(current)
        if (updated == current) return VaultResult.Ok(current)

        val secret = passphrase ?: return VaultResult.Err(VaultError.WrongPassphrase)
        return when (val result = files.save(vaultFile, secret, updated)) {
            is VaultResult.Ok -> {
                backup = updated
                VaultResult.Ok(updated)
            }

            is VaultResult.Err -> result
        }
    }

    /** Re-seals the vault under a new master passphrase. */
    fun changePassphrase(newPassphrase: CharArray): VaultResult<Unit> {
        if (!PassphrasePolicy.isAcceptable(newPassphrase))
            return VaultResult.Err(VaultError.WeakPassphrase)

        val current = backup ?: return VaultResult.Err(VaultError.WrongPassphrase)
        return when (val result = files.save(vaultFile, newPassphrase, current)) {
            is VaultResult.Ok -> {
                passphrase?.fill('\u0000')
                passphrase = newPassphrase.copyOf()
                VaultResult.Ok(Unit)
            }

            is VaultResult.Err -> result
        }
    }

    /** Writes the vault - or one vault - to [path] under its own passphrase, for sharing. */
    fun exportTo(path: java.nio.file.Path, exportPassphrase: CharArray, scope: ExportScope): VaultResult<Unit> {
        val current = backup ?: return VaultResult.Err(VaultError.WrongPassphrase)
        val toWrite = when (scope) {
            ExportScope.Everything -> current
            is ExportScope.SingleVault -> {
                val vault = current.vaults.getOrNull(scope.index) ?: return VaultResult.Err(VaultError.NoVaultFile)
                Backup(vaults = listOf(vault))
            }
        }
        return files.writeBackup(path, exportPassphrase, toWrite)
    }

    /** Applies a backup file to the local vault, either replacing it or merging into one vault. */
    fun importFrom(path: java.nio.file.Path, importPassphrase: CharArray, mode: ImportMode): VaultResult<ImportSummary> {
        val current = backup ?: return VaultResult.Err(VaultError.WrongPassphrase)

        val incoming = when (val result = files.openBackup(path, importPassphrase)) {
            is VaultResult.Ok -> result.value
            is VaultResult.Err -> return result
        }

        val merged = when (mode) {
            ImportMode.Restore -> incoming

            is ImportMode.Merge -> incoming.vaults.foldIndexed(current) { _, acc, _ -> acc }.let { base ->
                // Every incoming vault's items land in the one vault the user chose; a merge that
                // invented vaults would silently reorganise the local file.
                incoming.vaults.fold(base) { acc, source ->
                    mergeVaultContents(acc, source, mode.targetVaultIndex)
                }
            }
        }

        val summary = ImportSummary(
            vaults = incoming.vaults.size,
            logins = incoming.vaults.sumOf { it.logins.size },
            cards = incoming.vaults.sumOf { it.cards.size },
        )

        return when (val result = mutate { merged }) {
            is VaultResult.Ok -> VaultResult.Ok(summary)
            is VaultResult.Err -> result
        }
    }

    /** Writes the current vault to a different file, which then becomes the active one. */
    fun saveAs(path: java.nio.file.Path, forPassphrase: CharArray): VaultResult<Unit> {
        val current = backup ?: return VaultResult.Err(VaultError.WrongPassphrase)
        return when (val result = files.writeBackup(path, forPassphrase, current)) {
            is VaultResult.Ok -> {
                passphrase?.fill('\u0000')
                passphrase = forPassphrase.copyOf()
                vaultFile = SealedBackupFile(path)
                VaultResult.Ok(Unit)
            }

            is VaultResult.Err -> result
        }
    }

    /** Drops the decrypted vault and the passphrase. */
    fun lock() {
        backup = null
        passphrase?.fill('\u0000')
        passphrase = null
    }

    private fun adopt(newPassphrase: CharArray, unlocked: Backup) {
        passphrase?.fill('\u0000')
        passphrase = newPassphrase.copyOf()
        backup = unlocked
    }

    /**
     * Adds [source]'s items to the vault at [targetVaultIndex], skipping titles already present.
     *
     * Duplicates are matched on the fields that make an entry recognisable - a login's title with
     * its username, a card's title with its number - because the format carries no ids.
     */
    private fun mergeVaultContents(backup: Backup, source: Vault, targetVaultIndex: Int): Backup =
        BackupEditor.updateVault(backup, targetVaultIndex) { target ->
            val existingLogins = target.logins.map { it.title to it.username }.toSet()
            val existingCards = target.cards.map { it.title to it.number }.toSet()
            target.copy(
                logins = target.logins + source.logins.filter { (it.title to it.username) !in existingLogins },
                cards = target.cards + source.cards.filter { (it.title to it.number) !in existingCards },
            )
        }
}
