package net.matsudamper.money.backend.app.interfaces

import java.time.LocalDateTime
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.element.UserId

interface ImportedMailAiParseRepository {
    fun getResult(
        userId: UserId,
        importedMailId: ImportedMailId,
    ): AiParseResult?

    /**
     * 実行中のパースが無い、または [staleRunningBefore] より前から実行中のまま止まっている場合のみ実行中にする。
     * @return 実行中にできたか
     */
    fun tryStartParsing(
        userId: UserId,
        importedMailId: ImportedMailId,
        now: LocalDateTime,
        staleRunningBefore: LocalDateTime,
    ): Boolean

    fun saveSucceeded(
        userId: UserId,
        importedMailId: ImportedMailId,
        usages: List<ParsedUsage>,
        now: LocalDateTime,
    ): Boolean

    fun saveFailed(
        userId: UserId,
        importedMailId: ImportedMailId,
        errorMessage: String,
        now: LocalDateTime,
    ): Boolean

    sealed interface AiParseResult {
        val updatedDateTime: LocalDateTime

        data class Running(
            override val updatedDateTime: LocalDateTime,
        ) : AiParseResult

        data class Succeeded(
            val usages: List<ParsedUsage>,
            override val updatedDateTime: LocalDateTime,
        ) : AiParseResult

        data class Failed(
            val errorMessage: String,
            override val updatedDateTime: LocalDateTime,
        ) : AiParseResult
    }

    data class ParsedUsage(
        val title: String,
        val description: String,
        val amount: Int?,
        val dateTime: LocalDateTime?,
    )
}
