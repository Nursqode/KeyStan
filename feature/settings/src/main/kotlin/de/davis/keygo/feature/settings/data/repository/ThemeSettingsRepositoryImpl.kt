package de.davis.keygo.feature.settings.data.repository

import androidx.datastore.core.DataStore
import de.davis.keygo.core.ui.theme.KeyPalette
import de.davis.keygo.core.ui.theme.ThemeMode
import de.davis.keygo.feature.settings.data.local.model.ProtoThemeSettings
import de.davis.keygo.feature.settings.data.mapper.toDomain
import de.davis.keygo.feature.settings.data.mapper.toProto
import de.davis.keygo.feature.settings.di.annotation.ThemeSettingsQualifier
import de.davis.keygo.feature.settings.domain.model.ThemeSettings
import de.davis.keygo.feature.settings.domain.repository.ThemeSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single
internal class ThemeSettingsRepositoryImpl(
    @ThemeSettingsQualifier
    private val dataStore: DataStore<ProtoThemeSettings>,
) : ThemeSettingsRepository {

    override fun observe(): Flow<ThemeSettings> = dataStore.data
        .map(ProtoThemeSettings::toDomain)

    // Each setter starts from the stored message, so writing one setting never resets the others.
    override suspend fun setPalette(palette: KeyPalette) {
        dataStore.updateData {
            it.toBuilder().setPalette(palette.toProto()).build()
        }
    }

    override suspend fun setMode(mode: ThemeMode) {
        dataStore.updateData {
            it.toBuilder().setMode(mode.toProto()).build()
        }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.updateData {
            it.toBuilder().setDynamicColor(enabled).build()
        }
    }
}
