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
     * メールが存在し、実行中のパースが無い、または [staleRunningBefore] より前から実行中のまま止まっている場合のみ実行中にする。
     * @param startedDateTime 保存時にこの実行の結果かを判定するため秒単位で渡す
     * @return 実行中にできたか
     */
    fun tryStartParsing(
        userId: UserId,
        importedMailId: ImportedMailId,
        startedDateTime: LocalDateTime,
        staleRunningBefore: LocalDateTime,
    ): Boolean

    /**
     * [startedDateTime] で開始した実行が実行中のまま残っている場合のみ保存する
     */

    fun saveSucceeded(
        userId: UserId,
        importedMailId: ImportedMailId,
        startedDateTime: LocalDateTime,
        usages: List<ParsedUsage>,
        now: LocalDateTime,
    ): Boolean

    /**
     * [startedDateTime] で開始した実行が実行中のまま残っている場合のみ保存する
     */
    fun saveFailed(
        userId: UserId,
        importedMailId: ImportedMailId,
        startedDateTime: LocalDateTime,
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
