package de.davis.keygo.feature.settings.domain.repository

import de.davis.keygo.core.ui.theme.KeyPalette
import de.davis.keygo.core.ui.theme.ThemeMode
import de.davis.keygo.feature.settings.domain.model.ThemeSettings
import kotlinx.coroutines.flow.Flow

interface ThemeSettingsRepository {

    fun observe(): Flow<ThemeSettings>

    suspend fun setPalette(palette: KeyPalette)

    suspend fun setMode(mode: ThemeMode)

    suspend fun setDynamicColor(enabled: Boolean)
}
