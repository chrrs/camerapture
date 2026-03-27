import me.chrr.tapestry.gradle.platform.PlatformType

plugins {
    id("me.chrr.tapestry.gradle") version "1.0.2"
}

tapestry {
    versions {
        minecraft = prop("minecraft.version")
        fabricLoader = prop("fabric.loader.version")
        neoforge = prop("neoforge.version")
    }

    info {
        id = "camerapture"
        version = prop("mod.version")

        name = "Camerapture"
        description = "Take photos and display pictures in Minecraft!"
        authors = listOf("chrrrs")
        license = "MIT"

        url = "https://github.com/chrrs/camerapture"
        sources = "https://github.com/chrrs/camerapture"
        issues = "https://github.com/chrrs/camerapture/issues"

        icon = "assets/camerapture/icon.png"
    }

    transform {
        classTweaker = "camerapture.accesswidener"
        mixinConfigs.add("camerapture.mixins.json")
        mixinConfigs.add("camerapture-client.mixins.json")
        mixinConfigs(PlatformType.Fabric).add("camerapture-fabric.mixins.json")
    }

    depends {
        minecraft = prop("minecraft.compatible").map { it.split(",") }
        fabric("fabric-api", slug = "fabric-api")

        // FIXME: replace Cloth Config with Tapestry Config.
        fabric("cloth-config", slug = "cloth-config") { optional = true }
        neoforge("cloth-config", slug = "cloth-config") { optional = true }
    }

    game {
        runDir = rootProject.file("run")
        username = "chrrz"
    }

    publish {
        readChangelogFrom(rootProject.file("CHANGELOG.md"))
        modrinth = "9dzLWnmZ"
        curseforge = "1051342"
    }
}