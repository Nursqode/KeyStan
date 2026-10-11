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
import java.nio.file.StandardCopyOption
import de.davis.keygo.desktop.core.backup.KeySource

/**
 * A backup file on disk, read and replaced as a whole.
 *
 * Writes go through a sibling temporary file that is flushed and then atomically moved over the
 * target. A vault is rewritten on every edit, so a crash or a full disk during that rewrite must
 * leave the previous file intact - truncating the real file first would lose the vault.
 */
class SealedBackupFile(val path: Path) {

    fun exists(): Boolean = Files.isRegularFile(path)

    fun read(): String = Files.readString(path)

    fun write(text: String) {
        path.parent?.let(Files::createDirectories)
        val temporary = path.resolveSibling("${path.fileName}.tmp")
        try {
            Files.newByteChannel(
                temporary,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.WRITE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
            ).use { channel ->
                val bytes = java.nio.ByteBuffer.wrap(text.toByteArray(Charsets.UTF_8))
                while (bytes.hasRemaining()) channel.write(bytes)
                // Force the bytes to the device before the rename, otherwise the rename can be
                // durable while the content it points at is not.
                (channel as? java.nio.channels.FileChannel)?.force(true)
            }
            Files.move(
                temporary,
                path,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (e: java.nio.file.AtomicMoveNotSupportedException) {
            // Some network and FAT filesystems cannot move atomically; a plain replace still
            // leaves either the old or the new file, never a partial one.
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    /** What the file needs to be opened, or null when it cannot be read as a backup at all. */
    fun inspect(): KeySource? = runCatching {
        de.davis.keygo.desktop.core.backup.JsonBackupFormat.inspect(read())
    }.getOrNull()
}
