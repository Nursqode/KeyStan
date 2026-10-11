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
 * Binary Canonical Serialization, as far as this format needs it.
 *
 * The AEAD's additional authenticated data is `BackupAad { version: u32, source: KeySource }`
 * serialized by the Rust core with the `bcs` crate, and BCS is not uniform about integers:
 * `serialize_u32` writes four little-endian bytes, while an enum's variant index goes through
 * `output_variant_index` and is ULEB128. Getting either wrong does not fail loudly - it makes every
 * existing file open as "wrong passphrase" instead.
 */
internal object Bcs {

    /** `serialize_u32`: a fixed four-byte little-endian integer, not ULEB128. */
    fun u32(value: Int): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value ushr 8) and 0xFF).toByte(),
        ((value ushr 16) and 0xFF).toByte(),
        ((value ushr 24) and 0xFF).toByte(),
    )

    /** An enum's variant index, which BCS does write as ULEB128. */
    fun variantIndex(index: Int): ByteArray = uleb128(index.toLong())

    fun uleb128(value: Long): ByteArray {
        require(value >= 0) { "BCS encodes only unsigned integers" }
        var remaining = value
        val out = ArrayList<Byte>(4)
        do {
            var byte = (remaining and 0x7F).toInt()
            remaining = remaining ushr 7
            if (remaining != 0L) byte = byte or 0x80
            out.add(byte.toByte())
        } while (remaining != 0L)
        return out.toByteArray()
    }
}

/**
 * The authenticated data of a sealed backup: `BackupAad { version, source }` under BCS.
 *
 * [KeySource] must not be reordered - BCS records the variant *index*, so `PASSPHRASE` is 0 and
 * `ARK` is 1, and swapping them would invalidate every backup ever written.
 */
internal fun backupAad(version: Int, source: KeySource): ByteArray =
    Bcs.u32(version) + Bcs.variantIndex(source.ordinal)
