plugins {
    alias(libs.plugins.kotlinJvm)
}

kotlin {
    jvmToolchain(libs.versions.javaToolchain.get().toInt())
}

dependencies {
    compileOnly(libs.detektApi)

    testImplementation(libs.detektApi)
    testImplementation(libs.detektTest)
    testImplementation(libs.kotestRunnerJunit5)
    testImplementation(libs.kotestAssertionsCore)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
