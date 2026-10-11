/*
 * KeyStan Desktop - a Windows companion for the KeyGo Android password manager.
 * Copyright (C) 2026 Nursqode
 *
 * Part of KeyStan (https://github.com/Nursqode/KeyStan), a fork of KeyGo
 * (https://github.com/OffRange/KeyGo). Licensed under the GNU General Public License v3.0 or
 * later, like the project it belongs to; see LICENSE.
 */

package de.davis.keygo.desktop.core.backup

import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.modes.GCMSIVBlockCipher
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.Argon2Parameters
import org.bouncycastle.crypto.params.KeyParameter

/**
 * Argon2id cost profile, mirroring `keygo_core::crypto::primitive::argon2::Argon2Params`.
 *
 * The defaults are the ones the Android app seals with; they are recorded in the file's header, so
 * a file states its own cost and this default only decides what new exports cost.
 */
data class Argon2Params(
    /** Memory cost in KiB. */
    val memKib: Int,
    /** Iterations / time cost. */
    val iters: Int,
    /** Degree of parallelism. */
    val lanes: Int,
) {
    companion object {
        val Default = Argon2Params(memKib = 64 * 1024, iters = 3, lanes = 4)
    }
}

/**
 * The cryptographic half of the backup format: derivations and the AEAD, byte-for-byte compatible
 * with the Rust core.
 *
 * Every operation here is authenticated against [backupAad], and every input is validated the way
 * the Rust side validates it, so a malformed header fails as a clean error instead of as an
 * allocation attempt or a panic.
 */
internal object BackupCrypto {

    const val DERIVED_KEY_LEN = 32
    const val MIN_SALT_LEN = 16
    const val NONCE_LEN = 12
    const val TAG_LEN = 16

    /** Matches `MAX_ARGON2_MEM_KIB`: a hostile header must not be able to demand all RAM. */
    const val MAX_ARGON2_MEM_KIB = 256 * 1024

    /**
     * Domain labels mixed into the derivation. They separate the passphrase path from the ARK
     * path so that the same 32 bytes cannot produce the same backup key through both.
     */
    private const val DOMAIN_PASSPHRASE = "v1:backup/passphrase"
    private const val DOMAIN_ARK = "v1:backup/ark"

    private const val TAG_BITS = 128
    private const val MAC_ALGORITHM = "HmacSHA256"

    private val random = SecureRandom()

    fun randomBytes(size: Int): ByteArray = ByteArray(size).also(random::nextBytes)

    /** Argon2id over `(passphrase, salt || domain)`, exactly as `BackupKey::from_passphrase`. */
    fun derivePassphraseKey(
        passphrase: ByteArray,
        salt: ByteArray,
        params: Argon2Params = Argon2Params.Default,
    ): ByteArray {
        if (passphrase.isEmpty())
            throw kdfError("empty password")
        if (salt.size < MIN_SALT_LEN)
            throw kdfError("salt too short: ${salt.size} < $MIN_SALT_LEN")
        if (params.memKib > MAX_ARGON2_MEM_KIB)
            throw kdfError("argon2 mem_kib too large: ${params.memKib} > $MAX_ARGON2_MEM_KIB")

        // The domain label is appended to the caller's salt rather than used as an Argon2 "secret"
        // or "associated data": that is what the Rust `derive_argon2id_with_params` does, and the
        // bytes fed to Argon2 have to agree exactly for a file to open on both sides.
        val fullSalt = salt + DOMAIN_PASSPHRASE.encodeToByteArray()

        val argonParams = try {
            Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(params.iters)
                .withMemoryAsKB(params.memKib)
                .withParallelism(params.lanes)
                .withSalt(fullSalt)
                .build()
        } catch (e: IllegalArgumentException) {
            throw kdfError(e.message ?: "invalid argon2 parameters", e)
        }

        val generator = Argon2BytesGenerator()
        generator.init(argonParams)
        val derived = ByteArray(DERIVED_KEY_LEN)
        try {
            generator.generateBytes(passphrase, derived)
        } catch (e: IllegalArgumentException) {
            throw kdfError(e.message ?: "argon2 derivation failed", e)
        }
        return derived
    }

    /** HKDF-SHA256 over the ARK, exactly as `BackupKey::from_ark`. */
    fun deriveArkKey(ark: ByteArray, salt: ByteArray): ByteArray {
        if (ark.size != DERIVED_KEY_LEN)
            throw BackupError.Crypto(
                BackupError.CryptoKind.INVALID_KEY_LENGTH,
                "ark must be $DERIVED_KEY_LEN bytes, got ${ark.size}",
            )
        return hkdfSha256(
            ikm = ark,
            salt = salt,
            info = DOMAIN_ARK.encodeToByteArray(),
            length = DERIVED_KEY_LEN,
        )
    }

