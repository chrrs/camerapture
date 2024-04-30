plugins {
    id("fabric-loom") version "1.5-SNAPSHOT"
}

val minecraftVersion: String by project
val yarnMappings: String by project
val loaderVersion: String by project

val modVersion: String by project
val mavenGroup: String by project
val archivesBase: String by project

val fabricVersion: String by project

group = mavenGroup
version = "$modVersion+mc$minecraftVersion"

base {
    archivesName.set(archivesBase)
}

repositories {
}

loom {
    splitEnvironmentSourceSets()

    mods {
        create("camerapture") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }
}

dependencies {
    // To change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$yarnMappings:v2")
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")

    // Fabric API. This is technically optional, but you probably want it anyway.
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricVersion")

    include(implementation("io.github.darkxanter:webp-imageio:0.3.2")!!)
}

tasks {
    processResources {
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") {
            expand(mutableMapOf(
                "version" to project.version,
                "loader_version" to loaderVersion,
                "minecraft_version" to minecraftVersion,
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