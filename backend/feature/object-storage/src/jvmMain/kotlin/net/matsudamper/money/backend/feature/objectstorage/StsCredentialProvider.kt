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
        durationSeconds: Int = 7200,
    ): AwsSessionCredentials {
        val jwt = jwtIssuer.issueWebIdentityToken(
            subject = userId.value.toString(),
            name = userId.value.toString(),
            audience = config.audience,
            ttl = Duration.ofMinutes(5)
        )

        StsClient.builder().apply {
            if (config.endpoint.isNotBlank()) {
                endpointOverride(URI(config.endpoint))
            }
            region(Region.of(config.region))
            credentialsProvider(AnonymousCredentialsProvider.create())
        }.build().use { stsClient ->
            val request = AssumeRoleWithWebIdentityRequest.builder()
                .roleArn(config.roleArn)
                .roleSessionName(config.roleSessionName)
                .webIdentityToken(jwt)
                .durationSeconds(durationSeconds)
                .build()

            val response = stsClient.assumeRoleWithWebIdentity(request)
            val credentials = response.credentials()

            return AwsSessionCredentials.create(
                credentials.accessKeyId(),
                credentials.secretAccessKey(),
                credentials.sessionToken()
            )
        }
    }
}