    /** AES-256-GCM-SIV (RFC 8452); the 16-byte tag is appended to the ciphertext. */
    fun seal(key: ByteArray, nonce: ByteArray, plaintext: ByteArray, aad: ByteArray): ByteArray {
        requireKeyAndNonce(key, nonce)
        val cipher = GCMSIVBlockCipher(AESEngine.newInstance())
        cipher.init(true, AEADParameters(KeyParameter(key), TAG_BITS, nonce, aad))
        val out = ByteArray(cipher.getOutputSize(plaintext.size))
        var written = cipher.processBytes(plaintext, 0, plaintext.size, out, 0)
        written += cipher.doFinal(out, written)
        return out.copyOf(written)
    }

    /**
     * Opens a sealed payload.
     *
     * GCM-SIV is misused-resistant but not forgiving: a wrong key, a modified ciphertext or a
     * mismatched AAD all surface here as [BackupError.CryptoKind.DECRYPTION] and nothing else.
     */
    fun open(key: ByteArray, nonce: ByteArray, ciphertext: ByteArray, aad: ByteArray): ByteArray {
        requireKeyAndNonce(key, nonce)
        if (ciphertext.size < TAG_LEN)
            throw BackupError.Crypto(BackupError.CryptoKind.DECRYPTION, "ciphertext shorter than its tag")

        val cipher = GCMSIVBlockCipher(AESEngine.newInstance())
        cipher.init(false, AEADParameters(KeyParameter(key), TAG_BITS, nonce, aad))
        val out = ByteArray(cipher.getOutputSize(ciphertext.size))
        return try {
            var written = cipher.processBytes(ciphertext, 0, ciphertext.size, out, 0)
            written += cipher.doFinal(out, written)
            out.copyOf(written)
        } catch (e: Exception) {
            throw BackupError.Crypto(
                BackupError.CryptoKind.DECRYPTION,
                "authentication failed: the file is corrupt or the credential is wrong",
                e,
            )
        }
    }

    private fun requireKeyAndNonce(key: ByteArray, nonce: ByteArray) {
        if (key.size != DERIVED_KEY_LEN)
            throw BackupError.Crypto(
                BackupError.CryptoKind.INVALID_KEY_LENGTH,
                "backup key must be $DERIVED_KEY_LEN bytes, got ${key.size}",
            )
        if (nonce.size != NONCE_LEN)
            throw BackupError.MalformedHeader
    }

    /**
     * RFC 5869 HKDF-SHA256, implemented over the JDK's HMAC rather than a crypto library's own
     * helper: it is extract-then-expand over a single block here, and the ARK golden vector pins
     * the result against the Rust `hkdf` crate.
     */
    internal fun hkdfSha256(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
        if (length <= 0 || length > 255 * DERIVED_KEY_LEN)
            throw kdfError("invalid hkdf output length: $length")

        val mac = try {
            Mac.getInstance(MAC_ALGORITHM)
        } catch (e: java.security.NoSuchAlgorithmException) {
            throw kdfError("$MAC_ALGORITHM is unavailable", e)
        }

        // Extract. HKDF treats an absent salt as HashLen zero bytes, and HMAC pads a short key with
        // zeros to the block size either way - so an empty salt is passed as a zero key, which the
        // JDK accepts and which keys the HMAC identically.
        val extractKey = if (salt.isEmpty()) ByteArray(DERIVED_KEY_LEN) else salt
        mac.init(SecretKeySpec(extractKey, MAC_ALGORITHM))
        val prk = mac.doFinal(ikm)

        // Expand. One byte of output per counter value, chained through the previous block.
        mac.init(SecretKeySpec(prk, MAC_ALGORITHM))
        val okm = ByteArray(length)
        var generated = 0
        var counter = 1
        var block = ByteArray(0)
        while (generated < length) {
            mac.update(block)
            mac.update(info)
            mac.update(counter.toByte())
            block = mac.doFinal()
            val take = minOf(block.size, length - generated)
            block.copyInto(okm, generated, 0, take)
            generated += take
            counter++
        }
        return okm
    }

    private fun kdfError(message: String, cause: Throwable? = null) =
        BackupError.Crypto(BackupError.CryptoKind.KDF, message, cause)
}
