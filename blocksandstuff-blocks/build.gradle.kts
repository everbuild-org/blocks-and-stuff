plugins {
    java
    kotlin("jvm")
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":blocksandstuff-common"))

    compileOnly(libs.minestom)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.minestom)
    testImplementation("org.junit.jupiter:junit-jupiter")
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
                name.set("Blocks and Stuff - Blocks")
                description.set("Block implementations for Minestom")
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