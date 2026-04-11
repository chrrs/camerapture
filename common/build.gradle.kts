repositories {
    maven("https://maven.shedaniel.me/") { content { includeGroup("me.shedaniel.cloth") } }
    maven("https://api.modrinth.com/maven") { content { includeGroup("maven.modrinth") } }
    maven("https://maven.chrr.me/releases") { content { includeGroup("me.chrr.tapestry") } }
}

dependencies {
    fun tapestryModule(name: String) =
        implementation("me.chrr.tapestry:$name:${rootProject.property("tapestry.version")}")

    compileOnlyApi("me.shedaniel.cloth:cloth-config-fabric:${rootProject.property("clothconfig.version")!!}")
    compileOnly("maven.modrinth:jade:${rootProject.property("jade.version")!!}+fabric")
    compileOnly("maven.modrinth:first-person-model:${rootProject.property("firstpersonmodel.version")!!}")

    jij(implementation("dev.matrixlab.webp4j:webp4j-core:2.1.1")!!)
    jij(tapestryModule("tapestry-base")!!)
}