/*
 * KeyStan Desktop - a Windows companion for the KeyGo Android password manager.
 * Copyright (C) 2026 Nursqode
 *
 * Part of KeyStan (https://github.com/Nursqode/KeyStan), a fork of KeyGo
 * (https://github.com/OffRange/KeyGo). Licensed under the GNU General Public License v3.0 or
 * later, like the project it belongs to; see LICENSE.
 */

package de.davis.keygo.desktop.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The vault icons the Android app knows, by the exact names it stores in a backup.
 *
 * The format treats a vault's icon as an opaque label to hand back on restore, so a name written
 * here that Android does not recognise would come back as an unknown icon. Using its list - and its
 * default of `Person` - keeps a vault created on either platform looking the same on both.
 */
object VaultIcons {

    val names: List<String> = listOf(
        "Person", "Home", "Favorite", "Work", "Business", "School", "MenuBook", "Lock", "Computer",
        "PhoneAndroid", "AccountBalanceWallet", "CreditCard", "ShoppingCart", "Flight", "Hotel",
        "DirectionsCar", "Restaurant", "LocalCafe", "FitnessCenter", "MusicNote", "SportsEsports", "Star",
    )

    val Default: String = "Person"

    fun vectorFor(name: String): ImageVector = when (name) {
        "Home" -> Icons.Filled.Home
        "Favorite" -> Icons.Filled.Favorite
        "Work" -> Icons.Filled.Work
        "Business" -> Icons.Filled.Business
        "School" -> Icons.Filled.School
        "MenuBook" -> Icons.AutoMirrored.Filled.MenuBook
        "Lock" -> Icons.Filled.Lock
        "Computer" -> Icons.Filled.Computer
        "PhoneAndroid" -> Icons.Filled.PhoneAndroid
        "AccountBalanceWallet" -> Icons.Filled.AccountBalanceWallet
        "CreditCard" -> Icons.Filled.CreditCard
        "ShoppingCart" -> Icons.Filled.ShoppingCart
        "Flight" -> Icons.Filled.Flight
        "Hotel" -> Icons.Filled.Hotel
        "DirectionsCar" -> Icons.Filled.DirectionsCar
        "Restaurant" -> Icons.Filled.Restaurant
        "LocalCafe" -> Icons.Filled.LocalCafe
        "FitnessCenter" -> Icons.Filled.FitnessCenter
        "MusicNote" -> Icons.Filled.MusicNote
        "SportsEsports" -> Icons.Filled.SportsEsports
        "Star" -> Icons.Filled.Star
        else -> Icons.Filled.Person
    }
}
