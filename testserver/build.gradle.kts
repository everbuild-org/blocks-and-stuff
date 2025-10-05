plugins {
    id("java")
    id("application")
    kotlin("jvm")
}

group = "org.everbuild.blocksandstuff"
version = parent!!.version

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":blocksandstuff-blocks"))
    implementation(project(":blocksandstuff-fluids"))

    implementation(libs.minestom)
    implementation(libs.tinylog.api)
    implementation(libs.tinylog.impl)
    implementation(libs.tinylog.slf4j)

    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation(kotlin("stdlib-jdk8"))
}

application {
    mainClass.set("org.everbuild.blocksandstuff.testserver.TestServer")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(25)
}