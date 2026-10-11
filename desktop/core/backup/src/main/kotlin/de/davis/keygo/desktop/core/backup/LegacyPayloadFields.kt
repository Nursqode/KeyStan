/*
 * KeyStan Desktop - a Windows companion for the KeyGo Android password manager.
 * Copyright (C) 2026 Nursqode
 *
 * Part of KeyStan (https://github.com/Nursqode/KeyStan), a fork of KeyGo
 * (https://github.com/OffRange/KeyGo). Licensed under the GNU General Public License v3.0 or
 * later, like the project it belongs to; see LICENSE.
 */

package de.davis.keygo.desktop.core.backup

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Promotes backup fields that older writers used, before the payload is decoded.
 *
 * The frozen v1 backups carry a login's single `website` string and a single `passkey` object where
 * the current model has the `websites` and `passkeys` lists. serde ignores unknown keys, so the
 * Android app silently drops those values on import; the desktop turns them into the list the
 * current model reads, which is why an old file survives a desktop round trip without losing a site.
 *
 * This only ever *adds* the current field - nothing is renamed or removed, and what is written back
 * is the current format either way.
 */
internal fun promoteLegacyFields(payload: JsonElement): JsonElement {
    val root = payload as? JsonObject ?: return payload
    val vaults = root["vaults"] as? JsonArray ?: return payload

    val promotedVaults = JsonArray(
        vaults.map { element ->
            val vault = element as? JsonObject ?: return@map element
            val logins = vault["logins"] as? JsonArray ?: return@map element
            JsonObject(vault + ("logins" to JsonArray(logins.map(::promoteLogin))))
        },
    )
    return JsonObject(root + ("vaults" to promotedVaults))
}

private fun promoteLogin(login: JsonElement): JsonElement {
    val fields = login as? JsonObject ?: return login
    var promoted = fields

    if (fields["websites"].isMissingOrEmpty()) {
        val website = fields["website"]
        if (website is JsonPrimitive && website.isString && website !is JsonNull)
            promoted = JsonObject(promoted + ("websites" to JsonArray(listOf(website))))
    }

    if (fields["passkeys"].isMissingOrEmpty()) {
        val passkey = fields["passkey"]
        if (passkey is JsonObject)
            promoted = JsonObject(promoted + ("passkeys" to JsonArray(listOf(passkey))))
    }

    return promoted
}

private fun JsonElement?.isMissingOrEmpty(): Boolean =
    this == null || (this is JsonArray && isEmpty())
