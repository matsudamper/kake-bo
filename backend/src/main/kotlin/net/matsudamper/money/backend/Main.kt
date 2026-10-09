package net.matsudamper.money.backend

import java.io.File
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.withTimeout
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticFiles
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.conditionalheaders.ConditionalHeaders
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.forwardedheaders.ForwardedHeaders
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.contentLength
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.receiveChannel
import io.ktor.server.request.receiveText
import io.ktor.server.response.cacheControl
import io.ktor.server.response.header
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.accept
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.utils.io.readRemaining
import io.opentelemetry.instrumentation.ktor.v3_0.KtorServerTelemetry
import net.matsudamper.money.backend.base.ObjectMapper
import net.matsudamper.money.backend.base.OpenTelemetryInitializer
import net.matsudamper.money.backend.base.ServerEnv
import net.matsudamper.money.backend.base.TraceLogger
import net.matsudamper.money.backend.datasource.db.DbConnectionImpl
import net.matsudamper.money.backend.di.DiContainer
import net.matsudamper.money.backend.di.MainDiContainer
import net.matsudamper.money.backend.feature.oidc.jwks
import net.matsudamper.money.backend.feature.oidc.oidcDiscovery
import net.matsudamper.money.backend.feature.session.KtorCookieManager
import net.matsudamper.money.backend.graphql.GraphqlOperationTimeout
import net.matsudamper.money.backend.graphql.MoneyGraphQlSchema
import net.matsudamper.money.backend.image.ImageUploadConfig
import net.matsudamper.money.backend.image.getImage
import net.matsudamper.money.backend.image.postImage
import org.slf4j.event.Level

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            System.setProperty("logback.configurationFile", "logback.xml")

            OpenTelemetryInitializer.initialize()

            // Initialize
            MoneyGraphQlSchema.graphql
            val diContainer = MainDiContainer()
            if (System.getenv("CI")?.toBooleanStrictOrNull() != true) {
                runCatching { DbConnectionImpl.warmup() }
                    .onFailure { TraceLogger.impl().noticeThrowable(it, isError = true) }
                runCatching { diContainer.warmup() }
                    .onFailure { TraceLogger.impl().noticeThrowable(it, isError = true) }
            }

            val engine = embeddedServer(
                CIO,
                port = ServerEnv.port,
                module = { myApplicationModule(diContainer = diContainer) },
            )
            Runtime.getRuntime().addShutdownHook(
                Thread {
                    engine.stop(1000, 1000)
                    diContainer.close()
                },
            )
            engine.start(wait = true)
        }
    }
}

