package net.matsudamper.money.backend.feature.objectstorage

import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.time.Duration
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.matsudamper.money.backend.app.interfaces.ImageResizerGateway
import net.matsudamper.money.backend.feature.oidc.JwtIssuer
import net.matsudamper.money.element.UserId

/**
 * s3-image-resizer の `POST /v1/resize` を呼ぶ。
 * 認証には STS 用と同じ WebIdentity JWT を使い、resizer 側がその JWT で AssumeRoleWithWebIdentity する。
 */
class S3ImageResizerGateway(
    private val jwtIssuer: JwtIssuer,
    private val config: ObjectStorageConfig,
    private val requestTimeout: Duration = Duration.ofSeconds(90),
) : ImageResizerGateway {
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * ImageStorageGateway の put / delete と同じく例外は結果型に畳み込み、呼び出し元がロールバックできるようにする
     */
    override fun createThumbnail(request: ImageResizerGateway.Request): ImageResizerGateway.Result {
        val httpRequest = try {
            buildRequest(
                userId = request.userId,
                sourceKey = S3ImageKey.of(request.sourceRelativePath),
                destinationKey = S3ImageKey.of(request.thumbnailRelativePath),
            )
        } catch (e: Exception) {
            return ImageResizerGateway.Result.Unavailable(e)
        }

        val response = try {
            httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
        } catch (e: HttpTimeoutException) {
            return ImageResizerGateway.Result.Unavailable(e)
        } catch (e: IOException) {
            return ImageResizerGateway.Result.Unavailable(e)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            return ImageResizerGateway.Result.Unavailable(e)
        }

        // ConvertFailed は画像自体が原因のものに限る。認証や設定の不備（401 / 403 / 404）や 429 は再試行できる障害として扱う
        return when (response.statusCode()) {
            204 -> ImageResizerGateway.Result.Success
            415 -> ImageResizerGateway.Result.UnsupportedFormat
            413 -> ImageResizerGateway.Result.TooLarge
            400, 422 -> ImageResizerGateway.Result.ConvertFailed(errorMessage(response))
            else -> ImageResizerGateway.Result.Unavailable(IOException("image resizer returned ${response.statusCode()}: ${errorMessage(response)}"))
        }
    }

    private fun buildRequest(
        userId: UserId,
        sourceKey: String,
        destinationKey: String,
    ): HttpRequest {
        val token = jwtIssuer.issueWebIdentityToken(
            subject = userId.value.toString(),
            name = userId.value.toString(),
            audience = config.audience,
            ttl = Duration.ofMinutes(5),
        )
        val body = ResizeRequest(
            source = ObjectRef(bucket = config.bucket, key = sourceKey),
            destination = ObjectRef(bucket = config.bucket, key = destinationKey),
            resize = ResizeRequest.Resize(shortSide = THUMBNAIL_SHORT_SIDE, maxLongSide = THUMBNAIL_MAX_LONG_SIDE),
            output = ResizeRequest.Output(format = "webp", quality = THUMBNAIL_QUALITY),
        )
        return HttpRequest.newBuilder(URI(config.imageResizerEndpoint.trimEnd('/') + "/v1/resize"))
            .timeout(requestTimeout)
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.encodeToString(ResizeRequest.serializer(), body)))
            .build()
    }

    private fun errorMessage(response: HttpResponse<String>): String {
        return runCatching { json.decodeFromString(ErrorResponse.serializer(), response.body()) }
            .getOrNull()
            ?.let { "${it.error}: ${it.message}" }
            ?: response.body().take(200)
    }

    @Serializable
    private data class ObjectRef(val bucket: String, val key: String)

    @Serializable
    private data class ResizeRequest(
        val source: ObjectRef,
        val destination: ObjectRef,
        val resize: Resize,
        val output: Output,
    ) {
        @Serializable
        data class Resize(val shortSide: Int, val maxLongSide: Int)

        @Serializable
        data class Output(val format: String, val quality: Int)
    }

    @Serializable
    private data class ErrorResponse(val error: String = "", val message: String = "")

    private companion object {
        /** 一覧は最大 120dp の正方形に Crop 表示するため、3x でも足りるよう短辺 384px */
        const val THUMBNAIL_SHORT_SIDE = 384
        const val THUMBNAIL_MAX_LONG_SIDE = 1536
        const val THUMBNAIL_QUALITY = 75
    }
}
