package net.matsudamper.money.backend.feature.aimailparser

import java.time.ZoneOffset
import net.matsudamper.money.backend.app.interfaces.ImportedMailRepository
import net.matsudamper.money.backend.app.interfaces.UserConfigRepository
import net.matsudamper.money.backend.base.TraceLogger
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.element.UserId

class ParseImportedMailWithAiUseCase(
    private val userConfigRepository: UserConfigRepository,
    private val importedMailRepository: ImportedMailRepository,
    private val aiMailParser: AiMailParser,
) {
    fun parse(
        userId: UserId,
        importedMailId: ImportedMailId,
    ): Result {
        val apiKey = userConfigRepository.getGeminiApiKey(userId)
        if (apiKey.isNullOrBlank()) return Result.ApiKeyNotSet

        val mail = importedMailRepository
            .getMails(userId = userId, mailIds = listOf(importedMailId))
            .firstOrNull()
            ?: return Result.MailNotFound

        val timezoneOffset = userConfigRepository.getTimezoneOffset(userId) ?: ZoneOffset.UTC
        val input = AiMailParseInput(
            subject = mail.subject,
            from = mail.from,
            dateTime = mail.dateTime.plusSeconds(timezoneOffset.totalSeconds.toLong()),
            plain = mail.plain,
            html = mail.html,
        )
        return aiMailParser.parse(apiKey = apiKey, input = input).fold(
            onSuccess = { usages -> Result.Success(usages) },
            onFailure = { throwable ->
                TraceLogger.impl().noticeThrowable(throwable, isError = false)
                Result.ParseFailed(errorMessage = throwable.message ?: throwable::class.java.simpleName)
            },
        )
    }

    sealed interface Result {
        data class Success(val usages: List<AiParsedUsage>) : Result
        data object ApiKeyNotSet : Result
        data object MailNotFound : Result
        data class ParseFailed(val errorMessage: String) : Result
    }
}
