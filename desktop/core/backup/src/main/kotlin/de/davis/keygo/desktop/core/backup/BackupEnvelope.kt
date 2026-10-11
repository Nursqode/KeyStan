/*
 * KeyStan Desktop - a Windows companion for the KeyGo Android password manager.
 * Copyright (C) 2026 Nursqode
 *
 * Part of KeyStan (https://github.com/Nursqode/KeyStan), a fork of KeyGo
 * (https://github.com/OffRange/KeyGo). Licensed under the GNU General Public License v3.0 or
 * later, like the project it belongs to; see LICENSE.
 */

package de.davis.keygo.desktop.core.backup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Which credential sealed a backup, matching `KeySource` in the Rust core.
 *
 * Declaration order is load-bearing: the enum's BCS serialization - which is what authenticates the
 * header as additional data - is the variant **index**, so reordering these silently makes every
 * existing file undecryptable.
 */
@Serializable
enum class KeySource {
    @SerialName("passphrase")
    PASSPHRASE,

    @SerialName("ark")
    ARK,
    ;

    companion object {
        fun fromWire(value: String): KeySource? = when (value) {
            "passphrase" -> PASSPHRASE
            "ark" -> ARK
            else -> null
        }
    }
}

/**
 * The envelope's key-derivation description.
 *
 * This is one DTO rather than a sealed hierarchy because the wire shape is a JSON object tagged by
 * `type`, and the tag decides which of the optional cost parameters are meaningful. Fields not
 * belonging to the tag are left null and are omitted on write, exactly as serde omits them.
 */
@Serializable
data class Kdf(
    val type: String,
    @Serializable(with = Base64BytesSerializer::class) val salt: ByteArray,
    @SerialName("mem_kib") val memKib: Int? = null,
    val iters: Int? = null,
    val lanes: Int? = null,
) {
    companion object {
        const val TYPE_ARGON2ID = "argon2id"
        const val TYPE_HKDF_SHA256 = "hkdf_sha256"

        fun argon2id(salt: ByteArray, params: Argon2Params): Kdf = Kdf(
            type = TYPE_ARGON2ID,
            salt = salt,
            memKib = params.memKib,
            iters = params.iters,
            lanes = params.lanes,
        )

        fun hkdfSha256(salt: ByteArray): Kdf = Kdf(type = TYPE_HKDF_SHA256, salt = salt)
    }

    /** The recorded Argon2id cost profile, or null when this KDF is not Argon2id. */
    fun argon2Params(): Argon2Params? {
        if (type != TYPE_ARGON2ID) return null
        val mem = memKib ?: return null
        val iterations = iters ?: return null
        val parallelism = lanes ?: return null
        return Argon2Params(mem, iterations, parallelism)
    }
}

/** AEAD parameters recorded alongside a sealed payload. */
@Serializable
data class EncryptionHeader(
    val source: KeySource,
    val kdf: Kdf,
    @Serializable(with = Base64BytesSerializer::class) val nonce: ByteArray,
) {
    override fun equals(other: Any?): Boolean = other is EncryptionHeader &&
        source == other.source &&
        kdf == other.kdf &&
        nonce.contentEquals(other.nonce)

    override fun hashCode(): Int =
        (source.hashCode() * 31 + kdf.hashCode()) * 31 + nonce.contentHashCode()
}

/**
 * A sealed JSON backup.
 *
 * There is no plaintext envelope: `payload` is always ciphertext, whose nonce sits in
 * [encryption] and whose key is derived from either a passphrase or the account root key.
 */
@Serializable
data class BackupEnvelope(
    val version: Int,
    val encryption: EncryptionHeader,
    val payload: String,
)
