package net.matsudamper.money.backend

import kotlinx.serialization.Serializable
import com.fasterxml.jackson.annotation.JsonProperty
import net.matsudamper.money.backend.app.interfaces.ApiTokenRepository
import net.matsudamper.money.backend.base.mailparser.MailParser
import net.matsudamper.money.backend.logic.ApiTokenEncryptManager
import net.matsudamper.money.backend.logic.IPasswordManager
import net.matsudamper.money.backend.logic.PasswordManager

class RegisterMailHandler(
    private val apiTokenRepository: ApiTokenRepository,
) {
    fun handle(
        request: Request,
        apiKey: String?,
    ): Result {
        apiKey ?: return Result.Forbidden
        val result = MailParser.rawContentToResponse(request.raw)
        val encryptInfo = ApiTokenEncryptManager().getEncryptInfo(apiKey) ?: return Result.Forbidden

        val hashedPassword = PasswordManager().getHashedPassword(
            password = apiKey,
            salt = ByteArray(0),
            iterationCount = encryptInfo.iterationCount,
            keyLength = encryptInfo.keyByteLength,
            algorithm = IPasswordManager.Algorithm.entries.first { it.algorithmName == encryptInfo.algorithmName },
        )

        val verifyResult = apiTokenRepository.verifyToken(hashedToken = hashedPassword)
            ?: return Result.Forbidden

        verifyResult.userId
        return Result.Success(
            Response(
                status =
                if (result.content.isEmpty()) {
                    Response.Status.ERROR
                } else {
                    Response.Status.OK
                },
            ),
        )
    }

    @Serializable
    data class Request(
        @JsonProperty("raw") val raw: String,
    )

    sealed interface Result {
        data class Success(val response: Response) : Result
        data object Forbidden : Result
    }

    @Serializable
    data class Response(
        @JsonProperty("status") val status: Status,
    ) {
        @Serializable
        enum class Status {
            OK,
            ERROR,
        }
    }
}
