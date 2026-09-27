package net.matsudamper.money.backend.feature.aimailparser

import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.matsudamper.money.backend.app.interfaces.ImportedMailAiParseRepository
import net.matsudamper.money.backend.app.interfaces.ImportedMailRepository
import net.matsudamper.money.backend.app.interfaces.UserConfigRepository
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.element.UserId

class StartImportedMailAiParseUseCase(
    private val userConfigRepository: UserConfigRepository,
    private val importedMailRepository: ImportedMailRepository,
    private val aiParseRepository: ImportedMailAiParseRepository,
    private val aiMailParser: AiMailParser,
    private val clock: Clock,
    private val backgroundScope: CoroutineScope,
) {
    fun start(
        userId: UserId,
        importedMailId: ImportedMailId,
    ): Result {
        val apiKey = userConfigRepository.getGeminiApiKey(userId)
        if (apiKey.isNullOrBlank()) return Result.ApiKeyNotSet

        val mail = importedMailRepository
            .getMails(userId = userId, mailIds = listOf(importedMailId))
            .firstOrNull()
            ?: return Result.MailNotFound

        // DBのDATETIMEは秒単位なので、保存時の照合に使えるよう秒に揃える
        val startedDateTime = currentDateTime().truncatedTo(ChronoUnit.SECONDS)
        val isStarted = aiParseRepository.tryStartParsing(
            userId = userId,
            importedMailId = importedMailId,
            startedDateTime = startedDateTime,
            staleRunningBefore = startedDateTime.minus(STALE_RUNNING_DURATION),
        )
        if (isStarted.not()) return Result.AlreadyRunning

        val timezoneOffset = userConfigRepository.getTimezoneOffset(userId) ?: ZoneOffset.UTC
        val input = AiMailParseInput(
            subject = mail.subject,
            from = mail.from,
            dateTime = mail.dateTime.plusSeconds(timezoneOffset.totalSeconds.toLong()),
            plain = mail.plain,
            html = mail.html,
        )
        backgroundScope.launch {
            parseAndSave(
                userId = userId,
                importedMailId = importedMailId,
                startedDateTime = startedDateTime,
                apiKey = apiKey,
                input = input,
            )
        }
        return Result.Started
    }

    private fun parseAndSave(
        userId: UserId,
        importedMailId: ImportedMailId,
        startedDateTime: LocalDateTime,
        apiKey: String,
        input: AiMailParseInput,
    ) {
        val parseResult = aiMailParser.parse(
            apiKey = apiKey,
            input = input,
        )
        runCatching {
            parseResult.fold(
                onSuccess = { usages ->
                    aiParseRepository.saveSucceeded(
                        userId = userId,
                        importedMailId = importedMailId,
                        startedDateTime = startedDateTime,
                        usages = usages.map { usage ->
                            ImportedMailAiParseRepository.ParsedUsage(
                                title = usage.title,
                                description = usage.description,
                                amount = usage.amount,
                                dateTime = usage.dateTime,
                            )
                        },
                        now = currentDateTime(),
                    )
                },
                onFailure = { throwable ->
                    throwable.printStackTrace()
                    aiParseRepository.saveFailed(
                        userId = userId,
                        importedMailId = importedMailId,
                        startedDateTime = startedDateTime,
                        errorMessage = throwable.message ?: throwable::class.java.simpleName,
                        now = currentDateTime(),
                    )
                },
            )
        }.onFailure {
            it.printStackTrace()
        }
    }

    private fun currentDateTime(): LocalDateTime {
        return LocalDateTime.now(clock)
    }

    sealed interface Result {
        data object Started : Result
        data object ApiKeyNotSet : Result
        data object MailNotFound : Result
        data object AlreadyRunning : Result
    }

    companion object {
        // サーバー再起動などで実行中のまま残ったものを再実行できるようにする
        val STALE_RUNNING_DURATION: Duration = Duration.ofMinutes(10)
    }
}
