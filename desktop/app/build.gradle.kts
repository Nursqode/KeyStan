import org.gradle.api.tasks.JavaExec
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:ui"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
}

tasks.test {
    useJUnit()
}

/**
 * Skiko unpacks its native rendering library into `~/.skiko` unless told otherwise, which fails
 * where the home directory is read-only or sandboxed. `-Pskiko.library.path=<dir>` points the run
 * at a writable directory - useful for a portable install, and required for an automated smoke run.
 */
tasks.matching { it.name == "run" }.configureEach {
    if (this is JavaExec) {
        providers.gradleProperty("skiko.library.path").orNull?.let { systemProperty("skiko.library.path", it) }
        providers.gradleProperty("keystan.dataDir").orNull?.let { systemProperty("keystan.dataDir", it) }
    }
}

compose.desktop {
    application {
        mainClass = "de.davis.keygo.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "KeyStan"
            packageVersion = "1.1.0"
            description = "A local-first password manager, compatible with KeyGo on Android"
            vendor = "Nursqode"
            copyright = "© 2026 Nursqode. GNU GPL v3.0 or later."

            windows {
                // A stable upgrade UUID is what makes 1.2.0 replace 1.1.0 instead of being
                // installed beside it; jpackage would otherwise generate a new one every build.
                upgradeUuid = "35ED3B02-6522-3DDB-A3D6-454F9055E051"

                // The installer's whole point over the portable folder: a Start-menu entry and a
                // desktop shortcut, and a per-user install so no administrator prompt appears.
                menu = true
                shortcut = true
                menuGroup = "KeyStan"
                perUserInstall = true
                dirChooser = true
            }
        }
    }
}
