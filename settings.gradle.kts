pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        kotlin("jvm") version "2.1.10"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "blocksandstuff"

include("blocksandstuff-common")
include("blocksandstuff-fluids")
include("blocksandstuff-blocks")
include("testserver")
