/*
 * KeyStan Desktop - a Windows companion for the KeyGo Android password manager.
 * Copyright (C) 2026 Nursqode
 *
 * Part of KeyStan (https://github.com/Nursqode/KeyStan), a fork of KeyGo
 * (https://github.com/OffRange/KeyGo). Licensed under the GNU General Public License v3.0 or
 * later, like the project it belongs to; see LICENSE.
 */

package de.davis.keygo.desktop.core.backup

import java.nio.CharBuffer
import java.util.Base64
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import de.davis.keygo.desktop.core.backup.model.Backup

/** Version stamped into newly written backups. Mirrors `keygo_core::backup::CURRENT_VERSION`. */
const val CURRENT_VERSION: Int = 1

/** Oldest envelope version this build can still read. */
const val MIN_SUPPORTED_VERSION: Int = 1

/**
 * What seals, or opens, a backup.
 *
 * [Passphrase] carries raw bytes (never a `String`, which cannot be wiped) so the caller decides the
 * encoding and remains the owner of the buffer.
 */
sealed interface BackupCredential {
    class Passphrase(val bytes: ByteArray) : BackupCredential
    class Ark(val key: ByteArray) : BackupCredential
}

/**
 * UTF-8 bytes of a passphrase.
 *
 * The Android app hands the Rust side UTF-8 (`encodeToByteArray`), so the desktop must too:
 * Argon2 consumes bytes, and any other encoding would silently produce a different key for the
 * same typed passphrase.
 */
fun CharArray.toUtf8Bytes(): ByteArray {
    val buffer = Charsets.UTF_8.encode(CharBuffer.wrap(this))
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return bytes
}

/** A sealed payload: the header to publish plus the authenticated ciphertext. */
internal class SealedPayload(val header: EncryptionHeader, val ciphertext: ByteArray)

/**
 * Reads and writes the KeyGo JSON backup format (envelope version 1).
 *
 * This is the desktop counterpart of the Rust `JsonBackupManager`, and the only thing that has to
 * agree with Android for a file to travel between the two: a file written here is a file the app
 * restores, and the other way round.
 */
object JsonBackupFormat {

    /**
     * The payload as serde writes it: every field present, `null` for absent optionals, compact
     * separators. `encodeDefaults` is on so that a defaulted field is still spelled out - Android's
     * serde fills in the same values for missing keys, but writing them keeps a desktop export
     * readable by any older build that predates a field.
     */
    private val payloadJson = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /**
     * The envelope as serde writes it: `encodeDefaults` is off so that a KDF omits the cost fields
     * that do not belong to its type, e.g. an ARK header carries only its salt.
     */
    private val envelopeJson = Json {
        encodeDefaults = false
        ignoreUnknownKeys = true
    }

    /** Seals [backup] with [credential] and returns the complete envelope as JSON text. */
    fun export(backup: Backup, credential: BackupCredential): String {
        val payload = payloadJson.encodeToString(Backup.serializer(), backup).encodeToByteArray()
        val sealed = seal(payload, credential, CURRENT_VERSION)
        val envelope = BackupEnvelope(
            version = CURRENT_VERSION,
            encryption = sealed.header,
            payload = Base64BytesSerializer.encode(sealed.ciphertext),
        )
        return envelopeJson.encodeToString(BackupEnvelope.serializer(), envelope)
    }

    /**
     * Opens [data] with [credential].
     *
     * Fields that older writers used (a single `website`/`passkey` instead of the current lists) are
     * promoted before decoding; see [promoteLegacyFields]. What comes back is always the current
     * model, so a re-export writes the current format.
     *
     * @throws BackupError when the file is not a backup, is a version this build cannot read, or
     *   the credential does not open it.
     */
    fun import(data: String, credential: BackupCredential): Backup {
        val envelope = parseEnvelope(data)
        val ciphertext = decodePayload(envelope.payload)
        val plaintext = open(envelope.encryption, ciphertext, credential, envelope.version)
        return try {
            val payload = Json.parseToJsonElement(plaintext.decodeToString())
            payloadJson.decodeFromJsonElement(Backup.serializer(), promoteLegacyFields(payload))
        } catch (e: SerializationException) {
            throw BackupError.Json(e.message ?: "malformed backup payload", e)
        } catch (e: IllegalArgumentException) {
            throw BackupError.Json(e.message ?: "malformed backup payload", e)
        }
    }

