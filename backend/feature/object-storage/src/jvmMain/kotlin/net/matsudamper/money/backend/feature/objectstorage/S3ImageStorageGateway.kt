package net.matsudamper.money.backend.feature.objectstorage

import java.net.URI
import java.time.Duration
import net.matsudamper.money.backend.app.interfaces.ImageStorageGateway
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest

public class S3ImageStorageGateway(
    private val stsCredentialProvider: StsCredentialProvider,
    private val config: ObjectStorageConfig,
) : ImageStorageGateway {

    override val storageType: ImageStorageGateway.StorageType = ImageStorageGateway.StorageType.S3

    override fun put(request: ImageStorageGateway.PutRequest): ImageStorageGateway.PutResult {
        val contentLength = request.contentLength
        if (contentLength == null) {
            return ImageStorageGateway.PutResult.Failure(IllegalArgumentException("contentLength is required for S3 upload"))
        }

        if (contentLength == 0L) {
            return ImageStorageGateway.PutResult.Empty
        }

        if (contentLength > request.maxBytes) {
            return ImageStorageGateway.PutResult.PayloadTooLarge
        }

        val credentials = stsCredentialProvider.assumeWithWebIdentity(userId = request.userId)

        val key = buildKey(request.userId.value.toString(), request.relativePath)

        val result = runCatching {
            S3Client.builder().apply {
                if (config.endpoint.isNotBlank()) {
                    endpointOverride(URI(config.endpoint))
                }
                region(Region.of(config.region))
                credentialsProvider(StaticCredentialsProvider.create(credentials))
                serviceConfiguration(
                    S3Configuration.builder()
                        .pathStyleAccessEnabled(config.pathStyleAccess)
                        .build(),
                )
            }.build().use { s3Client ->
                val putObjectRequest = PutObjectRequest.builder()
                    .bucket(config.bucket)
                    .key(key)
                    .contentType(request.contentType)
                    .contentLength(contentLength)
                    .build()

                s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(request.inputStream, contentLength),
                )
            }
        }

        if (result.isFailure) {
            return ImageStorageGateway.PutResult.Failure(result.exceptionOrNull() ?: IllegalStateException("Unknown failure during S3 put"))
        }

        return ImageStorageGateway.PutResult.Success(relativePath = request.relativePath)
    }

    override fun delete(request: ImageStorageGateway.DeleteRequest): ImageStorageGateway.DeleteResult {
        val credentials = stsCredentialProvider.assumeWithWebIdentity(userId = request.userId)
        val key = buildKey(request.userId.value.toString(), request.relativePath)

        return runCatching {
            S3Client.builder().apply {
                if (config.endpoint.isNotBlank()) {
                    endpointOverride(URI(config.endpoint))
                }
                region(Region.of(config.region))
                credentialsProvider(StaticCredentialsProvider.create(credentials))
                serviceConfiguration(
                    S3Configuration.builder()
                        .pathStyleAccessEnabled(config.pathStyleAccess)
                        .build(),
                )
            }.build().use { s3Client ->
                val deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(config.bucket)
                    .key(key)
                    .build()
                s3Client.deleteObject(deleteObjectRequest)
            }
        }.fold(
            onSuccess = { ImageStorageGateway.DeleteResult.Success },
            onFailure = { ImageStorageGateway.DeleteResult.Failure(it) },
        )
    }

    override fun buildDisplayUrl(request: ImageStorageGateway.BuildUrlRequest): String {
        val credentials = stsCredentialProvider.assumeWithWebIdentity(userId = request.userId, durationSeconds = 7200)
        val key = buildKey(request.userId.value.toString(), request.relativePath)

        S3Presigner.builder().apply {
            if (config.endpoint.isNotBlank()) {
                endpointOverride(URI(config.endpoint))
            }
            region(Region.of(config.region))
            credentialsProvider(StaticCredentialsProvider.create(credentials))
            serviceConfiguration(
                S3Configuration.builder()
                    .pathStyleAccessEnabled(config.pathStyleAccess)
                    .build(),
            )
        }.build().use { presigner ->
            val getObjectRequest = GetObjectRequest.builder()
                .bucket(config.bucket)
                .key(key)
                .build()

            val presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(getObjectRequest)
                .build()

            val presignedGetObjectResponse = presigner.presignGetObject(presignRequest)
            return presignedGetObjectResponse.url().toString()
        }
    }

    private fun buildKey(userIdStr: String, relativePath: String): String {
        return "img/$userIdStr/$relativePath"
    }
}
