import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import proguard.gradle.ProGuardTask
import java.io.BufferedReader
import java.io.FileReader

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.3.0-beta1")
    }
}

plugins {
    kotlin("jvm") version "1.7.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    application
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("com.github.UBotPlatform.KtUBotCommon:KtUBotCommon:0.9.0")
    implementation("com.github.ajalt.clikt:clikt:3.5.0")

    implementation(platform("org.apache.logging.log4j:log4j-bom:2.18.0"))
    implementation("org.apache.logging.log4j:log4j-api")
    implementation("org.apache.logging.log4j:log4j-core")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl")

    implementation(platform("io.ktor:ktor-bom:2.1.1"))
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-okhttp")

    implementation(platform("net.mamoe:mirai-bom:2.13.0-M1"))
    implementation("net.mamoe:mirai-core-api")
    runtimeOnly("net.mamoe:mirai-core")
    implementation("net.mamoe:mirai-logging-log4j2")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

kotlin.sourceSets.all {
    languageSettings.optIn("kotlin.RequiresOptIn")
}

application {
    mainClass.set("ubot.account.mirai.MiraiAccountKt")
}

tasks.withType<Jar> {
    manifest {
        attributes("Main-Class" to application.mainClass)
    }
}

val shadowJar = tasks.getByName<ShadowJar>("shadowJar")
tasks.register<ProGuardTask>("shrinkShadowJar") {
    dependsOn(shadowJar)
    doFirst {
        zipTree(shadowJar.archiveFile).matching {
            include("META-INF/services/*")
        }.forEach { file ->
            keepnames("class ${file.name}")
            val provider = BufferedReader(FileReader(file)).use {
                it.lines().map { provider ->
                    val length = provider.indexOf('#')
                    if (length == -1) {
                        provider
                    } else {
                        provider.substring(0, length)
                    }
                }.map { provider ->
                    provider.trim()
                }.filter { provider ->
                    provider.isNotEmpty()
                }.forEach { provider ->
                    keep("class $provider")
                }
            }
        }
    }
    zipTree(shadowJar.archiveFile).matching {
        include("META-INF/proguard/*.pro")
        include("WEB-INF/proguard/*.pro")
    }.let(::configuration)
    configuration("proguard-rules.pro")
    injars(shadowJar)
    outjars("$buildDir/libs/${project.name}-all-shrunk.jar")
    val javaHome = System.getProperty("java.home")
    if (System.getProperty("java.version").startsWith("1.")) {
        // Before Java 9, the runtime classes were packaged in a single jar file.
        libraryjars("$javaHome/lib/rt.jar")
    } else {
        // As of Java 9, the runtime classes are packaged in modular jmod files.
        File(javaHome, "jmods").listFiles().orEmpty().forEach { file ->
            libraryjars(
                mapOf(
                    "jarfilter" to "!**.jar",
                    "filter" to "!module-info.class"
                ), file
            )
        }
    }
}