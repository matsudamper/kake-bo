package net.matsudamper.money.backend.feature.oidc

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID

public class JwtIssuer(
    private val keyManager: OidcKeyManager,
    private val issuer: String,
) {
    public fun issueWebIdentityToken(
        subject: String,
        name: String,
        audience: String,
        ttl: Duration,
        customClaims: Map<String, Any> = emptyMap(),
    ): String {
        val header = JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID(keyManager.getKid())
            .build()

        val now = Instant.now()
        val claimsBuilder = JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject(subject)
            .audience(audience)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plus(ttl)))
            .jwtID(UUID.randomUUID().toString())
            .claim("name", name)

        customClaims.forEach { (k, v) ->
            claimsBuilder.claim(k, v)
        }

        val signedJWT = SignedJWT(header, claimsBuilder.build())
        signedJWT.sign(RSASSASigner(keyManager.getPrivateKey()))

        return signedJWT.serialize()
    }
}
