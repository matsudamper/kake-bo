plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("net.matsudamper.money.buildlogic.androidLibrary")
    alias(libs.plugins.ksp)
}

kotlin {
    js(IR) {
        browser()
    }
    androidTarget()
    sourceSets {
        jvmToolchain(libs.versions.javaToolchain.get().toInt())
        val commonMain by getting {
            dependencies {
                implementation(projects.shared)
                implementation(libs.kotlin.coroutines.core)
                implementation(libs.room3Runtime)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(projects.frontend.common.base)
                implementation(projects.frontend.common.graphql)
                implementation(libs.sqliteWeb)
                implementation(npm("@androidx/sqlite-web-worker", "$projectDir/sqlite-web-worker"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.workRuntimeKtx)
                implementation(libs.okHttp)
                implementation(libs.kotlin.serialization.json)
                implementation(projects.frontend.common.feature.localstore)
                implementation(projects.frontend.common.graphql)
                implementation(libs.sqliteFramework)
            }
        }
    }
    explicitApi()
    compilerOptions {
        // expect/actual クラスは Beta 機能のため警告を抑制する
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

dependencies {
    add("kspAndroid", libs.room3Compiler)
    add("kspJs", libs.room3Compiler)
}

// Room 3.0 KMP の KSP は commonMain メタデータコンパイルにも出力を生成するが、
// そのコードは platform-specific API を参照するためメタデータコンパイルには不要。
// KSP タスクを無効化し、設定フェーズで既存の生成済みファイルを削除する。
// KGP は FileTree によるレイジー解決を使うため、設定フェーズでファイルを削除すれば
// 実行フェーズのコンパイル時に参照されない。
afterEvaluate {
    tasks.findByName("kspCommonMainKotlinMetadata")?.enabled = false
    val kspMetadataDir = layout.buildDirectory.dir("generated/ksp/metadata/commonMain/kotlin").get().asFile
    if (kspMetadataDir.exists()) kspMetadataDir.deleteRecursively()
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

android {
    namespace = "net.matsudamper.money.frontend.common.feature.uploader"
}
