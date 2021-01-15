import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import proguard.gradle.ProGuardTask

buildscript {
    repositories {
        jcenter()
        google()
    }
    dependencies {
        // TODO: required due to https://github.com/Guardsquare/proguard/issues/30
        classpath("com.android.tools.build:gradle:3.0.0")
        classpath("com.guardsquare:proguard-gradle:7.0.1")
    }
}

plugins {
    kotlin("jvm") version "1.4.10"
    id("com.github.johnrengelman.shadow") version "6.1.0"
    application
}

repositories {
    jcenter()
    maven("https://jitpack.io")
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.8")
    implementation("com.github.UBotPlatform:Ubot.Common.Kotlin:0.4.5")
    implementation("org.slf4j:slf4j-nop:1.7.30")
    implementation("org.fusesource.jansi:jansi:1.18")

    val miraiVersion = "2.0-RC"
    api("net.mamoe:mirai-core-api:$miraiVersion")
    runtimeOnly("net.mamoe:mirai-core:$miraiVersion")

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
    configuration("proguard.pro")
    injars(shadowJar)
    outjars("$buildDir/libs/${project.name}-all-shrunk.jar")
    val javaHome = System.getProperty("java.home")
    if (System.getProperty("java.version").startsWith("1.")) {
        // Before Java 9, the runtime classes were packaged in a single jar file.
        libraryjars("$javaHome/lib/rt.jar")
    } else {
        // As of Java 9, the runtime classes are packaged in modular jmod files.
        libraryjars(
                // filters must be specified first, as a map
                mapOf("jarfilter" to "!**.jar",
                        "filter" to "!module-info.class"),
                "$javaHome/jmods/java.base.jmod"
        )
    }
}