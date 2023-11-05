import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

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
compileKotlin.kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

val rootProperties = Properties().also {
    it.load(file("../gradle.properties").inputStream())
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin")
    compileOnly("org.jetbrains.compose:compose-gradle-plugin:${rootProperties["compose.version"] as kotlin.String}")
    compileOnly("org.gradle.kotlin:gradle-kotlin-dsl-plugins:4.2.1")
}

gradlePlugin {
    plugins {
        register("compose") {
            id = "net.matsudamper.money.buildlogic.compose"
            implementationClass = "net.matsudamper.money.buildlogic.ComposePlugin"
        }
    }
}
