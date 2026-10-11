/*
 * KeyStan Desktop - a Windows companion for the KeyGo Android password manager.
 * Copyright (C) 2026 Nursqode
 *
 * Part of KeyStan (https://github.com/Nursqode/KeyStan), a fork of KeyGo
 * (https://github.com/OffRange/KeyGo). Licensed under the GNU General Public License v3.0 or
 * later, like the project it belongs to; see LICENSE.
 */

package de.davis.keygo.desktop.core.backup

/**
 * Failures a backup operation can report.
 *
 * Mirrors `keygo_core::backup::BackupError` so that a desktop error can be mapped onto the same
 * user-facing message the Android app shows for the same file.
 */
sealed class BackupError(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** Key derivation or AEAD failure. [kind] separates "wrong passphrase" from "bad parameters". */
    class Crypto(val kind: CryptoKind, message: String, cause: Throwable? = null) :
        BackupError(message, cause)

    /** The envelope or its payload is not valid JSON. */
    class Json(message: String, cause: Throwable? = null) : BackupError(message, cause)

    /** The payload or a header field is not valid base64. */
    data object Base64 : BackupError("invalid base64 in backup payload or header")

    /** The envelope declares a version this build does not know how to read. */
    class UnsupportedVersion(val version: Int) :
        BackupError("unsupported backup version: $version")

    /** The header is internally inconsistent (e.g. an ARK source with a passphrase KDF). */
    data object MalformedHeader : BackupError("malformed encryption header")

    /** The supplied credential does not match the key source the file declares. */
    data object CredentialMismatch :
        BackupError("credential does not match the backup's key source")

    enum class CryptoKind {
        /** Argon2id/HKDF rejected the inputs (empty passphrase, short salt, hostile cost params). */
        KDF,

        /** The AEAD tag did not verify: wrong key, or the file was modified. */
        DECRYPTION,
        ENCRYPTION,
        INVALID_KEY_LENGTH,
    }
}
