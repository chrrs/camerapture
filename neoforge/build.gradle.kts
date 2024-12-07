fun Project.hasProp(namespace: String, key: String) = hasProperty("$namespace.$key")
fun Project.prop(namespace: String, key: String) = property("$namespace.$key") as String

base.archivesName.set(rootProject.prop("mod", "name"))

architectury {
    platformSetupLoomIde()
    neoForge()
}

loom {
    accessWidenerPath = project(":common").loom.accessWidenerPath
    runConfigs.all { ideConfigGenerated(false) }
    runConfigs["client"].runDir = "../run"
    runConfigs["server"].runDir = "../run/server"
}

val common: Configuration by configurations.creating {
    configurations.compileClasspath.get().extendsFrom(this)
    configurations.runtimeClasspath.get().extendsFrom(this)
    configurations.getByName("developmentNeoForge").extendsFrom(this)
}

repositories {
    maven("https://maven.neoforged.net/releases/")
}

dependencies {
    "neoForge"("net.neoforged:neoforge:${rootProject.prop("neoforge", "version")}")

    include("io.github.darkxanter:webp-imageio:0.3.2")
    forgeRuntimeLibrary("io.github.darkxanter:webp-imageio:0.3.2")

    common(project(":common", "namedElements")) { isTransitive = false }
    shadowCommon(project(":common", "transformProductionNeoForge")) { isTransitive = false }
}

tasks.processResources {
    from(project(":common").sourceSets.map { it.resources })

    // We construct our minecraft dependency string based on the versions provided in gradle.properties
    val gameVersions = rootProject.prop("platform", "versions").split(",")
    val first = gameVersions.firstOrNull()!!
    val last = gameVersions.lastOrNull()!!
    val minecraftDependency = if (gameVersions.size == 1) "[$first]" else "[$first, $last]"

    // For neoforge.mods.toml and pack.mcmeta, we source some properties from gradle.properties.
    filesMatching(listOf("META-INF/neoforge.mods.toml", "pack.mcmeta")) {
        expand(
            "modName" to rootProject.prop("mod", "name"),
            "version" to rootProject.prop("mod", "version"),
            "minecraftDependency" to minecraftDependency,
        )
    }
}
