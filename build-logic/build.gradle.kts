import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "net.matsudamper.money.buildlogic"

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = JavaVersion.VERSION_21.toString()

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin")
    compileOnly("org.jetbrains.compose:compose-gradle-plugin:1.8.2")
    compileOnly("org.gradle.kotlin:gradle-kotlin-dsl-plugins:6.1.2")
    implementation("com.android.application:com.android.application.gradle.plugin:8.10.0-alpha05")
}

gradlePlugin {
    plugins {
        register("compose") {
            id = "net.matsudamper.money.buildlogic.compose"
            implementationClass = "net.matsudamper.money.buildlogic.ComposePlugin"
        }
        register("androidLibrary") {
            id = "net.matsudamper.money.buildlogic.androidLibrary"
            implementationClass = "net.matsudamper.money.buildlogic.AndroidLibraryPlugin"
        }
    }
}
