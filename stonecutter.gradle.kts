plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21" /* [SC] DO NOT EDIT */

// Read the versions from CHISELED_VERSIONS, and only build / publish those versions.
// If it's blank, we build / publish all available versions.
val chiseledVersions = providers.environmentVariable("CHISELED_VERSIONS")
    .orNull?.ifBlank { null }?.split(",")
val chiseledProjects = stonecutter.versions
    .filter { chiseledVersions?.contains(it.version) ?: true }

stonecutter registerChiseled tasks.register("chiseledBuild", stonecutter.chiseled) {
    versions.set(chiseledProjects)
    group = "project"
    ofTask("build")
}

stonecutter registerChiseled tasks.register("chiseledPublish", stonecutter.chiseled) {
    versions.set(chiseledProjects)
    group = "project"
    ofTask("publishMods")
}
