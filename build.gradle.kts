plugins {
    kotlin("jvm") version "2.1.10"
}

group = "org.everbuild.blocksandstuff"
version = "1.7.0-SNAPSHOT"

allprojects {
    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
        maven(url = "https://central.sonatype.com/repository/maven-snapshots/") {
            content {
                includeModule("net.minestom", "minestom")
            }
        }
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
}

