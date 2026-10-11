/*
 * KeyStan Desktop - a Windows companion for the KeyGo Android password manager.
 * Copyright (C) 2026 Nursqode
 *
 * Part of KeyStan (https://github.com/Nursqode/KeyStan), a fork of KeyGo
 * (https://github.com/OffRange/KeyGo). Licensed under the GNU General Public License v3.0 or
 * later, like the project it belongs to; see LICENSE.
 */

package de.davis.keygo.desktop.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import java.awt.datatransfer.StringSelection
import kotlinx.coroutines.launch

/**
 * A copy-to-clipboard action for the composables that need one.
 *
 * Compose's clipboard is suspend-based now, so the coroutine scope is set up once here instead of in
 * every screen that copies a username or a password. The AWT transferable is the desktop clipboard's
 * native form.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun rememberClipboardCopier(): (String) -> Unit {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    return remember(clipboard, scope) {
        { text ->
            if (text.isNotEmpty()) {
                scope.launch { clipboard.setClipEntry(ClipEntry(StringSelection(text))) }
            }
        }
    }
}
