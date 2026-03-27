repositories {
    maven("https://maven.terraformersmc.com/releases/") { content { includeGroup("com.terraformersmc") } }
}

dependencies {
    implementation("net.fabricmc:fabric-loader:${rootProject.property("fabric.loader.version")!!}")
    implementation("net.fabricmc.fabric-api:fabric-api:${rootProject.property("fabric.api.version")!!}")

    compileOnly("com.terraformersmc:modmenu:${rootProject.property("modmenu.version")!!}")
}
