package net.matsudamper.money.backend.lib

import java.util.Base64
import com.yubico.webauthn.data.AttestationObject

/**
 * @see https://github.com/Yubico/java-webauthn-server/blob/main/webauthn-server-core/src/main/java/com/yubico/webauthn/TpmAttestationStatementVerifier.java
 */
class FIDOModel {
    fun verify(
        base64AttestationObject: String,
        base64ClientDataJSON: String,
    ) {
        val decoder = Base64.getDecoder()
        verify(
            attestationObject = AttestationObject(
                com.yubico.webauthn.data.ByteArray(decoder.decode(base64AttestationObject)),
            ),
            clientDataJSON = com.yubico.webauthn.data.ByteArray(decoder.decode(base64ClientDataJSON)),
        )
    }

    private fun verify(
        attestationObject: AttestationObject,
        clientDataJSON: com.yubico.webauthn.data.ByteArray,
    ) {
        println(attestationObject)
        println(clientDataJSON)
    }
}
