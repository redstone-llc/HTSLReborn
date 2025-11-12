plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("plugin.serialization").version("2.2.10")
}

group = "llc.redstone"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))


    implementation("net.benwoodworth.knbt:knbt:0.11.8")

    implementation(tegralLibs.niwen.lexer)
    implementation(tegralLibs.niwen.parser)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}