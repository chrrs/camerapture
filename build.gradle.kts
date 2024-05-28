import java.lang.System.getenv

plugins {
    id("fabric-loom") version "1.6-SNAPSHOT"
    id("com.modrinth.minotaur") version "2.+"
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

val modVersion = property("mod.version")
val loaderVersion = property("fabric.loader")
val minecraftDependency = property("deps.minecraft")

tasks {
    processResources {

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

    modrinth.get().dependsOn(build)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

modrinth {
    token.set(getenv("MODRINTH_TOKEN"))
    projectId.set(property("modrinth.id") as String)

    val modVersion = property("mod.version") as String
    versionName.set("$modVersion - Fabric $minecraftVersion")
    versionType.set(if (modVersion.contains("beta")) "beta" else "release")

    versionNumber.set("$version")
    changelog.set(getenv("CHANGELOG") ?: "No changelog provided.")

    gameVersions.addAll((property("modrinth.compatibleVersions") as String).split(','))
    loaders.addAll("fabric", "quilt")

    uploadFile.set(tasks.remapJar.get())

    dependencies {
        required.project("fabric-api")
    }
}