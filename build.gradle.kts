plugins {
    kotlin("jvm") version "2.3.0-Beta2"
}

group = "org.everbuild.blocksandstuff"
version = "1.8.0-SNAPSHOT"

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
        maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://redirector.kotlinlang.org/maven/bootstrap")
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
}

