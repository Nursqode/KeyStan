plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":core:vault"))
    api(compose.desktop.currentOs)
    // The plugin's `compose.material3` accessor is deprecated in favour of naming the artifact
    // directly, but it is what pins the Material 3 version the plugin was built against; a literal
    // coordinate here would be a second version to keep in step by hand.
    @Suppress("DEPRECATION")
    api(compose.material3)
    @Suppress("DEPRECATION")
    api(compose.materialIconsExtended)
    api(libs.kotlinx.coroutines.swing)
}
