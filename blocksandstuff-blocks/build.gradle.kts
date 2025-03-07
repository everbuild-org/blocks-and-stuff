plugins {
    id("java")
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
    testImplementation(libs.mockito)
    testImplementation(libs.minestom)
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}