fun Application.myApplicationModule(diContainer: DiContainer) {
    install(KtorServerTelemetry) {
        setOpenTelemetry(OpenTelemetryInitializer.get())
    }
    install(ForwardedHeaders)
    install(XForwardedHeaders)
    install(Compression)
    install(ConditionalHeaders)
    install(ContentNegotiation) {
        json(
            json = ObjectMapper.kotlinxSerialization,
            contentType = ContentType.Application.Json,
        )
    }
    install(CORS) {
        allowHost(host = ServerEnv.domain!!, schemes = listOf("https"))
        allowNonSimpleContentTypes = true
    }
    install(CallLogging) {
        level = Level.INFO
        filter { true }

        format { call ->
            buildString {
                appendLine("request=${call.request.path()}")
                appendLine(
                    call.request.headers.entries().joinToString("\n") { (key, value) ->
                        "$key=$value"
                    },
                )
            }
        }
    }
    // OPFS（Origin Private File System）を Web Worker から使用するために必要なクロスオリジン分離ヘッダ
    install(DefaultHeaders) {
        header("Cross-Origin-Opener-Policy", "same-origin")
        header("Cross-Origin-Embedder-Policy", "require-corp")
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            TraceLogger.impl().noticeThrowable(cause, isError = true)
            call.respondText(
                status = HttpStatusCode.InternalServerError,
                text = HttpStatusCode.InternalServerError.description,
            )
        }
        status(HttpStatusCode.NotFound) { call, _ ->
            if (call.request.httpMethod == HttpMethod.Get) {
                call.response.cacheControl(CacheControl.NoStore(null))
                call.respondFile(File(ServerEnv.htmlPath))
            } else {
                call.respondText(
                    status = HttpStatusCode.NotFound,
                    text = HttpStatusCode.NotFound.description,
                )
            }
        }
    }

    routing {
        get("/healthz") {
            call.respondText("ok")
        }
        accept(ContentType.Application.Json) {
            post("/query") {
                call.respondText(
                    contentType = ContentType.Application.Json,
                ) {
                    // 操作のタイムアウトは本文を読むまで決まらないため、読み取りは通常の期限で打ち切り、
                    // 実行には受信からの経過時間を差し引いた残りを使って、合計を操作のタイムアウトに収める
                    val receivedAt = TimeSource.Monotonic.markNow()
                    val requestText = withTimeout(GraphqlOperationTimeout.DEFAULT_TIMEOUT) {
                        call.receiveText()
                    }
                    val handler = GraphqlHandler(
                        cookieManager = KtorCookieManager(call = call),
                        diContainer = diContainer,
                    )
                    return@respondText withTimeout(handler.resolveTimeout(requestText) - receivedAt.elapsedNow()) {
                        handler.handle(requestText = requestText)
                    }
                }
            }
            post("/api/register_mail/v1") {
                withTimeout(5.seconds) {
                    val registerMailHandler = RegisterMailHandler(
                        diContainer = diContainer,
                    )
                    val userId = registerMailHandler.authenticate(apiKey = call.request.headers["Authorization"])
                    if (userId == null) {
                        call.respondText(
                            status = HttpStatusCode.Forbidden,
                            text = HttpStatusCode.Forbidden.value.toString(),
                        )
                        return@withTimeout
                    }

                    val maxBodyBytes = ServerEnv.registerMailMaxBytes
                    val declaredContentLength = call.request.contentLength()
                    if (declaredContentLength != null && declaredContentLength > maxBodyBytes) {
                        call.respondText(
                            status = HttpStatusCode.PayloadTooLarge,
                            text = HttpStatusCode.PayloadTooLarge.value.toString(),
                        )
                        return@withTimeout
                    }
                    // Content-Lengthが無い/偽装されている場合に備え、上限+1バイトまでで読み込みを打ち切る
                    val bodyBytes = call.receiveChannel().readRemaining(maxBodyBytes + 1).readByteArray()
                    if (bodyBytes.size > maxBodyBytes) {
                        call.respondText(
                            status = HttpStatusCode.PayloadTooLarge,
                            text = HttpStatusCode.PayloadTooLarge.value.toString(),
                        )
                        return@withTimeout
                    }
                    val request = ObjectMapper.kotlinxSerialization.decodeFromString(
                        RegisterMailHandler.Request.serializer(),
                        bodyBytes.decodeToString(),
                    )
                    val result = registerMailHandler.handle(
                        request = request,
                        userId = userId,
                    )
                    when (result) {
                        RegisterMailHandler.Result.InternalServerError -> {
                            call.respondText(
                                status = HttpStatusCode.InternalServerError,
                                text = HttpStatusCode.InternalServerError.value.toString(),
                            )
                        }

                        is RegisterMailHandler.Result.Success -> {
                            call.respondText(
                                contentType = ContentType.Application.Json,
                                text = Json.encodeToString(
                                    RegisterMailHandler.Response.serializer(),
                                    result.response,
                                ),
                            )
                        }
                    }
                }
            }
        }
        postImage(
            diContainer = diContainer,
            config = ImageUploadConfig(
                maxUploadBytes = ServerEnv.imageUploadMaxBytes,
            ),
        )
        getImage(
            diContainer = diContainer,
        )

        val oidcKeyManager = diContainer.createOidcKeyManager()
        val s3 = ServerEnv.S3
        if (s3 != null && oidcKeyManager != null) {
            oidcDiscovery(issuer = s3.oidcIssuer)
            jwks(keyManager = oidcKeyManager)
        }

        get("/.well-known/assetlinks.json") {
            call.respondText(
                contentType = ContentType.Application.Json,
            ) {
                getAssetLinkJson()
            }
        }
        staticFiles(
            remotePath = "/",
            dir = File(ServerEnv.frontPath),
        ) {
            modify { file, call ->
                call.response.header(HttpHeaders.CacheControl, staticFileCacheControlOf(file.name))
            }
            contentType { file ->
                when (file.extension) {
                    "wasm" -> ContentType.Application.Wasm
                    "js" -> ContentType.Application.JavaScript
                    else -> null
                }
            }
        }
    }
}

/**
 * ファイル名に中身のハッシュが入っているものは内容が変わらないので長期キャッシュさせる。
 * index.html はハッシュ付きの名前を指す入口なので毎回取り直させる。
 */
private fun staticFileCacheControlOf(fileName: String): String {
    return when {
        fileName == "index.html" -> "no-store"
        contentHashedFileNameRegex.matches(fileName) -> "public, max-age=31536000, immutable"
        else -> "no-cache"
    }
}

private val contentHashedFileNameRegex = Regex("""^(?:.+\.)?[0-9a-f]{16,}(?:\.module)?\.(?:js|wasm)$""")

private fun getAssetLinkJson(): String {
    return """
        [
          {
            "relation": [
              "delegate_permission/common.handle_all_urls",
              "delegate_permission/common.get_login_creds"
            ],
            "target": {
              "namespace": "android_app",
              "package_name": "${ServerEnv.appPackageName}",
              "sha256_cert_fingerprints": [
                "${ServerEnv.appFingerprint}"
              ]
            }
          }
        ]
    """.trimIndent()
}
