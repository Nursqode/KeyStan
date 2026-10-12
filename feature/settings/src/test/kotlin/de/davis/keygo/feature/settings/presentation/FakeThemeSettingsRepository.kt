package de.davis.keygo.feature.settings.presentation

import de.davis.keygo.core.ui.theme.KeyPalette
import de.davis.keygo.core.ui.theme.ThemeMode
import de.davis.keygo.feature.settings.domain.model.ThemeSettings
import de.davis.keygo.feature.settings.domain.repository.ThemeSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class FakeThemeSettingsRepository(
    settings: ThemeSettings = ThemeSettings(),
) : ThemeSettingsRepository {

    val state = MutableStateFlow(settings)

    /** What callers asked to store, so a test can tell a write that happened from one that did not. */
    val setPaletteCalls = mutableListOf<KeyPalette>()
    val setModeCalls = mutableListOf<ThemeMode>()
    val setDynamicColorCalls = mutableListOf<Boolean>()

    override fun observe(): Flow<ThemeSettings> = state

    override suspend fun setPalette(palette: KeyPalette) {
        setPaletteCalls += palette
        state.update { it.copy(palette = palette) }
    }

    override suspend fun setMode(mode: ThemeMode) {
        setModeCalls += mode
        state.update { it.copy(mode = mode) }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        setDynamicColorCalls += enabled
        state.update { it.copy(dynamicColor = enabled) }
    }
}
