plugins {
    alias(libs.plugins.kotlinAndroid)
    id("net.matsudamper.money.buildlogic.androidLibrary")
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(projects.shared)
    implementation(projects.frontend.common.base)
    implementation(projects.frontend.common.graphql)
    implementation(projects.frontend.common.navigation)
    implementation(projects.frontend.common.viewmodel)

    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kotlin.datetime)
    implementation(libs.koinCore)
    implementation(libs.androidxCoreKtx)
    implementation(libs.roomRuntime)
    implementation(libs.roomKtx)
    ksp(libs.roomCompiler)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotestRunnerJunit5)
}

android {
    namespace = "net.matsudamper.money.frontend.android.feature.notificationusage"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
