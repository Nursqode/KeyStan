package de.davis.keygo.feature.settings.domain.usecase

import de.davis.keygo.feature.settings.domain.repository.ThemeSettingsRepository
import org.koin.core.annotation.Single

@Single
class SetDynamicColorUseCase(
    private val repository: ThemeSettingsRepository,
) {

    suspend operator fun invoke(enabled: Boolean) = repository.setDynamicColor(enabled)
}
