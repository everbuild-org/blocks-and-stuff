plugins {
    kotlin("jvm") version "2.1.10"
}

group = "org.everbuild.blocksandstuff"
version = "1.4.0-SNAPSHOT"

allprojects {
    group = rootProject.group
    version = rootProject.version
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
}

