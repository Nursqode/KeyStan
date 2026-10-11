/*
 * KeyStan Desktop - a Windows companion for the KeyGo Android password manager.
 * Copyright (C) 2026 Nursqode
 *
 * Part of KeyStan (https://github.com/Nursqode/KeyStan), a fork of KeyGo
 * (https://github.com/OffRange/KeyGo). Licensed under the GNU General Public License v3.0 or
 * later, like the project it belongs to; see LICENSE.
 */

package de.davis.keygo.desktop.core.vault.domain

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import de.davis.keygo.desktop.core.backup.BackupCredential
import de.davis.keygo.desktop.core.backup.JsonBackupFormat
import de.davis.keygo.desktop.core.backup.model.Backup
import de.davis.keygo.desktop.core.backup.model.Card
import de.davis.keygo.desktop.core.backup.model.Login
import de.davis.keygo.desktop.core.backup.model.Vault
import de.davis.keygo.desktop.core.vault.data.SealedBackupFile
import de.davis.keygo.desktop.core.vault.data.VaultFiles

/**
 * The behaviour a user depends on: what they typed comes back after a restart, a wrong passphrase
 * says so, and a file they export opens on the phone.
 */
class VaultSessionTest {

    private val temporary: Path = createTempDirectory("keystan-vault-test")

    @AfterTest
    fun cleanUp() {
        temporary.toFile().deleteRecursively()
    }

    private fun newSession(fileName: String = "vault.ksb") =
        VaultSession(VaultFiles(), SealedBackupFile(temporary.resolve(fileName)))

    private fun passphrase(value: String) = value.toCharArray()

    @Test
    fun aCreatedVaultStartsWithOneVaultAndReopensWithTheSamePassphrase() {
        val session = newSession()
        val created = session.createNew(passphrase("correct horse"))
        assertTrue(created is VaultResult.Ok)
        assertEquals(1, session.backup?.vaults?.size)

        val reopened = newSession().unlock(passphrase("correct horse"))
        assertTrue(reopened is VaultResult.Ok)
        assertEquals("Personal", reopened.valueOrNull()?.vaults?.single()?.name)
    }

    @Test
    fun aWrongPassphraseIsReportedAsSuchAndLeavesTheVaultLocked() {
        newSession().createNew(passphrase("correct horse"))

        val session = newSession()
        val result = session.unlock(passphrase("wrong horse"))

        assertEquals(VaultError.WrongPassphrase, result.errorOrNull())
        assertFalse(session.isUnlocked)
    }

    @Test
    fun aShortPassphraseIsRefusedRatherThanAcceptedWeakly() {
        val result = newSession().createNew(passphrase("short"))

        assertEquals(VaultError.WeakPassphrase, result.errorOrNull())
    }

    @Test
    fun editsArePersistedImmediatelyAndSurviveARestart() {
        val session = newSession()
        session.createNew(passphrase("correct horse"))
        session.mutate { backup ->
            BackupEditor.addLogin(
                backup,
                vaultIndex = 0,
                login = Login(title = "Email", username = "alice", password = "s3cr3t"),
            )
        }

        val reopened = newSession().unlock(passphrase("correct horse"))
        val login = (reopened as VaultResult.Ok).value.vaults.single().logins.single()
        assertEquals("Email", login.title)
        assertEquals("s3cr3t", login.password)
    }

    @Test
    fun lockingForgetsTheVaultAndRefusesFurtherEdits() {
        val session = newSession()
        session.createNew(passphrase("correct horse"))
        session.lock()

        assertFalse(session.isUnlocked)
        assertEquals(VaultError.WrongPassphrase, session.mutate { it }.errorOrNull())
    }

    @Test
    fun changingTheMasterPassphraseInvalidatesTheOldOne() {
        val session = newSession()
        session.createNew(passphrase("correct horse"))
        assertTrue(session.changePassphrase(passphrase("new correct horse")) is VaultResult.Ok)

        assertEquals(VaultError.WrongPassphrase, newSession().unlock(passphrase("correct horse")).errorOrNull())
        assertTrue(newSession().unlock(passphrase("new correct horse")) is VaultResult.Ok)
    }

    @Test
    fun anExportOpensWithItsOwnPassphraseAndIsAPlainBackup() {
        val session = newSession()
        session.createNew(passphrase("correct horse"))
        session.mutate { backup ->
            BackupEditor.addLogin(backup, 0, Login(title = "Email", username = "alice", password = "s3cr3t"))
        }

        val exported = temporary.resolve("export.json")
        assertTrue(session.exportTo(exported, passphrase("share me"), ExportScope.Everything) is VaultResult.Ok)

        // Read it exactly as the Android app would: a passphrase-sealed v1 envelope.
        val backup = JsonBackupFormat.import(
            Files.readString(exported),
            BackupCredential.Passphrase("share me".toByteArray()),
        )
        assertEquals("s3cr3t", backup.vaults.single().logins.single().password)
    }

    @Test
    fun aSingleVaultExportCarriesOnlyThatVault() {
        val session = newSession()
        session.createNew(passphrase("correct horse"))
        session.mutate { backup ->
            BackupEditor.addVault(backup, Vault(name = "Work"))
                .let { BackupEditor.addLogin(it, 1, Login(title = "VPN", password = "s3cr3t")) }
        }

        val exported = temporary.resolve("work.json")
        session.exportTo(exported, passphrase("share me"), ExportScope.SingleVault(1))

        val backup = JsonBackupFormat.import(
            Files.readString(exported),
            BackupCredential.Passphrase("share me".toByteArray()),
        )
        assertEquals(listOf("Work"), backup.vaults.map { it.name })
        assertEquals("VPN", backup.vaults.single().logins.single().title)
    }

