package net.matsudamper.money.backend

import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.serialization.Serializable
import com.fasterxml.jackson.annotation.JsonProperty
import net.matsudamper.money.backend.app.interfaces.ImportedMailRepository
import net.matsudamper.money.backend.base.element.MailResult
import net.matsudamper.money.backend.base.mailparser.MailParser
import net.matsudamper.money.backend.di.DiContainer
import net.matsudamper.money.backend.logic.ApiTokenEncryptManager
import net.matsudamper.money.backend.logic.IPasswordManager
import net.matsudamper.money.backend.logic.PasswordManager

class RegisterMailHandler(
    private val diContainer: DiContainer,
) {
    private val apiTokenRepository get() = diContainer.createApiTokenRepository()
    private val importedMailRepository get() = diContainer.createDbMailRepository()
    fun handle(
        request: Request,
        apiKey: String?,
    ): Result {
        apiKey ?: return Result.Forbidden
        val encryptInfo = ApiTokenEncryptManager().getEncryptInfo(apiKey) ?: return Result.Forbidden

        val hashedPassword = PasswordManager().getHashedPassword(
            password = apiKey,
            salt = encryptInfo.salt,
            iterationCount = encryptInfo.iterationCount,
            keyLength = encryptInfo.keyByteLength,
            algorithm = IPasswordManager.Algorithm.entries.first { it.algorithmName == encryptInfo.algorithmName },
        )

        val verifyResult = apiTokenRepository.verifyToken(hashedToken = hashedPassword)
            ?: return Result.Forbidden

        val mail = MailParser.rawContentToResponse(request.raw)

        val addResult = importedMailRepository.addMail(
            userId = verifyResult.userId,
            plainText = mail.content.filterIsInstance<MailResult.Content.Text>().firstOrNull()?.text,
            html = mail.content.filterIsInstance<MailResult.Content.Html>().firstOrNull()?.html,
            from = mail.from.firstOrNull() ?: "",
            subject = mail.subject,
            dateTime = LocalDateTime.ofInstant(mail.sendDate, ZoneOffset.UTC),
        )

        return when (addResult) {
            is ImportedMailRepository.AddUserResult.Failed -> {
                when (val error = addResult.error) {
                    is ImportedMailRepository.AddUserResult.ErrorType.InternalServerError -> {
                        error.e.printStackTrace()
                    }
                }
                Result.InternalServerError
            }

            is ImportedMailRepository.AddUserResult.Success -> {
                Result.Success(
                    Response(
                        status = if (mail.content.isEmpty()) {
                            Response.Status.ERROR
                        } else {
                            Response.Status.OK
                        },
                    ),
                )
            }
        }
    }

    @Serializable
    data class Request(
        @param:JsonProperty("raw") val raw: String,
    )

    sealed interface Result {
        data class Success(val response: Response) : Result
        data object Forbidden : Result
        data object InternalServerError : Result
    }

    @Serializable
    data class Response(
        @param:JsonProperty("status") val status: Status,
    ) {
        @Serializable
        enum class Status {
            OK,
            ERROR,
        }
    }
}
