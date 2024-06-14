import dev.kikugie.stonecutter.gradle.StonecutterSettings

pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases")
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.4"
}

extensions.configure<StonecutterSettings> {
    shared {
        versions("1.20.1", "1.20.4")
        vcsVersion = "1.20.4"
    }

    kotlinController = true
    centralScript = "build.gradle.kts"
    create(rootProject)
}

rootProject.name = "Camerapture"