plugins {
    alias(libs.plugins.kotlinJvm)
}

dependencies {
    compileOnly(libs.detektApi)
    compileOnly(libs.detektKotlinAnalysisApi)

    testImplementation(libs.detektApi)
    testImplementation(libs.detektKotlinAnalysisApi)
    testImplementation(libs.detektTest)
    testImplementation(libs.kotestRunnerJunit5)
    testImplementation(libs.kotestAssertionsCore)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
