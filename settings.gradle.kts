pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://redirector.kotlinlang.org/maven/bootstrap")
    }
    plugins {
        kotlin("jvm") version "2.4.0"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "blocksandstuff"

include("blocksandstuff-common")
include("blocksandstuff-fluids")
include("blocksandstuff-blocks")
include("testserver")
