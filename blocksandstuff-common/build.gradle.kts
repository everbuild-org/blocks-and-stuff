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
    compileOnly(libs.minestom)
    implementation(libs.gson)

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