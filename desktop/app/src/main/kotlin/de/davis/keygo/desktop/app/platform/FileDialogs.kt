/*
 * KeyStan Desktop - a Windows companion for the KeyGo Android password manager.
 * Copyright (C) 2026 Nursqode
 *
 * Part of KeyStan (https://github.com/Nursqode/KeyStan), a fork of KeyGo
 * (https://github.com/OffRange/KeyGo). Licensed under the GNU General Public License v3.0 or
 * later, like the project it belongs to; see LICENSE.
 */

package de.davis.keygo.desktop.app.platform

import java.awt.FileDialog
import java.awt.Frame
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Native file pickers, via AWT.
 *
 * Compose Multiplatform has no file dialog of its own, and AWT's [FileDialog] is the native one on
 * Windows and macOS - the same dialog every other desktop app opens - which matters more here than
 * a uniform look, because the user is choosing where their vault lives.
 */
object FileDialogs {

    /** Asks for an existing file; returns null when the user cancels. */
    fun open(title: String, directory: Path?, extensions: List<String> = emptyList()): Path? =
        choose(title, FileDialog.LOAD, directory, suggestedName = null, extensions)

    /** Asks where to write a new file; returns null when the user cancels. */
    fun save(title: String, suggestedName: String, directory: Path?, extensions: List<String> = emptyList()): Path? =
        choose(title, FileDialog.SAVE, directory, suggestedName, extensions)

    private fun choose(
        title: String,
        mode: Int,
        directory: Path?,
        suggestedName: String?,
        extensions: List<String>,
    ): Path? = runCatching {
        val dialog = FileDialog(null as Frame?, title, mode)
        directory?.let { dialog.directory = it.toString() }
        suggestedName?.let { dialog.file = it }
        if (extensions.isNotEmpty()) dialog.setFilenameFilter { _, name -> extensions.any(name::endsWith) }
        dialog.isVisible = true

        val selected = dialog.file ?: return@runCatching null
        val folder = dialog.directory ?: return@runCatching null
        Paths.get(folder, selected)
    }.getOrNull()
}
