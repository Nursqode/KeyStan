/*
 * KeyStan Desktop - a Windows companion for the KeyGo Android password manager.
 * Copyright (C) 2026 Nursqode
 *
 * Part of KeyStan (https://github.com/Nursqode/KeyStan), a fork of KeyGo
 * (https://github.com/OffRange/KeyGo). Licensed under the GNU General Public License v3.0 or
 * later, like the project it belongs to; see LICENSE.
 */

package de.davis.keygo.desktop.app

/**
 * The desktop build's version, shown in the About card.
 *
 * Kept in step with `packageVersion` in `app/build.gradle.kts` by hand: the build file's value is
 * what the packaged executable reports to Windows, and this is what the user reads in the app.
 */
const val DESKTOP_VERSION = "1.1.0"
