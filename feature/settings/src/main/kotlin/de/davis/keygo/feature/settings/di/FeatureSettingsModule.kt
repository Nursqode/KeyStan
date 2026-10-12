package de.davis.keygo.feature.settings.di

import android.content.Context
import androidx.datastore.dataStore
import de.davis.keygo.core.util.data.serializer.DefaultProtoSerializer
import de.davis.keygo.feature.settings.data.local.model.ProtoThemeSettings
import de.davis.keygo.feature.settings.di.annotation.ThemeSettingsQualifier
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@Configuration
@ComponentScan("de.davis.keygo.feature.settings")
object FeatureSettingsModule {

    private val Context.themeSettingsDataStore by dataStore(
        "theme_settings.pb",
        DefaultProtoSerializer(
            defaultInstance = ProtoThemeSettings.getDefaultInstance(),
            parser = ProtoThemeSettings.parser()
        )
    )

    @Single
    @ThemeSettingsQualifier
    internal fun provideThemeSettingsDataStore(context: Context) =
        context.themeSettingsDataStore
}
