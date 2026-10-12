package de.davis.keygo.feature.settings.domain.usecase

import de.davis.keygo.core.ui.theme.ThemeMode
import de.davis.keygo.feature.settings.domain.repository.ThemeSettingsRepository
import org.koin.core.annotation.Single

@Single
class SetThemeModeUseCase(
    private val repository: ThemeSettingsRepository,
) {

    suspend operator fun invoke(mode: ThemeMode) = repository.setMode(mode)
}
