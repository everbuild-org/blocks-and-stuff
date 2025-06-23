plugins {
    kotlin("jvm") version "2.1.10"
    id("org.jetbrains.dokka") version "2.0.0"
}

group = "org.everbuild.blocksandstuff"
version = "1.3.0-SNAPSHOT"

allprojects {
    group = rootProject.group
    version = rootProject.version
}

dependencies {
    dokka(project(":blocksandstuff-blocks"))
    dokka(project(":blocksandstuff-fluids"))
    dokka(project(":blocksandstuff-common"))
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.dokka")
}

