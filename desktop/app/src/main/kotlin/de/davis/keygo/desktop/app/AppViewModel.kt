/*
 * KeyStan Desktop - a Windows companion for the KeyGo Android password manager.
 * Copyright (C) 2026 Nursqode
 *
 * Part of KeyStan (https://github.com/Nursqode/KeyStan), a fork of KeyGo
 * (https://github.com/OffRange/KeyGo). Licensed under the GNU General Public License v3.0 or
 * later, like the project it belongs to; see LICENSE.
 */

package de.davis.keygo.desktop.app

import java.nio.file.Path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import de.davis.keygo.desktop.core.backup.model.Backup
import de.davis.keygo.desktop.core.backup.model.Card
import de.davis.keygo.desktop.core.backup.model.Login
import de.davis.keygo.desktop.core.backup.model.Vault
import de.davis.keygo.desktop.core.ui.i18n.Strings
import de.davis.keygo.desktop.core.ui.i18n.stringsFor
import de.davis.keygo.desktop.core.ui.i18n.textFor
import de.davis.keygo.desktop.core.vault.data.AppPaths
import de.davis.keygo.desktop.core.vault.data.AppSettings
import de.davis.keygo.desktop.core.vault.data.AppSettingsStore
import de.davis.keygo.desktop.core.vault.data.SealedBackupFile
import de.davis.keygo.desktop.core.vault.data.VaultFiles
import de.davis.keygo.desktop.core.vault.domain.BackupEditor
import de.davis.keygo.desktop.core.vault.domain.ExportScope
import de.davis.keygo.desktop.core.vault.domain.ImportMode
import de.davis.keygo.desktop.core.vault.domain.Item
import de.davis.keygo.desktop.core.vault.domain.ItemKind
import de.davis.keygo.desktop.core.vault.domain.ItemRef
import de.davis.keygo.desktop.core.vault.domain.VaultError
import de.davis.keygo.desktop.core.vault.domain.VaultResult
import de.davis.keygo.desktop.core.vault.domain.VaultSession
import de.davis.keygo.desktop.core.vault.domain.summaries

/**
 * Holds the app's state and performs every operation that touches the vault file.
 *
 * Sealing a vault costs a deliberate amount of work (Argon2id over 64 MiB), so anything that writes
 * runs on [Dispatchers.IO] and reports progress through [AppState.busy] rather than freezing the
 * window. The UI never sees a `BackupError` or an `IOException`: both are turned into state here.
 */
