plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.shadow)
    application
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events(
            org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
        )
    }
}

dependencies {
    implementation(libs.kotlinxCli)
    implementation(libs.kotlinxSerialization)
}

application {
    mainClass = "jp.juggler.cursorGrid.MainKt"
}

tasks.register<Copy>("copyFatJar") {
    group = "shadow"
    dependsOn(tasks.named("shadowJar"))
    from(tasks.named("shadowJar").flatMap { (it as com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar).archiveFile })
    into(rootProject.layout.projectDirectory)
    rename { "cursorGrid.jar" }
}
