import me.modmuss50.mpp.ReleaseType

plugins {
    id("fabric-loom") version "1.6-SNAPSHOT"
    id("me.modmuss50.mod-publish-plugin") version "0.5.1"
}

val minecraftVersion = stonecutter.current.version

group = property("mod.mavenGroup") as String
version = "${property("mod.version")}+mc$minecraftVersion"

base {
    archivesName.set(property("mod.archivesName") as String)
}

repositories {
    maven("https://maven.shedaniel.me/")
    maven("https://maven.terraformersmc.com/releases/")

    maven("https://api.modrinth.com/maven") {
        content {
            includeGroup("maven.modrinth")
        }
    }
}

loom {
    splitEnvironmentSourceSets()

    mods {
        create("camerapture") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }

    accessWidenerPath = rootProject.file("src/main/resources/camerapture.accesswidener")

    runConfigs["client"].runDir = "../../run"
    runConfigs["server"].runDir = "../../run/server"

    if (stonecutter.current.isActive) {
        runConfigs.all { ideConfigGenerated(true) }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:${property("fabric.yarn")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("fabric.loader")}")

    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabricApi")}")
    modImplementation("maven.modrinth:jade:${property("deps.jade")}")

    modImplementation("com.terraformersmc:modmenu:${property("deps.modMenu")}")
    modApi("me.shedaniel.cloth:cloth-config-fabric:${property("deps.clothConfig")}") {
        exclude("net.fabricmc.fabric-api")
    }

    include(implementation("io.github.darkxanter:webp-imageio:0.3.2")!!)
}

val modVersion = property("mod.version") as String
val loaderVersion = property("fabric.loader") as String
val minecraftDependency = property("deps.minecraft") as String

tasks {
    processResources {
        // 1.20.5 changed some things about datapacks. Because of this, let's create a system
        // to handle assets. Now we can make directories named [1.20.5] and [1.20], and it
        // includes and excludes them appropriately.
        val version = if (stonecutter.eval(minecraftVersion, ">=1.20.5")) "1.20.5" else "1.20"
        val opposite = if (version == "1.20") "1.20.5" else "1.20"

        // Keep matching files.
        filesMatching("data/camerapture/[$version]/**") {
            path = path.replaceFirst("/[$version]", "")
        }

        // Remove the other ones.
        filesMatching("data/camerapture/[$opposite]/**") {
            exclude()
        }

        // For fabric.mod.json, we source some properties from gradle.properties.
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") {
            expand(
                mapOf(
                    "version" to modVersion,
                    "loaderVersion" to loaderVersion,
                    "minecraftDependency" to minecraftDependency,
                )
            )
        }
    }

    jar {
        from("LICENSE")
    }

    publishMods.get().dependsOn(build)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

fun fetchChangelog() =
    rootProject.file("CHANGELOG.md").readText()
        .replace("\r\n", "\n")
        .substringAfter("## $modVersion\n")
        .substringBefore("\n## ")

publishMods {
    val gameVersions = (property("modrinth.compatibleVersions") as String).split(',')

    displayName.set("$modVersion - Fabric $minecraftVersion")
    type.set(if (modVersion.contains("beta")) ReleaseType.BETA else ReleaseType.STABLE)
    modLoaders.addAll("fabric", "quilt")

    file.set(tasks.remapJar.get().archiveFile)

    // If the CHANGELOG environment variable is not set, we'll just fetch it ourselves.
    changelog.set(
        providers.environmentVariable("CHANGELOG")
            .orElse(providers.provider(::fetchChangelog))
    )

    modrinth {
        projectId.set(property("modrinth.id") as String)
        accessToken.set(providers.environmentVariable("MODRINTH_TOKEN"))
        minecraftVersions.addAll(gameVersions)
        requires("fabric-api")
    }

    curseforge {
        projectId.set(property("curseforge.id") as String)
        accessToken.set(providers.environmentVariable("CURSEFORGE_TOKEN"))
        minecraftVersions.addAll(gameVersions)
        requires("fabric-api")
    }
}