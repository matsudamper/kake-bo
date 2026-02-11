import org.jetbrains.kotlin.gradle.dsl.JvmTarget
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

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_24)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_24
    targetCompatibility = JavaVersion.VERSION_24
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin")
    compileOnly("org.jetbrains.compose:compose-gradle-plugin:1.10.1")
    compileOnly("org.gradle.kotlin:gradle-kotlin-dsl-plugins:6.5.2")
    implementation("com.android.application:com.android.application.gradle.plugin:8.13.2")
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