    @Test
    fun restoringReplacesEverythingWithTheFilesContents() {
        val source = newSession("source.ksb")
        source.createNew(passphrase("phone pass"))
        source.mutate { BackupEditor.addLogin(it, 0, Login(title = "FromPhone", password = "p")) }
        val sourceFile = temporary.resolve("source.ksb")

        val session = newSession()
        session.createNew(passphrase("correct horse"))
        session.mutate { BackupEditor.addLogin(it, 0, Login(title = "Local", password = "p")) }

        val summary = session.importFrom(sourceFile, passphrase("phone pass"), ImportMode.Restore)

        assertTrue(summary is VaultResult.Ok)
        assertEquals(listOf("FromPhone"), session.backup?.vaults?.single()?.logins?.map { it.title })
    }

    @Test
    fun mergingAddsOnlyWhatIsNotAlreadyThere() {
        val source = newSession("source.ksb")
        source.createNew(passphrase("phone pass"))
        source.mutate { backup ->
            BackupEditor.addLogin(backup, 0, Login(title = "Shared", username = "alice", password = "a"))
                .let { BackupEditor.addLogin(it, 0, Login(title = "OnlyOnPhone", username = "bob", password = "b")) }
        }
        val sourceFile = temporary.resolve("source.ksb")

        val session = newSession()
        session.createNew(passphrase("correct horse"))
        session.mutate { BackupEditor.addLogin(it, 0, Login(title = "Shared", username = "alice", password = "a")) }

        session.importFrom(sourceFile, passphrase("phone pass"), ImportMode.Merge(0))

        assertEquals(
            listOf("Shared", "OnlyOnPhone"),
            session.backup?.vaults?.single()?.logins?.map { it.title },
        )
    }

    @Test
    fun mergingSurvivesARepeatedImport() {
        val source = newSession("source.ksb")
        source.createNew(passphrase("phone pass"))
        source.mutate { BackupEditor.addLogin(it, 0, Login(title = "OnlyOnPhone", password = "b")) }
        val sourceFile = temporary.resolve("source.ksb")

        val session = newSession()
        session.createNew(passphrase("correct horse"))
        session.importFrom(sourceFile, passphrase("phone pass"), ImportMode.Merge(0))
        session.importFrom(sourceFile, passphrase("phone pass"), ImportMode.Merge(0))

        assertEquals(1, session.backup?.vaults?.single()?.logins?.size)
    }

    @Test
    fun aFileSealedWithTheAccountsKeyIsExplainedRatherThanReportedAsWrongPassword() {
        val arkSealed = temporary.resolve("ark.json")
        Files.writeString(
            arkSealed,
            JsonBackupFormat.export(
                Backup(vaults = listOf(Vault(name = "Personal"))),
                BackupCredential.Ark(ByteArray(32) { 5 }),
            ),
        )

        val session = newSession()
        session.createNew(passphrase("correct horse"))
        val result = session.importFrom(arkSealed, passphrase("anything"), ImportMode.Restore)

        assertEquals(VaultError.NeedsAccountKey, result.errorOrNull())
    }

    @Test
    fun aFileThatIsNotABackupSaysSo() {
        val notABackup = temporary.resolve("notes.txt")
        Files.writeString(notABackup, "just some text")

        val session = newSession()
        session.createNew(passphrase("correct horse"))
        val result = session.importFrom(notABackup, passphrase("anything"), ImportMode.Restore)

        assertTrue(result.errorOrNull() is VaultError.Unreadable)
    }

    @Test
    fun savingElsewhereMovesTheActiveFile() {
        val session = newSession()
        session.createNew(passphrase("correct horse"))
        session.mutate { BackupEditor.addLogin(it, 0, Login(title = "Email", password = "s3cr3t")) }

        val second = temporary.resolve("second.ksb")
        assertTrue(session.saveAs(second, passphrase("second pass")) is VaultResult.Ok)

        // Save-as leaves the original file exactly as it was, and the session now writes to the new
        // one - so a later edit must not reach the old path.
        val original = newSession("vault.ksb").unlock(passphrase("correct horse"))
        assertEquals("Email", (original as VaultResult.Ok).value.vaults.single().logins.single().title)
        session.mutate { BackupEditor.addLogin(it, 0, Login(title = "OnlyInCopy", password = "p")) }
        val originalAgain = newSession("vault.ksb").unlock(passphrase("correct horse"))
        assertEquals(
            1,
            (originalAgain as VaultResult.Ok).value.vaults.single().logins.size,
        )

        assertEquals(
            VaultError.NoVaultFile,
            VaultSession(VaultFiles(), SealedBackupFile(temporary.resolve("missing.ksb")))
                .unlock(passphrase("x")).errorOrNull(),
        )

        val reopened = VaultSession(VaultFiles(), SealedBackupFile(second)).unlock(passphrase("second pass"))
        assertEquals("OnlyInCopy", (reopened as VaultResult.Ok).value.vaults.single().logins.last().title)
    }

    @Test
    fun theLocalFileIsAnOrdinaryV1Backup() {
        // The vault file *is* a backup: importing it on the phone needs no conversion step, and the
        // envelope must therefore carry the version and source Android expects.
        val session = newSession()
        session.createNew(passphrase("correct horse"))
        session.mutate { BackupEditor.addCard(it, 0, Card(title = "Visa", number = "4111111111111111")) }

        val text = Files.readString(temporary.resolve("vault.ksb"))
        assertEquals(
            de.davis.keygo.desktop.core.backup.KeySource.PASSPHRASE,
            JsonBackupFormat.inspect(text),
        )
        val restored = JsonBackupFormat.import(text, BackupCredential.Passphrase("correct horse".toByteArray()))
        assertEquals("4111111111111111", restored.vaults.single().cards.single().number)
    }
}
