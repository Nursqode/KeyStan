/*
 * KeyStan Desktop - a Windows companion for the KeyGo Android password manager.
 * Copyright (C) 2026 Nursqode
 *
 * Part of KeyStan (https://github.com/Nursqode/KeyStan), a fork of KeyGo
 * (https://github.com/OffRange/KeyGo). Licensed under the GNU General Public License v3.0 or
 * later, like the project it belongs to; see LICENSE.
 */

package de.davis.keygo.desktop.core.vault.data

import java.nio.file.Path
import de.davis.keygo.desktop.core.backup.BackupCredential
import de.davis.keygo.desktop.core.backup.BackupError
import de.davis.keygo.desktop.core.backup.JsonBackupFormat
import de.davis.keygo.desktop.core.backup.KeySource
import de.davis.keygo.desktop.core.backup.model.Backup
import de.davis.keygo.desktop.core.backup.toUtf8Bytes
import de.davis.keygo.desktop.core.vault.domain.VaultError
import de.davis.keygo.desktop.core.vault.domain.VaultResult

/**
 * Reads and writes sealed backups, translating format failures into the errors the UI speaks.
 *
 * This is the only place that knows both the backup module and the filesystem, which is what keeps
 * [de.davis.keygo.desktop.core.vault.domain.VaultSession] - and therefore the UI - free of
 * `BackupError` and `IOException` handling.
 */
class VaultFiles {

    fun create(file: SealedBackupFile, passphrase: CharArray, backup: Backup): VaultResult<Backup> =
        sealInto(file, passphrase, backup)

    fun open(file: SealedBackupFile, passphrase: CharArray): VaultResult<Backup> {
        if (!file.exists()) return VaultResult.Err(VaultError.NoVaultFile)
        return openBackup(file.path, passphrase)
    }

    fun save(file: SealedBackupFile, passphrase: CharArray, backup: Backup): VaultResult<Backup> =
        sealInto(file, passphrase, backup)

    /** What a file needs to be opened, without decrypting it. */
    fun inspect(path: Path): KeySource? =
        if (java.nio.file.Files.isRegularFile(path)) SealedBackupFile(path).inspect() else null

    /** Reads any backup file, wherever it lives - used by import. */
    fun openBackup(path: Path, passphrase: CharArray): VaultResult<Backup> {
        val text = try {
            java.nio.file.Files.readString(path)
        } catch (e: java.io.IOException) {
            return VaultResult.Err(VaultError.Io(e.message ?: "cannot read ${path.fileName}"))
        }

        // An ARK-sealed file is worth its own answer: the user needs to know the file is fine and
        // it is the *key* that the desktop cannot have.
        if (runCatching { JsonBackupFormat.inspect(text) }.getOrNull() == KeySource.ARK)
            return VaultResult.Err(VaultError.NeedsAccountKey)

        val bytes = passphrase.toUtf8Bytes()
        return try {
            VaultResult.Ok(JsonBackupFormat.import(text, BackupCredential.Passphrase(bytes)))
        } catch (e: BackupError) {
            VaultResult.Err(e.toVaultError())
        } catch (e: Exception) {
            VaultResult.Err(VaultError.Unreadable(e.message ?: "cannot read the backup"))
        } finally {
            bytes.fill(0)
        }
    }

    /** Writes [backup] to [path], sealed with [passphrase] - used by export. */
    fun writeBackup(path: Path, passphrase: CharArray, backup: Backup): VaultResult<Unit> =
        when (val sealed = sealInto(SealedBackupFile(path), passphrase, backup)) {
            is VaultResult.Ok -> VaultResult.Ok(Unit)
            is VaultResult.Err -> sealed
        }

    private fun sealInto(file: SealedBackupFile, passphrase: CharArray, backup: Backup): VaultResult<Backup> {
        val bytes = passphrase.toUtf8Bytes()
        return try {
            val text = JsonBackupFormat.export(backup, BackupCredential.Passphrase(bytes))
            file.write(text)
            VaultResult.Ok(backup)
        } catch (e: BackupError) {
            VaultResult.Err(e.toVaultError())
        } catch (e: java.io.IOException) {
            VaultResult.Err(VaultError.Io(e.message ?: "cannot write ${file.path.fileName}"))
        } catch (e: Exception) {
            VaultResult.Err(VaultError.Io(e.message ?: "cannot write ${file.path.fileName}"))
        } finally {
            bytes.fill(0)
        }
    }
}

/**
 * Maps a format failure onto the reason the user is shown.
 *
 * A failed AEAD tag is the only failure that means "wrong passphrase"; everything else means the
 * file itself is the problem, and saying "wrong passphrase" there would send the user hunting for a
 * password that was never going to work.
 */
internal fun BackupError.toVaultError(): VaultError = when (this) {
    is BackupError.Crypto ->
        if (kind == BackupError.CryptoKind.DECRYPTION) VaultError.WrongPassphrase
        else VaultError.Unreadable(message ?: "the file could not be decrypted")

    is BackupError.Json -> VaultError.Unreadable(message ?: "the file is not a backup")
    BackupError.Base64 -> VaultError.Unreadable("the file is damaged")
    is BackupError.UnsupportedVersion -> VaultError.Unreadable("backup version $version is not supported")
    BackupError.MalformedHeader -> VaultError.Unreadable("the file's encryption header is damaged")
    BackupError.CredentialMismatch -> VaultError.NeedsAccountKey
}
