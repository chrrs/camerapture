import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import me.modmuss50.mpp.ReleaseType
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.RemapJarTask
import org.gradle.internal.extensions.stdlib.capitalized

plugins {
    id("architectury-plugin") version "3.4-SNAPSHOT"
    id("dev.architectury.loom") version "1.7-SNAPSHOT" apply false
    id("com.gradleup.shadow") version "9.0.0-beta2" apply false
    id("me.modmuss50.mod-publish-plugin") version "0.8.1"
}

fun Project.hasProp(namespace: String, key: String) = hasProperty("$namespace.$key")
fun Project.prop(namespace: String, key: String) = property("$namespace.$key") as String

val versions = prop("platform", "versions").split(",")
architectury.minecraft = versions.last()

group = prop("mod", "group")
version = "${prop("mod", "version")}+mc${versions.last()}"

tasks {
    // Create a new task `buildAll` that builds and copies all jars to a common output directory.
    val buildAll by registering(Copy::class) {
        group = "build"

        val tasks = subprojects.filter { it.path != ":common" }.map { it.tasks.named("remapJar") }
        dependsOn(tasks)

        from(tasks)
        into(layout.buildDirectory.dir("libs"))
    }

    // We call buildAll to make sure everything is built before publishing.
    publishMods.get().dependsOn(buildAll)
}

subprojects {
    apply(plugin = "architectury-plugin")
    apply(plugin = "dev.architectury.loom")

    group = rootProject.group
    version = "${rootProject.version}-$name"

    val base = extensions.getByType<BasePluginExtension>()
    base.archivesName.set(rootProject.prop("mod", "name"))

    configure<LoomGradleExtensionAPI> {
        dependencies {
            "minecraft"("com.mojang:minecraft:${versions.last()}")

            // Patch Yarn to work properly with NeoForge.
            @Suppress("UnstableApiUsage")
            "mappings"(layered {
                mappings("net.fabricmc:yarn:${prop("fabric", "yarnVersion")}:v2")
                mappings("dev.architectury:yarn-mappings-patch-neoforge:${prop("neoforge", "yarnPatch")}")
            })
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    // Shadow `common` into all loader projects.
    if (path != ":common") {
        apply(plugin = "com.gradleup.shadow")

        val shadowCommon by configurations.creating {
            configurations.getByName("implementation").extendsFrom(this)
        }

        tasks.named<ShadowJar>("shadowJar") {
            archiveClassifier.set("dev-shadow")
            configurations = listOf(shadowCommon)
        }

        tasks.named<RemapJarTask>("remapJar") {
            inputFile.set(tasks.named<ShadowJar>("shadowJar").flatMap { it.archiveFile })
            dependsOn("shadowJar")
            injectAccessWidener.set(true)
        }

        tasks.named<Jar>("jar") {
            archiveClassifier.set("dev")
        }
    }
}

/// Find the changelog entry for the given version in `CHANGELOG.md`.
fun fetchChangelog(modVersion: String): String {
    val regex = Regex("## ${Regex.escape(modVersion)}\\n+([\\S\\s]*?)(?:\$|\\n+##)")
    val content = file("CHANGELOG.md").readText().replace("\r\n", "\n")
    return regex.find(content)?.groupValues?.get(1) ?: "*No changelog.*"
}

publishMods {
    val modVersion = prop("mod", "version")
    changelog.set(fetchChangelog(modVersion))

    type.set(when {
        modVersion.contains("alpha") -> ReleaseType.ALPHA
        modVersion.contains("beta") -> ReleaseType.BETA
        else -> ReleaseType.STABLE
    })

    /// Generate some common options between Modrinth and CurseForge publishing.
    fun platformOptions(platform: String) = publishOptions {
        val project = project(":$platform")
        val name = if (platform == "neoforge") "NeoForge" else platform.capitalized()

        displayName.set("$modVersion - $name ${versions.last()}")
        version.set(project.version.toString())
        modLoaders.addAll(project.prop("platform", "loaders").split(","))
        file.set(project.tasks.getByName<RemapJarTask>("remapJar").archiveFile)
    }.get()

    modrinth("modrinthFabric") {
        projectId.set(prop("modrinth", "id"))
        accessToken.set(providers.environmentVariable("MODRINTH_TOKEN"))

        from(platformOptions("fabric"))
        minecraftVersions.addAll(versions)
        requires("fabric-api")
        optional("cloth-config")
    }

    modrinth("modrinthNeoForge") {
        projectId.set(prop("modrinth", "id"))
        accessToken.set(providers.environmentVariable("MODRINTH_TOKEN"))

        from(platformOptions("neoforge"))
        minecraftVersions.addAll(versions)
        optional("cloth-config")
    }

    curseforge("curseforgeFabric") {
        projectId.set(prop("curseforge", "id"))
        accessToken.set(providers.environmentVariable("CURSEFORGE_TOKEN"))

        from(platformOptions("fabric"))
        minecraftVersions.addAll(versions)
        requires("fabric-api")
        optional("cloth-config")
    }

    curseforge("curseforgeNeoForge") {
        projectId.set(prop("curseforge", "id"))
        accessToken.set(providers.environmentVariable("CURSEFORGE_TOKEN"))

        from(platformOptions("neoforge"))
        minecraftVersions.addAll(versions)
        optional("cloth-config")
    }

    github {
        repository.set(prop("github", "repository"))
        accessToken = providers.environmentVariable("GITHUB_TOKEN")

        val tag = "v$modVersion"
        tagName.set(tag)
        displayName.set(tag)
        commitish.set("refs/tags/$tag")

        allowEmptyFiles.set(true)
        additionalFiles.from(subprojects.filter { it.path != ":common" }
            .map { it.tasks.getByName<RemapJarTask>("remapJar") }
            .map { it.archiveFile })
    }
}