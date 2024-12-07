import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.RemapJarTask

plugins {
    id("architectury-plugin") version "3.4-SNAPSHOT"
    id("dev.architectury.loom") version "1.7-SNAPSHOT" apply false
    id("com.gradleup.shadow") version "9.0.0-beta2" apply false
}

fun Project.hasProp(namespace: String, key: String) = hasProperty("$namespace.$key")
fun Project.prop(namespace: String, key: String) = property("$namespace.$key") as String

val versions = prop("platform", "versions").split(",")
architectury.minecraft = versions.last()

group = prop("mod", "group")
version = "${prop("mod", "version")}+mc${versions.last()}"

tasks {
    // Create a new task `collect` that copies all jars to a common output directory.
    val collect by registering(Copy::class) {
        val tasks = subprojects.filter { it.path != ":common" }.map { it.tasks.named("remapJar") }

        dependsOn(tasks)
        from(tasks)
        into(layout.buildDirectory.dir("libs"))
    }
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

