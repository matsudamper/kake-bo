package net.matsudamper.money.backend.feature.objectstorage

import java.net.URI
import java.time.Duration
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
    public fun assumeWithWebIdentity(
        userId: UserId,
        durationSeconds: Int = 3600,
    ): AwsSessionCredentials {
        // STSのAssumeRoleWithWebIdentityはdurationSecondsが900〜43200の範囲外はエラーになる
        val clampedDuration = durationSeconds.coerceIn(900, 43200)
        val jwt = jwtIssuer.issueWebIdentityToken(
            subject = userId.value.toString(),
            name = userId.value.toString(),
            audience = config.audience,
            ttl = Duration.ofMinutes(5),
        )

        StsClient.builder().apply {
            if (config.stsEndpoint.isNotBlank()) {
                endpointOverride(URI(config.stsEndpoint))
            }
            region(Region.of(config.region))
            credentialsProvider(AnonymousCredentialsProvider.create())
        }.build().use { stsClient ->
            val request = AssumeRoleWithWebIdentityRequest.builder()
                .roleArn(config.roleArn)
                .roleSessionName(config.roleSessionName)
                .webIdentityToken(jwt)
                .durationSeconds(clampedDuration)
                .build()

            val response = stsClient.assumeRoleWithWebIdentity(request)
            val credentials = response.credentials()

            return AwsSessionCredentials.create(
                credentials.accessKeyId(),
                credentials.secretAccessKey(),
                credentials.sessionToken(),
            )
        }
    }
}
