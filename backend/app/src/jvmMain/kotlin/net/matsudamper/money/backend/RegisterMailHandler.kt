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
import net.matsudamper.money.element.UserId

class RegisterMailHandler(
    private val diContainer: DiContainer,
) {
    private val apiTokenRepository get() = diContainer.createApiTokenRepository()
    private val importedMailRepository get() = diContainer.createDbMailRepository()
    fun authenticate(apiKey: String?): UserId? {
        apiKey ?: return null
        val encryptInfo = ApiTokenEncryptManager().getEncryptInfo(apiKey) ?: return null

        val hashedPassword = PasswordManager().getHashedPassword(
            password = apiKey,
            salt = encryptInfo.salt,
            iterationCount = encryptInfo.iterationCount,
            keyLength = encryptInfo.keyByteLength,
            algorithm = IPasswordManager.Algorithm.entries.first { it.algorithmName == encryptInfo.algorithmName },
        )

        return apiTokenRepository.verifyToken(hashedToken = hashedPassword)?.userId
    }

    fun handle(
        request: Request,
        userId: UserId,
    ): Result {
        val mail = MailParser.rawContentToResponse(request.raw)

        val addResult = importedMailRepository.addMail(
            userId = userId,
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
