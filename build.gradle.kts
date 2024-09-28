import me.modmuss50.mpp.ReleaseType

plugins {
    id("fabric-loom") version "1.7-SNAPSHOT"
    id("me.modmuss50.mod-publish-plugin") version "0.7.4"
}

fun Project.prop(namespace: String, key: String) =
    property("$namespace.$key") as String

val minecraftVersion = stonecutter.current.version

// 1.20.5 changed some things about datapacks. Because of this, let's create a system
// to handle assets. Now we can make directories named [1.20.5] and [1.20], and it
// includes and excludes them appropriately.
val resourceVersion = if (stonecutter.eval(minecraftVersion, ">=1.20.5")) "1.20.5" else "1.20"

group = prop("mod", "group")
version = "${prop("mod", "version")}+mc$minecraftVersion"

base {
    archivesName.set(prop("mod", "name"))
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

    accessWidenerPath = rootProject.file("src/main/resources/[$resourceVersion]/camerapture.accesswidener")

    runConfigs["client"].runDir = "../../run"
    runConfigs["server"].runDir = "../../run/server"

    if (stonecutter.current.isActive) {
        runConfigs.all { ideConfigGenerated(true) }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:${prop("fabric", "yarn")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${prop("fabric", "loader")}")

    modImplementation("net.fabricmc.fabric-api:fabric-api:${prop("deps", "fabricApi")}")
    modImplementation("maven.modrinth:jade:${prop("deps", "jade")}")
    modCompileOnlyApi("maven.modrinth:first-person-model:${prop("deps", "firstPersonModel")}")

    modImplementation("com.terraformersmc:modmenu:${prop("deps", "modMenu")}")
    modApi("me.shedaniel.cloth:cloth-config-fabric:${prop("deps", "clothConfig")}") {
        exclude("net.fabricmc.fabric-api")
    }

    include(implementation("io.github.darkxanter:webp-imageio:0.3.2")!!)
}

tasks {
    processResources {
        // Keep matching files.
        filesMatching("[$resourceVersion]/**") {
            path = path.substring("[$resourceVersion]/".length)
        }

        // Remove the other ones.
        val opposite = if (resourceVersion == "1.20") "1.20.5" else "1.20"
        filesMatching("[$opposite]/**") {
            exclude()
        }

        // We construct our minecraft dependency string based on the versions provided in gradle.properties
        val gameVersions = prop("minecraft", "versions").split(",")
        val first = gameVersions.firstOrNull()!!
        val last = gameVersions.lastOrNull()!!
        val minecraftDependency = if (gameVersions.size == 1) first else ">=$first <=$last"

        // For fabric.mod.json, we source some properties from gradle.properties.
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") {
            expand(
                mapOf(
                    "modName" to prop("mod", "name"),
                    "version" to prop("mod", "version"),
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

publishMods {
    displayName.set("${prop("mod", "version")} - Fabric $minecraftVersion")

    file.set(tasks.remapJar.get().archiveFile)
    changelog.set(providers.environmentVariable("CHANGELOG"))
    type.set(if (prop("mod", "version").contains("beta")) ReleaseType.BETA else ReleaseType.STABLE)
    modLoaders.addAll("fabric", "quilt")

    val gameVersions = prop("minecraft", "versions").split(",")

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