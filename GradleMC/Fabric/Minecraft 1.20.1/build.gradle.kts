import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	id("fabric-loom")
    `maven-publish`
    id("org.jetbrains.kotlin.jvm") version "2.4.10"
}

repositories {
    mavenCentral()
}

loom {
    splitEnvironmentSourceSets()
    mods {
        register("gradlemc") {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets.getByName("client"))
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${providers.gradleProperty("fabric_kotlin_version").get()}")
    testImplementation(kotlin("test"))
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") { expand("version" to project.version) }
}

tasks.withType<JavaCompile>().configureEach { options.release.set(17) }
tasks.test { useJUnitPlatform() }

kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.jar { archiveBaseName.set("gradlemc") }
tasks.named<Jar>("remapJar") {
    archiveBaseName.set("gradlemc")
    archiveClassifier.set("fabric-1.20.1")
}

publishing {
    publications { register<MavenPublication>("mavenJava") { from(components["java"]) } }
}