class AppViewModel(
    private val scope: CoroutineScope,
    private val paths: AppPaths = AppPaths.default(),
) {

    private val settingsStore = AppSettingsStore(paths)
    private val files = VaultFiles()

    private var settings: AppSettings = settingsStore.load()

    /** The catalog for [settings]' language, used for every message this class produces. */
    private var strings: Strings = stringsFor(settings.language)
    private var session: VaultSession =
        VaultSession(files, SealedBackupFile(paths.vaultFile(settings.vaultFileName)))

    private val _state = MutableStateFlow(
        AppState(
            hasVaultFile = session.vaultFileExists(),
            vaultFileName = settings.vaultFileName,
            settings = settings,
            darkTheme = settings.theme.isDark(),
            strings = strings,
            totpNowSeconds = System.currentTimeMillis() / 1000,
        ),
    )
    val state: StateFlow<AppState> = _state.asStateFlow()

    // region locking

    /** Creates a vault at the current path, or opens the one already there. */
    fun submitPassphrase(passphrase: CharArray) {
        if (_state.value.busy) return
        if (session.vaultFileExists()) unlock(passphrase) else create(passphrase)
    }

    private fun create(passphrase: CharArray) {
        _state.update { it.copy(busy = true, busyLabel = strings.busyCreating, lockError = null) }
        scope.launch {
            val result = withContext(Dispatchers.IO) { session.createNew(passphrase) }
            when (result) {
                is VaultResult.Ok -> _state.update {
                    it.copy(
                        locked = false,
                        busy = false,
                        busyLabel = null,
                        lockError = null,
                        vaults = result.value.summaries(),
                        selectedVaultIndex = 0,
                    ).withRows()
                }

                is VaultResult.Err -> _state.update {
                    it.copy(busy = false, busyLabel = null, lockError = result.error.message())
                }
            }
        }
    }

    private fun unlock(passphrase: CharArray) {
        _state.update { it.copy(busy = true, busyLabel = strings.busyUnlocking, lockError = null) }
        scope.launch {
            val result = withContext(Dispatchers.IO) { session.unlock(passphrase) }
            when (result) {
                is VaultResult.Ok -> _state.update {
                    it.copy(
                        locked = false,
                        busy = false,
                        busyLabel = null,
                        lockError = null,
                        vaults = result.value.summaries(),
                        selectedVaultIndex = 0,
                        selectedItem = null,
                    ).withRows()
                }

                is VaultResult.Err -> _state.update {
                    it.copy(busy = false, busyLabel = null, lockError = result.error.message())
                }
            }
        }
    }

    fun lock() {
        session.lock()
        _state.update {
            it.copy(
                locked = true,
                detail = null,
                selectedItem = null,
                rows = emptyList(),
                vaults = emptyList(),
                screen = Screen.VAULT,
                dialog = AppDialog.NONE,
                revealSecrets = false,
                lockError = null,
                status = strings.statusVaultLocked,
            )
        }
    }

    /** Points the app at a vault file somewhere else, e.g. one on a USB stick. */
    fun useVaultFile(path: Path) {
        session.lock()
        session = VaultSession(files, SealedBackupFile(path))
        settings = settings.copy(vaultFileName = path.fileName.toString())
        if (path.parent != null) settingsStore.save(settings)
        _state.update {
            it.copy(
                locked = true,
                hasVaultFile = session.vaultFileExists(),
                vaultFileName = path.fileName.toString(),
                settings = settings,
                vaults = emptyList(),
                rows = emptyList(),
                detail = null,
                selectedItem = null,
                lockError = null,
            )
        }
    }

    fun vaultFilePath(): Path = session.vaultFile.path

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        settings = transform(settings)
        strings = stringsFor(settings.language)
        settingsStore.save(settings)
        _state.update {
            it.copy(settings = settings, darkTheme = settings.theme.isDark(), strings = strings)
        }
    }

    // endregion

    // region navigation

    fun showScreen(screen: Screen) = _state.update { it.copy(screen = screen, status = null) }

    fun selectVault(index: Int) = _state.update {
        it.copy(selectedVaultIndex = index, selectedItem = null, detail = null, query = "").withRows()
    }

    fun selectItem(ref: ItemRef?) = _state.update {
        val backup = session.backup
        val detail = if (backup != null && ref != null) BackupEditor.itemAt(backup, ref) else null
        it.copy(selectedItem = ref, detail = detail)
    }

    fun setQuery(query: String) = _state.update { it.copy(query = query).withRows() }

    fun toggleRevealSecrets() = _state.update { it.copy(revealSecrets = !it.revealSecrets) }

    fun openDialog(dialog: AppDialog) = _state.update { it.copy(dialog = dialog) }

    fun closeDialog() = _state.update { it.copy(dialog = AppDialog.NONE, editing = null) }

    fun notify(message: String) = _state.update { it.copy(status = message) }

    fun clearStatus() = _state.update { it.copy(status = null) }

    /**
     * Recomputes the one-time codes.
     *
     * Called once a second while a code is on screen. The vault itself is untouched - only a
     * timestamp in the state changes, and the rows derive their codes from it.
     */
    fun tick() {
        if (_state.value.locked) return
        val now = System.currentTimeMillis() / 1000
        if (now != _state.value.totpNowSeconds) _state.update { it.copy(totpNowSeconds = now) }
    }

    // endregion

    // region editing

    /** Opens the item editor for a new item of [kind] in the current vault. */
    fun startNewItem(kind: ItemKind) = _state.update {
        it.copy(
            dialog = AppDialog.EDIT_ITEM,
            editing = ItemDraft(ref = null, vaultIndex = it.selectedVaultIndex, kind = kind),
        )
    }

    /** Opens the item editor on an existing item. */
    fun startEditItem(ref: ItemRef) {
        val backup = session.backup ?: return
        val item = BackupEditor.itemAt(backup, ref) ?: return
        val draft = when (item) {
            is Item.OfLogin -> ItemDraft(ref, ref.vaultIndex, ItemKind.LOGIN, login = item.login)
            is Item.OfCard -> ItemDraft(ref, ref.vaultIndex, ItemKind.CARD, card = item.card)
        }
        _state.update { it.copy(dialog = AppDialog.EDIT_ITEM, editing = draft) }
    }

    fun saveLogin(draft: ItemDraft, login: Login) = edit { backup ->
        if (draft.ref == null) BackupEditor.addLogin(backup, draft.vaultIndex, login)
        else BackupEditor.updateItem(backup, draft.ref) { Item.OfLogin(login) }
    }

    fun saveCard(draft: ItemDraft, card: Card) = edit { backup ->
        if (draft.ref == null) BackupEditor.addCard(backup, draft.vaultIndex, card)
        else BackupEditor.updateItem(backup, draft.ref) { Item.OfCard(card) }
    }

    fun deleteItem(ref: ItemRef) {
        val removed = edit { backup -> BackupEditor.removeItem(backup, ref) }
        if (removed) _state.update { it.copy(selectedItem = null, detail = null, status = strings.statusItemDeleted) }
    }

    fun togglePinned(ref: ItemRef) = edit { backup ->
        BackupEditor.updateItem(backup, ref) { item ->
            when (item) {
                is Item.OfLogin -> Item.OfLogin(item.login.copy(pinned = !item.login.pinned))
                is Item.OfCard -> Item.OfCard(item.card.copy(pinned = !item.card.pinned))
            }
        }
    }

    fun moveItem(ref: ItemRef, targetVaultIndex: Int) {
        edit { backup -> BackupEditor.moveItem(backup, ref, targetVaultIndex) }
        _state.update { it.copy(selectedItem = null, detail = null, status = strings.statusItemMoved) }
    }

    // endregion

    // region vaults

    fun createVault(name: String, icon: String) = edit { backup ->
        BackupEditor.addVault(backup, Vault(name = name, icon = icon))
    }

    fun renameVault(index: Int, name: String, icon: String) = edit { backup ->
        BackupEditor.updateVault(backup, index) { it.copy(name = name, icon = icon) }
    }

    fun deleteVault(index: Int) {
        if ((session.backup?.vaults?.size ?: 0) <= 1) {
            notify(strings.statusVaultMustRemain)
            return
        }
        edit { backup -> BackupEditor.removeVault(backup, index) }
        _state.update { it.copy(selectedVaultIndex = 0, selectedItem = null, detail = null) }
    }

    // endregion

    // region transfer and passphrase

    fun exportTo(path: Path, exportPassphrase: CharArray, scope: ExportScope) {
        runBusy(strings.busyExporting) {
            val result = session.exportTo(path, exportPassphrase, scope)
            when (result) {
                is VaultResult.Ok -> _state.update {
                    it.copy(status = strings.statusExported(path.fileName.toString()), screen = Screen.VAULT)
                }

                is VaultResult.Err -> _state.update { it.copy(status = result.error.message()) }
            }
        }
    }

    fun importFrom(path: Path, importPassphrase: CharArray, mode: ImportMode) {
        runBusy(strings.busyImporting) {
            val result = session.importFrom(path, importPassphrase, mode)
            when (result) {
                is VaultResult.Ok -> _state.update { current ->
                    current.copy(
                        status = strings.statusImported(result.value.logins, result.value.cards),
                        screen = Screen.VAULT,
                        selectedItem = null,
                        detail = null,
                    ).withRows()
                }

                is VaultResult.Err -> _state.update { it.copy(status = result.error.message()) }
            }
        }
    }

    fun changePassphrase(newPassphrase: CharArray) {
        runBusy(strings.busyResealing) {
            when (val result = session.changePassphrase(newPassphrase)) {
                is VaultResult.Ok -> _state.update { it.copy(status = strings.statusPassphraseChanged) }
                is VaultResult.Err -> _state.update { it.copy(status = result.error.message()) }
            }
        }
    }

    // endregion

    // region internals

    /**
     * Applies an edit and, if it changes anything, writes the vault.
     *
     * The editor functions return what they changed so the caller can tell a no-op (opening a dialog
     * and pressing save without typing) from a real edit - a no-op must not pay for an Argon2id
     * derivation.
     */
    private fun edit(transform: (Backup) -> Backup): Boolean {
        if (session.backup == null) return false
        val before = session.backup
        val updated = transform(before!!)
        if (updated == before) return false

        scope.launch {
            val result = withContext(Dispatchers.IO) { session.mutate { updated } }
            when (result) {
                is VaultResult.Ok -> _state.update {
                    it.copy(
                        vaults = result.value.summaries(),
                        detail = it.selectedItem?.let { ref -> BackupEditor.itemAt(result.value, ref) },
                        status = null,
                    ).withRows()
                }

                is VaultResult.Err -> _state.update { it.copy(status = result.error.message()) }
            }
        }
        return true
    }

    /** Runs a vault operation off the UI thread, with the busy overlay up. */
    private fun runBusy(label: String, block: () -> Unit) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, busyLabel = label) }
        scope.launch {
            withContext(Dispatchers.IO) { block() }
            _state.update { it.copy(busy = false, busyLabel = null) }
        }
    }

    /** Rebuilds the visible item list from the vault, the selected vault and the search query. */
    private fun AppState.withRows(): AppState {
        val backup = session.backup ?: return copy(rows = emptyList(), detail = null)
        val vault = backup.vaults.getOrNull(selectedVaultIndex) ?: return copy(rows = emptyList(), detail = null)

        val needle = query.trim().lowercase()
        val logins = vault.logins.mapIndexedNotNull { index, login ->
            val row = ItemRow(
                ref = ItemRef(selectedVaultIndex, ItemKind.LOGIN, index),
                kind = ItemKind.LOGIN,
                title = login.title,
                subtitle = ItemFormatting.loginSubtitle(login),
                pinned = login.pinned,
                tags = login.tags,
                totpSecret = login.totpSecret?.takeIf { it.isNotBlank() },
                websites = login.websites,
            )
            row.takeIf { matches(needle, login.title, login.username, login.notes, login.tags, login.websites) }
        }
        val cards = vault.cards.mapIndexedNotNull { index, card ->
            val row = ItemRow(
                ref = ItemRef(selectedVaultIndex, ItemKind.CARD, index),
                kind = ItemKind.CARD,
                title = card.title,
                subtitle = ItemFormatting.cardSubtitle(card),
                pinned = card.pinned,
                tags = card.tags,
                totpSecret = null,
                websites = emptyList(),
            )
            row.takeIf { matches(needle, card.title, card.cardholder, card.notes, card.tags, listOf(card.number)) }
        }

        val sorted = (logins + cards).sortedWith(
            compareByDescending<ItemRow> { it.pinned }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.title },
        )
        return copy(
            rows = sorted,
            detail = selectedItem?.let { ref -> BackupEditor.itemAt(backup, ref) },
        )
    }

    private fun matches(needle: String, vararg fields: Any?): Boolean {
        if (needle.isEmpty()) return true
        return fields.any { field ->
            when (field) {
                null -> false
                is String -> field.lowercase().contains(needle)
                is List<*> -> field.any { it is String && it.lowercase().contains(needle) }
                else -> false
            }
        }
    }

    private fun VaultError.message(): String = strings.textFor(this)

    private fun de.davis.keygo.desktop.core.vault.data.ThemeMode.isDark(): Boolean = when (this) {
        de.davis.keygo.desktop.core.vault.data.ThemeMode.DARK -> true
        de.davis.keygo.desktop.core.vault.data.ThemeMode.LIGHT -> false
        de.davis.keygo.desktop.core.vault.data.ThemeMode.SYSTEM -> SystemTheme.isDark()
    }

    // endregion
}

/** Whether the operating system is currently in its dark theme. */
object SystemTheme {
    fun isDark(): Boolean = runCatching {
        // The Windows registry is the only reliable source on the JVM; everywhere else, default to
        // dark, which is what the app is designed around.
        val process = ProcessBuilder(
            "reg", "query",
            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
            "/v", "AppsUseLightTheme",
        ).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        output.contains("0x0")
    }.getOrDefault(true)
}
