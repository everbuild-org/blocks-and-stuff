plugins {
    id("java")
    kotlin("jvm")
}

group = "org.everbuild.blocksandstuff"
version = parent!!.version

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":blocksandstuff-common"))

    compileOnly(libs.minestom)

    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation(kotlin("stdlib-jdk8"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}