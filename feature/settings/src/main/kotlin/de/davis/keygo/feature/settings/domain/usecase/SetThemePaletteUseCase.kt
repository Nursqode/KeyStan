package de.davis.keygo.feature.settings.domain.usecase

import de.davis.keygo.core.ui.theme.KeyPalette
import de.davis.keygo.feature.settings.domain.repository.ThemeSettingsRepository
import org.koin.core.annotation.Single

@Single
class SetThemePaletteUseCase(
    private val repository: ThemeSettingsRepository,
) {

    suspend operator fun invoke(palette: KeyPalette) = repository.setPalette(palette)
}
