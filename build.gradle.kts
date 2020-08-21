plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.72"
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
    implementation("com.github.UBotPlatform:Ubot.Common.Kotlin:v0.2.0")
    implementation("net.mamoe:mirai-core-qqandroid:1.2.1")
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