plugins {
    alias(libs.plugins.kotlinJvm)
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
