plugins {
    kotlin("jvm")
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":blocksandstuff-common"))

    compileOnly(libs.minestom)
    compileOnly(libs.slf4j.api)
    compileOnly(libs.jetbrains.annotations)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.minestom)
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation(kotlin("stdlib-jdk8"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(25)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("Blocks and Stuff - Redstone")
                description.set("Event-driven redstone engine for Minestom")
                url.set("https://github.com/everbuild/blocks-and-stuff")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("everbuild")
                        name.set("Everbuild Team")
                        email.set("contact@everbuild.org")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/everbuild/blocks-and-stuff.git")
                    developerConnection.set("scm:git:ssh://github.com/everbuild/blocks-and-stuff.git")
                    url.set("https://github.com/everbuild/blocks-and-stuff")
                }
            }
        }
    }

    repositories {
        maven {
            name = "EverbuildMaven"
            url = uri("https://mvn.everbuild.org/public")
            credentials {
                username = project.findProperty("everbuildMavenUsername") as String? ?: ""
                password = project.findProperty("everbuildMavenPassword") as String? ?: ""
            }
        }
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}
