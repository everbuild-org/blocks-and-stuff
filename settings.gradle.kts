pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://redirector.kotlinlang.org/maven/bootstrap")
    }
    plugins {
        kotlin("jvm") version "2.3.0-dev-9673"
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
