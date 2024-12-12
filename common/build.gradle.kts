fun Project.hasProp(namespace: String, key: String) = hasProperty("$namespace.$key")
fun Project.prop(namespace: String, key: String) = property("$namespace.$key") as String

architectury {
    common("fabric", "forge")
}

loom {
    accessWidenerPath.set(file("src/main/resources/camerapture.accesswidener"))
    splitEnvironmentSourceSets()

    @Suppress("UnstableApiUsage")
    mixin.useLegacyMixinAp = false
}

repositories {
    maven("https://maven.shedaniel.me/") { content { includeGroup("me.shedaniel.cloth") } }
    maven("https://api.modrinth.com/maven") { content { includeGroup("maven.modrinth") } }
}

dependencies {
    // Include Fabric loader to have access to the @Environment annotation.
    modCompileOnlyApi("net.fabricmc:fabric-loader:${rootProject.prop("fabric", "loaderVersion")}")

    // Compat dependencies
    modCompileOnlyApi("me.shedaniel.cloth:cloth-config-fabric:${rootProject.prop("clothconfig", "version")}")
    modCompileOnlyApi("maven.modrinth:jade:${rootProject.prop("jade", "version")}+fabric")
    modCompileOnlyApi("maven.modrinth:first-person-model:${rootProject.prop("firstpersonmodel", "version")}")

    // ImageIO-WebP extension
    implementation("io.github.darkxanter:webp-imageio:0.3.2")
}