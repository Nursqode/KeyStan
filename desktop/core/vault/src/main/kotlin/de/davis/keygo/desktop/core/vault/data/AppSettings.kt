/*
 * KeyStan Desktop - a Windows companion for the KeyGo Android password manager.
 * Copyright (C) 2026 Nursqode
 *
 * Part of KeyStan (https://github.com/Nursqode/KeyStan), a fork of KeyGo
 * (https://github.com/OffRange/KeyGo). Licensed under the GNU General Public License v3.0 or
 * later, like the project it belongs to; see LICENSE.
 */

package de.davis.keygo.desktop.core.vault.data

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Which colour scheme the window uses. */
@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Everything the app remembers between runs, other than the vault itself.
 *
 * Only preferences live here - never a passphrase, and never anything derived from one. The vault
 * file is the only place secrets exist, and it is sealed on its own.
 */
@Serializable
data class AppSettings(
    val vaultFileName: String = "vault.ksb",
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** Minutes of inactivity before the vault locks; 0 disables automatic locking. */
    val autoLockMinutes: Int = 5,
    val lastImportDirectory: String? = null,
    val lastExportDirectory: String? = null,
    /** Whether list screens hide secrets until the user asks for them. */
    val concealSecrets: Boolean = true,
)

/** Where the app keeps its files. */
data class AppPaths(val dataDirectory: Path) {
    val settingsFile: Path get() = dataDirectory.resolve("settings.json")

    fun vaultFile(name: String): Path = dataDirectory.resolve(name)

    companion object {
        /**
         * The per-user application directory for the current platform.
         *
         * A password manager's files belong in the roaming application data directory rather than
         * next to the executable, which may be read-only (Program Files) or shared.
         *
         * `KEYSTAN_DATA_DIR` (or the `keystan.dataDir` system property) overrides it, which is what
         * makes a portable install on a USB stick - and a smoke test that must not touch the real
         * vault - possible.
         */
        fun default(): AppPaths {
            System.getenv("KEYSTAN_DATA_DIR")?.takeIf { it.isNotBlank() }?.let { return AppPaths(Paths.get(it)) }
            System.getProperty("keystan.dataDir")?.takeIf { it.isNotBlank() }?.let { return AppPaths(Paths.get(it)) }

            val os = System.getProperty("os.name").orEmpty().lowercase()
            val home = Paths.get(System.getProperty("user.home"))
            val directory = when {
                os.contains("win") -> System.getenv("APPDATA")?.let(Paths::get) ?: home.resolve("AppData/Roaming")
                os.contains("mac") -> home.resolve("Library/Application Support")
                else -> System.getenv("XDG_CONFIG_HOME")?.let(Paths::get) ?: home.resolve(".config")
            }
            return AppPaths(directory.resolve("KeyStan"))
        }
    }
}

/**
 * Reads and writes [AppSettings].
 *
 * A settings file that cannot be parsed is treated as absent rather than fatal: preferences are
 * convenience, and refusing to start over a corrupt one would strand the user's vault behind it.
 */
class AppSettingsStore(private val paths: AppPaths) {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun load(): AppSettings {
        val file = paths.settingsFile
        if (!Files.isRegularFile(file)) return AppSettings()
        return runCatching { json.decodeFromString(AppSettings.serializer(), Files.readString(file)) }
            .getOrElse { AppSettings() }
    }

    fun save(settings: AppSettings) {
        paths.dataDirectory.let(Files::createDirectories)
        SealedBackupFile(paths.settingsFile)
            .write(json.encodeToString(AppSettings.serializer(), settings))
    }
}
