/*
 * KeyStan Desktop - a Windows companion for the KeyGo Android password manager.
 * Copyright (C) 2026 Nursqode
 *
 * Part of KeyStan (https://github.com/Nursqode/KeyStan), a fork of KeyGo
 * (https://github.com/OffRange/KeyGo). Licensed under the GNU General Public License v3.0 or
 * later, like the project it belongs to; see LICENSE.
 */

package de.davis.keygo.desktop.core.backup.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The payload carried inside a sealed backup envelope.
 *
 * Field names, nullability and ordering mirror `keygo_core::backup::model` one-to-one: Android
 * restores this with serde, so a rename here silently drops data instead of failing.
 */
@Serializable
data class Backup(
    val vaults: List<Vault> = emptyList(),
)

@Serializable
data class Vault(
    val name: String = "",
    /**
     * Names an icon from the client's own set. The format assigns it no meaning beyond a label to
     * hand back on restore, so an unknown value is the client's problem, not the format's.
     */
    val icon: String = "",
    val logins: List<Login> = emptyList(),
    val cards: List<Card> = emptyList(),
)

/**
 * The item fields every backup item shares, flattened exactly as the Rust `backup_item!` macro
 * flattens them, so that the JSON shape stays identical on both sides.
 */
@Serializable
data class Login(
    val title: String = "",
    val notes: String? = null,
    val tags: List<String> = emptyList(),
    val pinned: Boolean = false,
    val username: String? = null,
    val password: String? = null,
    @SerialName("totp_secret") val totpSecret: String? = null,
    val websites: List<String> = emptyList(),
    val passkeys: List<Passkey> = emptyList(),
)

@Serializable
data class Passkey(
    @SerialName("user_name") val userName: String = "",
    @SerialName("user_display_name") val userDisplayName: String = "",
    @SerialName("credential_id") val credentialId: ByteArray = ByteArray(0),
    @SerialName("private_key") val privateKey: ByteArray = ByteArray(0),
    val rp: String = "",
) {
    // ByteArray gives identity equality, which would make two structurally identical passkeys
    // compare unequal. Compare contents instead.
    override fun equals(other: Any?): Boolean = other is Passkey &&
        userName == other.userName &&
        userDisplayName == other.userDisplayName &&
        credentialId.contentEquals(other.credentialId) &&
        privateKey.contentEquals(other.privateKey) &&
        rp == other.rp

    override fun hashCode(): Int {
        var result = userName.hashCode()
        result = 31 * result + userDisplayName.hashCode()
        result = 31 * result + credentialId.contentHashCode()
        result = 31 * result + privateKey.contentHashCode()
        result = 31 * result + rp.hashCode()
        return result
    }
}

@Serializable
data class Card(
    val title: String = "",
    val notes: String? = null,
    val tags: List<String> = emptyList(),
    val pinned: Boolean = false,
    val cardholder: String? = null,
    val number: String = "",
    @SerialName("expiration_month") val expirationMonth: Int? = null,
    @SerialName("expiration_year") val expirationYear: Int? = null,
    val cvv: String? = null,
)
