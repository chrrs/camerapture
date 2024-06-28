plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.20.4" /* [SC] DO NOT EDIT */

// Read the versions from CHISELED_VERSIONS, and only build / publish those versions.
val chiseledVersions = providers.environmentVariable("CHISELED_VERSIONS")
    .orNull?.split(",")
val chiseledProjects = stonecutter.versions.filter { chiseledVersions == null || it.version in chiseledVersions }

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
