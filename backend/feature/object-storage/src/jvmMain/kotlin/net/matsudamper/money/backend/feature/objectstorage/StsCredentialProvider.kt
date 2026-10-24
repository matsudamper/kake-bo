package net.matsudamper.money.backend.feature.objectstorage

import java.net.URI
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import net.matsudamper.money.backend.feature.oidc.JwtIssuer
import net.matsudamper.money.element.UserId
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sts.StsClient
import software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest

public class StsCredentialProvider(
    private val jwtIssuer: JwtIssuer,
    private val config: ObjectStorageConfig,
) {
    private companion object {
        // ロールの MaxSessionDuration の既定値が1時間のため、それを超えない値にする
        const val CREDENTIAL_DURATION_SECONDS = 3600
    }

    private val stsClient: StsClient by lazy {
        StsClient.builder().apply {
            if (config.stsEndpoint.isNotBlank()) {
                endpointOverride(URI(config.stsEndpoint))
            }
            region(Region.of(config.region))
            credentialsProvider(AnonymousCredentialsProvider.create())
        }.build()
    }

    private val credentialsCache = ConcurrentHashMap<UserId, ExpiringCredentials>()

    /**
     * @param minimumRemaining 返すクレデンシャルに最低限残っている必要がある有効期間
     */
    public fun assumeWithWebIdentity(
        userId: UserId,
        minimumRemaining: Duration,
    ): AwsSessionCredentials {
        return credentialsCache.compute(userId) { _, cached ->
            if (cached != null && cached.hasRemaining(minimumRemaining)) {
                cached
            } else {
                assumeRole(userId)
            }
        }!!.credentials
    }

    private fun assumeRole(userId: UserId): ExpiringCredentials {
        val jwt = jwtIssuer.issueWebIdentityToken(
            subject = userId.value.toString(),
            name = userId.value.toString(),
            audience = config.audience,
            ttl = Duration.ofMinutes(5),
        )
        val request = AssumeRoleWithWebIdentityRequest.builder()
            .roleArn(config.roleArn)
            .roleSessionName(config.roleSessionName)
            .webIdentityToken(jwt)
            .durationSeconds(CREDENTIAL_DURATION_SECONDS)
            .build()

        val credentials = stsClient.assumeRoleWithWebIdentity(request).credentials()
        return ExpiringCredentials(
            credentials = AwsSessionCredentials.create(
                credentials.accessKeyId(),
                credentials.secretAccessKey(),
                credentials.sessionToken(),
            ),
            expiration = credentials.expiration(),
        )
    }

    private class ExpiringCredentials(
        val credentials: AwsSessionCredentials,
        val expiration: Instant,
    ) {
        fun hasRemaining(duration: Duration): Boolean {
            return Instant.now().plus(duration).isBefore(expiration)
        }
    }
}
