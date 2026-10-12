package de.davis.keygo.feature.settings.data.repository

import de.davis.keygo.core.ui.theme.KeyPalette
import de.davis.keygo.core.ui.theme.ThemeMode
import de.davis.keygo.feature.settings.data.local.model.ProtoKeyPalette
import de.davis.keygo.feature.settings.data.local.model.ProtoThemeMode
import de.davis.keygo.feature.settings.data.local.model.ProtoThemeSettings
import de.davis.keygo.feature.settings.domain.model.ThemeSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThemeSettingsRepositoryImplTest {

    private val dataStore = FakeDataStore(ProtoThemeSettings.getDefaultInstance())
    private val repository = ThemeSettingsRepositoryImpl(dataStore)

    @Test
    fun defaultsAreSystemPaletteSystemModeAndWallpaperColors() = runTest {
        assertEquals(
            ThemeSettings(palette = KeyPalette.System, mode = ThemeMode.System, dynamicColor = true),
            repository.observe().first(),
        )
    }

    @Test
    fun settingThePaletteIsRemembered() = runTest {
        repository.setPalette(KeyPalette.Red)

        assertEquals(ThemeSettings(palette = KeyPalette.Red), repository.observe().first())
        assertEquals(ProtoKeyPalette.PALETTE_RED, dataStore.state.value.palette)
    }

    @Test
    fun settingTheModeIsRemembered() = runTest {
        repository.setMode(ThemeMode.Dark)

        assertEquals(ThemeSettings(mode = ThemeMode.Dark), repository.observe().first())
        assertEquals(ProtoThemeMode.THEME_DARK, dataStore.state.value.mode)
    }

    @Test
    fun turningDynamicColorOffIsRemembered() = runTest {
        repository.setDynamicColor(false)

        assertEquals(ThemeSettings(dynamicColor = false), repository.observe().first())
        assertFalse(dataStore.state.value.dynamicColor)
    }

    @Test
    fun eachSettingLeavesTheOthersAlone() = runTest {
        repository.setPalette(KeyPalette.Red)
        repository.setMode(ThemeMode.Dark)
        repository.setDynamicColor(false)
        repository.setMode(ThemeMode.System)

        assertEquals(
            ThemeSettings(palette = KeyPalette.Red, mode = ThemeMode.System, dynamicColor = false),
            repository.observe().first(),
        )
    }

    @Test
    fun observersSeeEveryChange() = runTest {
        val seen = mutableListOf<ThemeSettings>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observe().take(3).toList(seen)
        }

        repository.setMode(ThemeMode.Dark)
        repository.setMode(ThemeMode.Light)

        assertEquals(
            listOf(ThemeMode.System, ThemeMode.Dark, ThemeMode.Light)
                .map { ThemeSettings(mode = it) },
            seen,
        )
    }

    @Test
    fun observingStartsFromTheStoredValue() = runTest {
        dataStore.state.value = ProtoThemeSettings.newBuilder()
            .setPalette(ProtoKeyPalette.PALETTE_RED)
            .setMode(ProtoThemeMode.THEME_LIGHT)
            .setDynamicColor(false)
            .build()

        assertEquals(
            ThemeSettings(palette = KeyPalette.Red, mode = ThemeMode.Light, dynamicColor = false),
            repository.observe().first(),
        )
    }

    @Test
    fun aStoredProtoWithoutTheOptionalFieldReadsAsWallpaperColors() = runTest {
        // What a store written before this setting existed looks like: other fields set, no
        // dynamicColor at all. Reading the field plainly would report proto3's false.
        dataStore.state.value = ProtoThemeSettings.newBuilder()
            .setPalette(ProtoKeyPalette.PALETTE_RED)
            .build()

        val settings = repository.observe().first()

        assertFalse(dataStore.state.value.hasDynamicColor())
        assertTrue(settings.dynamicColor)
        assertEquals(KeyPalette.Red, settings.palette)
    }

    @Test
    fun anUnknownPaletteNumberFallsBackToSystem() = runTest {
        // A raw number no build-time ProtoKeyPalette entry claims - stands in for a value a newer
        // app version wrote, read back after a downgrade. setPalette(UNRECOGNIZED) itself throws,
        // so the raw *Value setter is the only way to construct this on purpose.
        dataStore.state.value = ProtoThemeSettings.newBuilder().setPaletteValue(99).build()

        assertEquals(KeyPalette.System, repository.observe().first().palette)
    }

    @Test
    fun anUnknownModeNumberFallsBackToSystem() = runTest {
        dataStore.state.value = ProtoThemeSettings.newBuilder().setModeValue(99).build()

        assertEquals(ThemeMode.System, repository.observe().first().mode)
    }
}
