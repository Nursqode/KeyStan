/*
 * KeyStan Desktop - a Windows companion for the KeyGo Android password manager.
 * Copyright (C) 2026 Nursqode
 *
 * Part of KeyStan (https://github.com/Nursqode/KeyStan), a fork of KeyGo
 * (https://github.com/OffRange/KeyGo). Licensed under the GNU General Public License v3.0 or
 * later, like the project it belongs to; see LICENSE.
 */

package de.davis.keygo.desktop.core.vault.domain

/**
 * The failures a vault operation can report to the UI.
 *
 * These are modelled as values rather than exceptions because every one of them is an expected
 * outcome of ordinary use - a mistyped passphrase, a file the user picked that is not a backup -
 * and the screen has to say something specific about each.
 */
sealed interface VaultError {

    /** No vault file exists yet at the configured path. */
    data object NoVaultFile : VaultError

    /** A vault file exists and the passphrase does not open it. */
    data object WrongPassphrase : VaultError

    /** The passphrase does not meet the app's own minimum (see [PassphrasePolicy]). */
    data object WeakPassphrase : VaultError

    /**
     * The file is sealed with the Android account's ARK rather than a passphrase.
     *
     * The desktop never holds an ARK - it belongs to the phone's account - so such a file can be
     * recognised and explained, but not opened.
     */
    data object NeedsAccountKey : VaultError

    /** The file is not a backup, or is a version or shape this build cannot read. */
    data class Unreadable(val reason: String) : VaultError

    /** The filesystem refused to read or write. */
    data class Io(val reason: String) : VaultError

    /** The two passphrases typed in a confirmation screen differ. */
    data object PassphraseMismatch : VaultError
}

/** A result that carries a [VaultError] instead of throwing for expected failures. */
sealed interface VaultResult<out T> {
    data class Ok<T>(val value: T) : VaultResult<T>
    data class Err(val error: VaultError) : VaultResult<Nothing>
}

inline fun <T> VaultResult<T>.onOk(action: (T) -> Unit): VaultResult<T> {
    if (this is VaultResult.Ok) action(value)
    return this
}

inline fun <T> VaultResult<T>.onErr(action: (VaultError) -> Unit): VaultResult<T> {
    if (this is VaultResult.Err) action(error)
    return this
}

fun <T> VaultResult<T>.valueOrNull(): T? = (this as? VaultResult.Ok)?.value

fun VaultResult<*>.errorOrNull(): VaultError? = (this as? VaultResult.Err)?.error

/** Minimum a new vault passphrase has to meet. */
object PassphrasePolicy {
    const val MIN_LENGTH = 8

    fun isAcceptable(passphrase: CharArray): Boolean = passphrase.size >= MIN_LENGTH
}
