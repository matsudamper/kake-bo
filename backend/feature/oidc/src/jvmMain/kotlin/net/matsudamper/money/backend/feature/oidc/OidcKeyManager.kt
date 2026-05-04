package net.matsudamper.money.backend.feature.oidc

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import java.security.PrivateKey

public class OidcKeyManager(jwkJson: String) {
    private val rsaKey: RSAKey = RSAKey.parse(jwkJson)

    public fun publicJwkSet(): String {
        return JWKSet(rsaKey.toPublicJWK()).toString()
    }

    public fun getKid(): String? = rsaKey.keyID

    public fun getPrivateKey(): PrivateKey = rsaKey.toPrivateKey()
}
