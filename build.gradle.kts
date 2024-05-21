import java.lang.System.getenv

plugins {
    id("fabric-loom") version "1.6-SNAPSHOT"
    id("com.modrinth.minotaur") version "2.+"
}

val minecraftVersion = stonecutter.current.version
val minecraftDependency: String by project

val modVersion: String by project
val mavenGroup: String by project
val archivesBase: String by project

val yarnMappings: String by project
val fabricVersion: String by project

val fabricApiVersion: String by project
val jadeVersionId: String by project

group = mavenGroup
version = "$modVersion+mc$minecraftVersion"

base {
    archivesName.set(archivesBase)
}

repositories {
    maven("https://www.cursemaven.com") {
        content {
            includeGroup("curse.maven")
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

    runConfigs["client"].runDir = "../../run"
    runConfigs["server"].runDir = "../../run/server"

    if (stonecutter.current.isActive) {
        runConfigs.all { ideConfigGenerated(true) }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$yarnMappings:v2")
    modImplementation("net.fabricmc:fabric-loader:$fabricVersion")

    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    modImplementation("curse.maven:jade-324717:$jadeVersionId")

    include(implementation("io.github.darkxanter:webp-imageio:0.3.2")!!)
}

tasks {
    processResources {
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") {
            expand(mapOf(
                "version" to modVersion,
                "loaderVersion" to fabricVersion,
                "minecraftDependency" to minecraftDependency
            ))
        }
    }

    jar {
        from("LICENSE")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

if (stonecutter.current.isActive) {
    modrinth {
        token = getenv("MODRINTH_TOKEN")
        projectId = "9dzLWnmZ"

        versionName = "$modVersion - Fabric $minecraftVersion"
        versionNumber = modVersion
        versionType = if (modVersion.contains("beta")) "beta" else "release"
        changelog = getenv("CHANGELOG") ?: "No changelog provided."

        gameVersions.add(minecraftVersion)
        loaders.addAll("fabric", "quilt")

        uploadFile.set(tasks.remapJar)

        dependencies {
            required.project("fabric-api")
        }
    }
}