import java.lang.System.getenv

plugins {
    id("fabric-loom") version "1.6-SNAPSHOT"
    id("com.modrinth.minotaur") version "2.+"
}

val minecraftVersion = stonecutter.current.version
val compatibleVersions: String by project
val minecraftDependency: String by project

val modVersion: String by project
val mavenGroup: String by project
val archivesBase: String by project

val yarnMappings: String by project
val fabricVersion: String by project

val fabricApiVersion: String by project
val jadeVersion: String by project

group = mavenGroup
version = "$modVersion+mc$minecraftVersion"

base {
    archivesName.set(archivesBase)
}

repositories {
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

    runConfigs["client"].runDir = "../../run"
    runConfigs["server"].runDir = "../../run/server"

    runConfigs.all {
        ideConfigGenerated(false)
    }

    if (stonecutter.current.isActive) {
        rootProject.tasks.register("runActiveClient") {
            group = "project"
            dependsOn(tasks.named("runClient"))
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$yarnMappings:v2")
    modImplementation("net.fabricmc:fabric-loader:$fabricVersion")

    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    modImplementation("maven.modrinth:jade:$jadeVersion")

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

    modrinth.get().dependsOn(build)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

modrinth {
    token.set(getenv("MODRINTH_TOKEN"))
    projectId.set("9dzLWnmZ")

    versionName.set("$modVersion - Fabric $minecraftVersion")
    versionNumber.set("$version")
    versionType.set(if (modVersion.contains("beta")) "beta" else "release")
    changelog.set(getenv("CHANGELOG") ?: "No changelog provided.")

    gameVersions.addAll(compatibleVersions.split(','))
    loaders.addAll("fabric", "quilt")

    uploadFile.set(tasks.remapJar.get())

    dependencies {
        required.project("fabric-api")
    }
}