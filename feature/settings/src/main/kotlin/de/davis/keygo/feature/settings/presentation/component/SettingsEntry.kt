package de.davis.keygo.feature.settings.presentation.component

import androidx.annotation.StringRes
import androidx.compose.material3.ListItemColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import de.davis.keygo.core.util.presentation.UIText

internal sealed interface SettingsEntry {
    @get:StringRes
    val title: Int
    val icon: ImageVector?

    /** [UIText] rather than a string resource: some rows describe live state. */
    val supporting: UIText?

    val colors: ListItemColors

    data class Toggle(
        @param:StringRes override val title: Int,
        override val icon: ImageVector? = null,
        override val supporting: UIText? = null,
        override val colors: ListItemColors,
        val checked: Boolean,
        val enabled: Boolean = true,
        val onCheckedChange: (Boolean) -> Unit,
    ) : SettingsEntry

    data class Action(
        @param:StringRes override val title: Int,
        override val icon: ImageVector? = null,
        override val supporting: UIText? = null,
        override val colors: ListItemColors,
        val navigationIcon: ImageVector? = null,
        val onClick: () -> Unit,
    ) : SettingsEntry

    data class Value(
        @param:StringRes override val title: Int,
        override val icon: ImageVector? = null,
        override val supporting: UIText? = null,
        override val colors: ListItemColors,
        val value: String,
        val onClick: (() -> Unit)? = null,
    ) : SettingsEntry

    data class Picker(
        @param:StringRes override val title: Int,
        override val icon: ImageVector? = null,
        override val colors: ListItemColors,
        val selectedIndex: Int,
        val options: List<Option>,
    ) : SettingsEntry {

        override val supporting: UIText? = options.getOrNull(selectedIndex)?.label

        /**
         * The colour this option would paint the interface in, or `null` for an option that has no
         * colour to show. It is the scheme a taken choice resolves to, so the swatch is a promise
         * the interface then keeps rather than a decoration standing in for one.
         */
        data class Option(
            val label: UIText,
            val onSelect: () -> Unit,
            val sample: Color? = null,
        )
    }
}

internal data class SettingsSection(
    @param:StringRes val title: Int,
    val entries: List<SettingsEntry>,
)
