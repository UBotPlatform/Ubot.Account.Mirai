plugins {
    kotlin("jvm") version "1.4.10"
    id("com.github.johnrengelman.shadow") version "6.0.0"
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
    implementation("io.ktor:ktor-client-cio:1.4.1")
    implementation("com.github.UBotPlatform:Ubot.Common.Kotlin:0.4.5") {
        exclude("org.jetbrains.kotlinx", "kotlinx-serialization-json")
    }
    implementation("net.mamoe:mirai-core-qqandroid:1.3.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

application {
    mainClassName = "ubot.account.mirai.MiraiAccountKt"
}

tasks.withType<Jar> {
    manifest {
        attributes("Main-Class" to "ubot.account.mirai.MiraiAccountKt")
    }
}