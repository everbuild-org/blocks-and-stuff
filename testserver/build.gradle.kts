plugins {
    id("java")
    id("application")
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
}

application {
    mainClass.set("org.everbuild.blocksandstuff.testserver.TestServer")
}

tasks.test {
    useJUnitPlatform()
}