import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt) apply false
}

dependencies {
}

val detektExcludedProjectPaths = setOf(
    ":backend:datasource:db:schema",
    ":backend:graphql",
)

subprojects {
    if (path.startsWith(":backend") && path !in detektExcludedProjectPaths) {
        apply(plugin = "dev.detekt")

        configure<dev.detekt.gradle.extensions.DetektExtension> {
            disableDefaultRuleSets.set(true)
            config.setFrom(rootProject.file("config/detekt/detekt.yml"))
            source.setFrom("src")
        }

        dependencies {
            add("detektPlugins", project(":detekt-rules"))
        }

        // RequiresAnalysisApi のルールは型解決付きタスクでしか走らない。
        // JVM は detektMain / detektTest、KMP は detektMainJvm / detektTestJvm。
        tasks.named("detekt").configure {
            dependsOn(
                tasks.matching {
                    it.name == "detektMain" ||
                        it.name == "detektTest" ||
                        it.name == "detektMainJvm" ||
                        it.name == "detektTestJvm"
                },
            )
        }
    }

    plugins.withType<JavaBasePlugin> {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(libs.versions.javaToolchain.get().toInt()))
                when {
                    path.startsWith(":backend") -> vendor.set(JvmVendorSpec.GRAAL_VM)
                    // GraalVM の jlink は jdk.internal.vm.ci を要求し、AGP の Android 用 JDK イメージ生成に失敗する
                    path.startsWith(":frontend") -> vendor.set(JvmVendorSpec.ADOPTIUM)
                }
            }
        }
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.all {
                languageSettings.optIn("kotlin.time.ExperimentalTime")
            }
        }
    }
}

allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        filter {
            verbose.set(true)
            val currentProjectPath = project.path
            exclude { currentProjectPath == ":backend:datasource:db:schema" }
            exclude { currentProjectPath == ":backend:graphql" }
            exclude { currentProjectPath == ":frontend:common:graphql:schema" }
            exclude { it.file.path.contains("generated") }
        }
    }

    tasks.withType<Test>().configureEach {
        testLogging {
            events(TestLogEvent.FAILED)
            exceptionFormat = TestExceptionFormat.FULL
            showStackTraces = true
            showCauses = true
        }
    }
}
