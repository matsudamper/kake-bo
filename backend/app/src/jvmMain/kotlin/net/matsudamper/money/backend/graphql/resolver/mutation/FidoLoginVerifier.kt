package net.matsudamper.money.backend.graphql.resolver.mutation

import java.util.Base64
import net.matsudamper.money.backend.fido.Auth4JModel
import net.matsudamper.money.backend.fido.AuthenticatorConverter
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.exception.GraphqlExceptions
import net.matsudamper.money.backend.lib.ChallengeModel
import net.matsudamper.money.element.UserId
import net.matsudamper.money.graphql.model.QlUserFidoLoginInput

internal class FidoLoginVerifier(
    private val context: GraphQlContext,
) {
    fun verifyAndGetUserId(input: QlUserFidoLoginInput): UserId? {
        val fidoRepository = context.diContainer.createFidoRepository()
        val challengeRepository = context.diContainer.createChallengeRepository()
        val requestUserId = String(
            Base64.getUrlDecoder().decode(input.base64UserHandle),
            Charsets.UTF_8,
        ).toIntOrNull()
            ?.let { UserId(it) }
            ?: return null
        val fidoList = fidoRepository.getFidoList(requestUserId)
        if (ChallengeModel(challengeRepository).validateChallenge(challenge = input.challenge).not()) {
            throw GraphqlExceptions.BadRequest("challenge is invalid")
        }

        for (fido in fidoList) {
            val authenticator = AuthenticatorConverter.convertFromBase64(
                base64AttestationStatement = fido.attestedStatement,
                attestationStatementFormat = fido.attestedStatementFormat,
                base64AttestedCredentialData = fido.attestedCredentialData,
                counter = fido.counter,
            )
            runCatching {
                Auth4JModel(
                    challenge = input.challenge,
                ).verify(
                    authenticator = authenticator,
                    credentialId = input.credentialId.toByteArray(),
                    base64UserHandle = input.base64UserHandle.toByteArray(),
                    base64AuthenticatorData = input.base64AuthenticatorData.toByteArray(),
                    base64ClientDataJSON = input.base64ClientDataJson.toByteArray(),
                    clientExtensionJSON = null,
                    base64Signature = input.base64Signature.toByteArray(),
                )
            }.getOrNull() ?: continue

            fidoRepository.updateCounter(
                fidoId = fido.fidoId,
                counter = authenticator.counter,
                userId = requestUserId,
            )
            return requestUserId
        }
        return null
    }
}
