import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import proguard.gradle.ProGuardTask

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.1.0-beta3")
    }
}

plugins {
    kotlin("jvm") version "1.4.30"
    id("com.github.johnrengelman.shadow") version "6.1.0"
    application
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("com.github.UBotPlatform.KtUBotCommon:KtUBotCommon:0.6.0")
    implementation("org.fusesource.jansi:jansi:2.3.2")
    implementation("com.github.project-mirai:mirai-slf4j-bridge:a84f76ac31")
    {
        exclude("io.github.karlatemp","unsafe-accessor")
    }
    implementation("com.github.ajalt.clikt:clikt:3.1.0")

    val miraiVersion = "2.6.1"
    api("net.mamoe:mirai-core-api:$miraiVersion")
    {
        // Work around mamoe/mirai#1197
        exclude("org.jetbrains.kotlin", "kotlin-serialization")
    }
    runtimeOnly("net.mamoe:mirai-core:$miraiVersion")
    {
        // Work around mamoe/mirai#1197
        exclude("org.jetbrains.kotlin", "kotlin-serialization")
    }

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

application {
    mainClassName = "ubot.account.mirai.MiraiAccountKt"
}

tasks.withType<Jar> {
    manifest {
        attributes("Main-Class" to application.mainClassName)
    }
}

val shadowJar = tasks.getByName<ShadowJar>("shadowJar")
tasks.register<ProGuardTask>("shrinkShadowJar") {
    zipTree(shadowJar.archiveFile).matching {
        include("META-INF/proguard/*.pro")
    }.forEach {
        configuration(it)
    }
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