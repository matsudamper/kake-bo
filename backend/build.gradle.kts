import java.nio.file.LinkOption
import kotlin.io.path.createLinkPointingTo
import kotlin.io.path.deleteExisting
import kotlin.io.path.fileSize
import kotlin.io.path.isRegularFile

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.graalvmNative)
    application
}

base.archivesName.set("money")
group = "net.matsudamper.money.backend"

val graalVmLanguageVersion = JavaLanguageVersion.of(24)
val graalVmLauncher = javaToolchains.launcherFor {
    languageVersion = graalVmLanguageVersion
    vendor = JvmVendorSpec.GRAAL_VM
}

val nativeBuildArgs = listOf(
    "--no-fallback",
    "-H:+ReportExceptionStackTraces",
    "--enable-url-protocols=http,https",
    "--initialize-at-run-time=com.zaxxer.hikari",
    "--initialize-at-run-time=org.mariadb.jdbc",
    "--initialize-at-run-time=ch.qos.logback.core.rolling",
    "--initialize-at-run-time=graphql.kickstart.tools",
    // jOOQ は全クラスをランタイム初期化に指定する。
    // DefaultDataType の静的ブロックが SQLDataType の初期化をトリガーし、
    // その中で ArrayDataType.getArrayType() が呼ばれる。
    // GraalVM native-image では Class.arrayType() が image に含まれていない型に対して
    // null を返すため、reflect-config.json に jOOQ カスタム型の配列クラスを登録している。
    "--initialize-at-run-time=org.jooq",
    "-H:+AddAllCharsets",
    "--initialize-at-run-time=io.opentelemetry.sdk",
    "--initialize-at-run-time=io.opentelemetry.exporter",
    "--initialize-at-run-time=io.opentelemetry.instrumentation",
    "--initialize-at-run-time=io.opentelemetry.extension",
    // Jackson: コアクラスはビルド時初期化、リフレクション依存部分はランタイム初期化
    "--initialize-at-build-time=com.fasterxml.jackson.core",
    "--initialize-at-build-time=com.fasterxml.jackson.databind",
    "--initialize-at-build-time=com.fasterxml.jackson.annotation",
    "--initialize-at-build-time=com.fasterxml.jackson.datatype.jsr310",
    "--initialize-at-build-time=com.fasterxml.jackson.module.kotlin",
    // KotlinModule が参照する Kotlin 標準ライブラリのクラス群をビルド時初期化
    // EnumEntriesList → AbstractList$Companion と連鎖するため kotlin パッケージ全体を指定
    "--initialize-at-build-time=kotlin",
)

java {
    toolchain {
        languageVersion = graalVmLanguageVersion
        vendor = JvmVendorSpec.GRAAL_VM
    }
}

kotlin {
    jvmToolchain {
        languageVersion = graalVmLanguageVersion
        vendor = JvmVendorSpec.GRAAL_VM
    }
}

application {
    mainClass.set("net.matsudamper.money.backend.Main")
    applicationName = "backend"
}

dependencies {
    implementation(project(":backend:di"))
    implementation(projects.backend.datasource.db)

    implementation(projects.shared)
    implementation(projects.backend.base)
    implementation(projects.backend.graphql)
    implementation(projects.backend.app)
    implementation(projects.backend.app.interfaces)
    implementation(projects.backend.feature.image)
    implementation(projects.backend.feature.session)

    implementation(kotlin("stdlib"))
    implementation(libs.jackson.jsr310)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.logback.classic)
    implementation(libs.logstashLogbackEncoder)

    implementation(libs.ktorServerCore)
    implementation(libs.ktorServerEngine)
    implementation(libs.ktorClientCore)
    implementation(libs.ktorClientCio)
    implementation(libs.ktorServerStatusPages)
    implementation(libs.ktorServerCors)
    implementation(libs.ktorServerDefaultHeaders)
    implementation(libs.ktorServerFowardedHeader)
    implementation(libs.ktorSerializationJson)
    implementation(libs.ktorServerContentNegotiation)
    implementation(libs.ktorServerCallLogging)
    implementation(libs.ktorServerCompression)
    implementation(libs.ktorServerConditionalHeaders)

    implementation(libs.opentelemetryKtor3)
    implementation(libs.opentelemetryKotlinExtension)

    compileOnly("org.graalvm.sdk:nativeimage:24.2.2")

    testImplementation(kotlin("test"))
    testImplementation(libs.kotestRunnerJunit5)
    testImplementation("io.mockk:mockk:1.14.9")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

graalvmNative {
    binaries {
        named("main") {
            mainClass.set("net.matsudamper.money.backend.Main")
            imageName.set("backend")
            buildArgs.addAll(nativeBuildArgs)
            buildArgs.add("--features=net.matsudamper.money.backend.graalvm.GraphqlReflectionFeature")
            javaLauncher.set(graalVmLauncher)
        }
    }
    toolchainDetection.set(false)
}

// Workaround: https://github.com/gradle/gradle/issues/28583
// nativeCompile / nativeTestCompile の前に壊れたシンボリックリンクを修復する
tasks.matching { it.name == "nativeCompile" || it.name == "nativeTestCompile" }.configureEach {
    doFirst {
        val binPath = graalVmLauncher.get().executablePath.asFile.toPath().parent
        val svmBinPath = binPath.resolve("../lib/svm/bin")
        fixSymlink(binPath.resolve("native-image"), svmBinPath.resolve("native-image"))
    }
}

// Workaround: https://github.com/gradle/gradle/issues/28583
// Gradle のコピー処理で GraalVM JDK 内のシンボリックリンクが空ファイルに化ける問題を修正する。
// 空ファイルになった native-image を削除し、実体へのハードリンクとして再作成する。
fun fixSymlink(target: java.nio.file.Path, expectedSrc: java.nio.file.Path) {
    if (!expectedSrc.isRegularFile(LinkOption.NOFOLLOW_LINKS)) {
        logger.info("fixSymlink: expected is not regular, skip (expected: {})", expectedSrc)
        return
    }
    if (!target.isRegularFile(LinkOption.NOFOLLOW_LINKS) || target.fileSize() > 0) {
        logger.info("fixSymlink: target is not regular or the file size > 0, skip (target: {})", target)
        return
    }
    logger.warn("fixSymlink: {} -> {}", target, expectedSrc)
    target.deleteExisting()
    target.createLinkPointingTo(expectedSrc)
}
