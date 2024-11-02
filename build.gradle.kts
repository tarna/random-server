plugins {
    application
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "1.5.21"
    id("com.gradleup.shadow") version "8.3.4"
}

group = "dev.tarna"
version = "1.0.0"

repositories {
    mavenCentral()
}

val ktor_version: String by project

dependencies {
    implementation("net.minestom:minestom-snapshots:c148954a47")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

    // Ktor
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-client-cio-jvm:3.0.0")
}

kotlin {
    jvmToolchain(21)
}

tasks {
    shadowJar {
        archiveBaseName.set("RandomServer")
        archiveVersion.set("")
        archiveClassifier.set("")
    }
}

application {
    mainClass.set("dev.tarna.randomserver.MainKt")
}