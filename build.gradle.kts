plugins {
    kotlin("jvm") version "2.1.10" apply false
}

group = "org.everbuild.blocksandstuff"
version = "1.0.0-SNAPSHOT"

allprojects {
    group = rootProject.group
    version = rootProject.version
}