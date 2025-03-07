plugins {
    id("java")
}

group = "org.everbuild.blocksandstuff"
version = parent!!.version

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(libs.minestom)

    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}