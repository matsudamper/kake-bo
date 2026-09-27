package net.matsudamper.money.backend.datasource.db.repository

import java.time.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.matsudamper.money.backend.app.interfaces.ImportedMailAiParseRepository
import net.matsudamper.money.backend.app.interfaces.ImportedMailAiParseRepository.AiParseResult
import net.matsudamper.money.backend.app.interfaces.ImportedMailAiParseRepository.ParsedUsage
import net.matsudamper.money.backend.datasource.db.DbConnection
import net.matsudamper.money.db.schema.tables.JUserMailAiParseResults
import net.matsudamper.money.db.schema.tables.records.JUserMailAiParseResultsRecord
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.element.UserId
import org.jooq.impl.DSL

class DbImportedMailAiParseRepository(
    private val dbConnection: DbConnection,
) : ImportedMailAiParseRepository {
    private val aiParseResults = JUserMailAiParseResults.USER_MAIL_AI_PARSE_RESULTS
    private val json = Json { ignoreUnknownKeys = true }

    override fun getResult(
        userId: UserId,
        importedMailId: ImportedMailId,
    ): AiParseResult? {
        val record = dbConnection.use { connection ->
            DSL.using(connection)
                .selectFrom(aiParseResults)
                .where(
                    aiParseResults.USER_ID.eq(userId.value)
                        .and(aiParseResults.USER_MAIL_ID.eq(importedMailId.id)),
                )
                .fetchOne()
        } ?: return null
        return record.toAiParseResult()
    }

    override fun tryStartParsing(
        userId: UserId,
        importedMailId: ImportedMailId,
        now: LocalDateTime,
        staleRunningBefore: LocalDateTime,
    ): Boolean {
        return dbConnection.use { connection ->
            DSL.using(connection).transactionResult { configuration ->
                val transaction = DSL.using(configuration)
                val current = transaction
                    .selectFrom(aiParseResults)
                    .where(aiParseResults.USER_MAIL_ID.eq(importedMailId.id))
                    .forUpdate()
                    .fetchOne()
                if (current != null) {
                    if (current.userId != userId.value) return@transactionResult false
                    val updatedDateTime = current.updateDatetime
                    val isRunning = current.status == STATUS_RUNNING &&
                        updatedDateTime != null &&
                        updatedDateTime.isAfter(staleRunningBefore)
                    if (isRunning) return@transactionResult false
                }
                transaction
                    .insertInto(aiParseResults)
                    .set(aiParseResults.USER_MAIL_ID, importedMailId.id)
                    .set(aiParseResults.USER_ID, userId.value)
                    .set(aiParseResults.STATUS, STATUS_RUNNING)
                    .set(aiParseResults.UPDATE_DATETIME, now)
                    .onDuplicateKeyUpdate()
                    .set(aiParseResults.STATUS, STATUS_RUNNING)
                    .set(aiParseResults.ERROR_MESSAGE, null as String?)
                    .set(aiParseResults.UPDATE_DATETIME, now)
                    .execute()
                true
            }
        }
    }

    override fun saveSucceeded(
        userId: UserId,
        importedMailId: ImportedMailId,
        usages: List<ParsedUsage>,
        now: LocalDateTime,
    ): Boolean {
        val resultJson = json.encodeToString(
            usages.map { usage ->
                StoredUsage(
                    title = usage.title,
                    description = usage.description,
                    amount = usage.amount,
                    dateTime = usage.dateTime?.toString(),
                )
            },
        )
        return updateResult(
            userId = userId,
            importedMailId = importedMailId,
            status = STATUS_SUCCEEDED,
            resultJson = resultJson,
            errorMessage = null,
            now = now,
        )
    }

    override fun saveFailed(
        userId: UserId,
        importedMailId: ImportedMailId,
        errorMessage: String,
        now: LocalDateTime,
    ): Boolean {
        return updateResult(
            userId = userId,
            importedMailId = importedMailId,
            status = STATUS_FAILED,
            resultJson = null,
            errorMessage = errorMessage.take(ERROR_MESSAGE_MAX_LENGTH),
            now = now,
        )
    }

    private fun updateResult(
        userId: UserId,
        importedMailId: ImportedMailId,
        status: String,
        resultJson: String?,
        errorMessage: String?,
        now: LocalDateTime,
    ): Boolean {
        return dbConnection.use { connection ->
            DSL.using(connection)
                .update(aiParseResults)
                .set(aiParseResults.STATUS, status)
                .set(aiParseResults.RESULT_JSON, resultJson)
                .set(aiParseResults.ERROR_MESSAGE, errorMessage)
                .set(aiParseResults.UPDATE_DATETIME, now)
                .where(
                    aiParseResults.USER_ID.eq(userId.value)
                        .and(aiParseResults.USER_MAIL_ID.eq(importedMailId.id)),
                )
                .execute()
        } == 1
    }

    private fun JUserMailAiParseResultsRecord.toAiParseResult(): AiParseResult? {
        val updatedDateTime = updateDatetime ?: return null
        return when (status) {
            STATUS_RUNNING -> AiParseResult.Running(
                updatedDateTime = updatedDateTime,
            )

            STATUS_SUCCEEDED -> AiParseResult.Succeeded(
                usages = json.decodeFromString<List<StoredUsage>>(resultJson ?: "[]").map { usage ->
                    ParsedUsage(
                        title = usage.title,
                        description = usage.description,
                        amount = usage.amount,
                        dateTime = usage.dateTime?.let { LocalDateTime.parse(it) },
                    )
                },
                updatedDateTime = updatedDateTime,
            )

            STATUS_FAILED -> AiParseResult.Failed(
                errorMessage = errorMessage.orEmpty(),
                updatedDateTime = updatedDateTime,
            )

            else -> null
        }
    }

    @Serializable
    private data class StoredUsage(
        val title: String,
        val description: String,
        val amount: Int?,
        val dateTime: String?,
    )

    private companion object {
        private const val STATUS_RUNNING = "RUNNING"
        private const val STATUS_SUCCEEDED = "SUCCEEDED"
        private const val STATUS_FAILED = "FAILED"
        private const val ERROR_MESSAGE_MAX_LENGTH = 1000
    }
}
