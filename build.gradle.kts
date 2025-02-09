@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.utils.`is`


plugins {
    java
    kotlin("jvm") version "2.1.0" // TODO: (project.properties["kotlinVersion"] as String)
    kotlin("plugin.serialization") version "2.1.0"
    id("fabric-loom") version "1.9-SNAPSHOT"
    `maven-publish`
}

version = project.properties["mod_version"]!!
group = project.properties["maven_group"]!!
val javaVersion: Int = (project.properties["java_version"]!! as String).toInt()

base {
    archivesName = project.properties["archives_base_name"] as String
}

repositories {
    maven("https://maven.parchmentmc.org") { name = "Parchment" }
    mavenCentral()
}

dependencies {
    // Minecraft & Mappings
    minecraft("com.mojang:minecraft:${properties["minecraft_version"]}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-1.20.1:${properties["parchmentmc_version"]}@zip")
    })

    // Fabric
    modImplementation("net.fabricmc:fabric-loader:${properties["loader_version"]}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${properties["fabric_version"]}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${project.properties["flk_version"]}+kotlin.${project.properties["kotlin_version"]}")

    // Other dependencies go here
}

tasks.withType<ProcessResources> {
    inputs.properties(project.properties.filter { (_, v) -> v is String })
    filesMatching("fabric.mod.json") {
        expand(project.properties)
    }
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
}

kotlin {
    explicitApiWarning()
    jvmToolchain(javaVersion)
}

publishing {
    publications {
        create<MavenPublication>("mod") {
            groupId = properties["maven_group"] as String
            artifactId = properties["archives_base_name"] as String

            from(components["java"])
        }
    }

    repositories {
        val username = "sapphoCompanyUsername".let { System.getenv(it) ?: findProperty(it) }?.toString()
        val password = "sapphoCompanyPassword".let { System.getenv(it) ?: findProperty(it) }?.toString()
        if (username != null && password != null) {
            maven("https://maven.is-immensely.gay/${properties["maven_category"]}") {
                name = "sapphoCompany"
                credentials {
                    this.username = username
                    this.password = password
                }
            }
        } else {
            println("Sappho Company credentials not present.")
        }
    }
}