    /**
     * Reports which credential a file needs, without decrypting it.
     *
     * The import flow needs this before it can ask for anything: an ARK-sealed file cannot be
     * opened with a passphrase, and asking for one would be a dead end.
     */
    fun inspect(data: String): KeySource = parseEnvelope(data).encryption.source

    /**
     * Seals [plaintext] under [credential].
     *
     * [salt] and [nonce] default to fresh random values; they are parameters only so that the
     * compatibility tests can reproduce a frozen Rust vector byte-for-byte. Never pass a fixed
     * value outside a test.
     */
    internal fun seal(
        plaintext: ByteArray,
        credential: BackupCredential,
        version: Int,
        salt: ByteArray = BackupCrypto.randomBytes(BackupCrypto.MIN_SALT_LEN),
        nonce: ByteArray = BackupCrypto.randomBytes(BackupCrypto.NONCE_LEN),
    ): SealedPayload {
        val key: ByteArray
        val source: KeySource
        val kdf: Kdf
        when (credential) {
            is BackupCredential.Passphrase -> {
                val params = Argon2Params.Default
                key = BackupCrypto.derivePassphraseKey(credential.bytes, salt, params)
                source = KeySource.PASSPHRASE
                kdf = Kdf.argon2id(salt, params)
            }

            is BackupCredential.Ark -> {
                key = BackupCrypto.deriveArkKey(credential.key, salt)
                source = KeySource.ARK
                kdf = Kdf.hkdfSha256(salt)
            }
        }

        val ciphertext = try {
            BackupCrypto.seal(key, nonce, plaintext, backupAad(version, source))
        } finally {
            // The derived key is only needed for this one call; the credential's own bytes belong
            // to the caller, which is why only this copy is wiped.
            key.fill(0)
        }
        return SealedPayload(EncryptionHeader(source, kdf, nonce), ciphertext)
    }

    /**
     * Opens a payload, checking that the header's key source and KDF agree with [credential] before
     * deriving anything.
     *
     * The checks mirror `backup::encryption::open`: a passphrase cannot open an ARK file, and a
     * header whose KDF does not match its declared source is malformed rather than merely wrong.
     */
    internal fun open(
        header: EncryptionHeader,
        ciphertext: ByteArray,
        credential: BackupCredential,
        version: Int,
    ): ByteArray {
        if (header.nonce.size != BackupCrypto.NONCE_LEN)
            throw BackupError.MalformedHeader

        val key = when {
            header.source == KeySource.PASSPHRASE && credential is BackupCredential.Ark ->
                throw BackupError.CredentialMismatch

            header.source == KeySource.ARK && credential is BackupCredential.Passphrase ->
                throw BackupError.CredentialMismatch

            header.source == KeySource.PASSPHRASE && credential is BackupCredential.Passphrase -> {
                if (header.kdf.type != Kdf.TYPE_ARGON2ID)
                    throw BackupError.MalformedHeader
                val params = header.kdf.argon2Params() ?: throw BackupError.MalformedHeader
                BackupCrypto.derivePassphraseKey(credential.bytes, header.kdf.salt, params)
            }

            header.source == KeySource.ARK && credential is BackupCredential.Ark -> {
                if (header.kdf.type != Kdf.TYPE_HKDF_SHA256)
                    throw BackupError.MalformedHeader
                BackupCrypto.deriveArkKey(credential.key, header.kdf.salt)
            }

            else -> throw BackupError.MalformedHeader
        }

        return try {
            BackupCrypto.open(key, header.nonce, ciphertext, backupAad(version, header.source))
        } finally {
            key.fill(0)
        }
    }

    private fun parseEnvelope(data: String): BackupEnvelope {
        val envelope = try {
            envelopeJson.decodeFromString(BackupEnvelope.serializer(), data)
        } catch (e: BackupError.Base64) {
            // serde reports a bad header salt as a JSON error because the base64 adapter runs
            // inside it; keep the same error shape so both sides report a corrupt header alike.
            throw BackupError.Json("invalid base64 in the encryption header", e)
        } catch (e: SerializationException) {
            throw BackupError.Json(e.message ?: "malformed backup envelope", e)
        } catch (e: IllegalArgumentException) {
            throw BackupError.Json(e.message ?: "malformed backup envelope", e)
        }

        if (envelope.version !in MIN_SUPPORTED_VERSION..CURRENT_VERSION)
            throw BackupError.UnsupportedVersion(envelope.version)
        return envelope
    }

    private fun decodePayload(payload: String): ByteArray = try {
        Base64.getDecoder().decode(payload)
    } catch (e: IllegalArgumentException) {
        throw BackupError.Base64
    }
}
