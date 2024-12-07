fun Project.hasProp(namespace: String, key: String) = hasProperty("$namespace.$key")
fun Project.prop(namespace: String, key: String) = property("$namespace.$key") as String

architectury {
    platformSetupLoomIde()
    fabric()
}

loom {
    accessWidenerPath = project(":common").loom.accessWidenerPath
    runConfigs.all { ideConfigGenerated(false) }
    runConfigs["client"].runDir = "../run"
    runConfigs["server"].runDir = "../run/server"

    @Suppress("UnstableApiUsage")
    mixin.useLegacyMixinAp = false
}

val common: Configuration by configurations.creating {
    configurations.compileClasspath.get().extendsFrom(this)
    configurations.runtimeClasspath.get().extendsFrom(this)
    configurations.getByName("developmentFabric").extendsFrom(this)
}

repositories {
    maven("https://maven.terraformersmc.com/releases/") { content { includeGroup("com.terraformersmc") } }
}

dependencies {
    modImplementation("net.fabricmc:fabric-loader:${rootProject.prop("fabric", "loaderVersion")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${rootProject.prop("fabric", "apiVersion")}")

    modApi("com.terraformersmc:modmenu:${rootProject.prop("modmenu", "version")}")

    include("io.github.darkxanter:webp-imageio:0.3.2")
    implementation("io.github.darkxanter:webp-imageio:0.3.2")

    common(project(":common", "namedElements")) { isTransitive = false }
    shadowCommon(project(":common", "transformProductionFabric")) { isTransitive = false }
}

tasks.processResources {
    from(project(":common").sourceSets.map { it.resources })

    // We construct our minecraft dependency string based on the versions provided in gradle.properties
    val gameVersions = rootProject.prop("platform", "versions").split(",")
    val first = gameVersions.firstOrNull()!!
    val last = gameVersions.lastOrNull()!!
    val minecraftDependency = if (gameVersions.size == 1) first else ">=$first <=$last"

    // For fabric.mod.json, we source some properties from gradle.properties.
    filesMatching("fabric.mod.json") {
        expand(
            "modName" to rootProject.prop("mod", "name"),
            "version" to rootProject.prop("mod", "version"),
            "minecraftDependency" to minecraftDependency,
        )
    }
}
