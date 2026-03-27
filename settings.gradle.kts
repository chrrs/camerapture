pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.chrr.me/releases/")
        gradlePluginPortal()
    }
}

include("common", "fabric", "neoforge")