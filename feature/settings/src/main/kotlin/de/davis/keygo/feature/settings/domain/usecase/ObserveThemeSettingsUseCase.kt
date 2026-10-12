package de.davis.keygo.feature.settings.domain.usecase

import de.davis.keygo.feature.settings.domain.model.ThemeSettings
import de.davis.keygo.feature.settings.domain.repository.ThemeSettingsRepository
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single

@Single
class ObserveThemeSettingsUseCase(
    private val repository: ThemeSettingsRepository,
) {

    /**
     * The stored appearance followed by every change, so a collector can draw from the first
     * emission alone instead of reading once and subscribing afterwards.
     */
    operator fun invoke(): Flow<ThemeSettings> = repository.observe()
}
