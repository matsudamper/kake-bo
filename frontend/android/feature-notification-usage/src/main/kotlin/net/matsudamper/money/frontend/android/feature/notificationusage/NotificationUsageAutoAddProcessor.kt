package net.matsudamper.money.frontend.android.feature.notificationusage

import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.apollographql.apollo.api.Optional
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.common.base.AppSettingsRepository
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageDraft
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageParser
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRecord
import net.matsudamper.money.frontend.graphql.AddMoneyUsageMutation
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.type.AddUsageQuery

internal class NotificationUsageAutoAddProcessor(
    private val dao: NotificationUsageDao,
    private val parsers: List<NotificationUsageParser>,
    private val appSettingsRepository: AppSettingsRepository,
    private val api: NotificationUsageAutoAddApi,
) {
    private val inFlightMutex = Mutex()
    private val inFlightKeys = mutableSetOf<String>()

    suspend fun process(notificationKey: String) {
        if (!startProcessing(notificationKey)) return

        try {
            val entity = dao.findByKey(notificationKey) ?: return
            if (entity.isAdded) return

            val record = entity.toRecord()
            val parserAndDraft = parsers.firstNotNullOfOrNull { parser ->
                val draft = parser.parse(record) ?: return@firstNotNullOfOrNull null
                if (!appSettingsRepository.notificationUsageAutoAddEnabled(parser.filterDefinition.id).first()) {
                    return@firstNotNullOfOrNull null
                }
                parser to draft
            } ?: return
            val draft = parserAndDraft.second
            val moneyUsageId = api.addUsage(draft.toPayload(record))
            if (moneyUsageId != null) {
                dao.markAsAdded(notificationKey, moneyUsageId.id)
            }
        } finally {
            finishProcessing(notificationKey)
        }
    }

    private suspend fun startProcessing(notificationKey: String): Boolean {
        return inFlightMutex.withLock {
            if (notificationKey in inFlightKeys) {
                false
            } else {
                inFlightKeys += notificationKey
                true
            }
        }
    }

    private suspend fun finishProcessing(notificationKey: String) {
        inFlightMutex.withLock {
            inFlightKeys -= notificationKey
        }
    }

    private fun NotificationUsageEntity.toRecord(): NotificationUsageRecord {
        return NotificationUsageRecord(
            notificationKey = notificationKey,
            packageName = packageName,
            text = text,
            postedAtEpochMillis = postedAtEpochMillis,
            receivedAtEpochMillis = receivedAtEpochMillis,
            isAdded = isAdded,
            moneyUsageId = moneyUsageId?.let { MoneyUsageId(it) },
        )
    }

    private fun NotificationUsageDraft.toPayload(record: NotificationUsageRecord): NotificationUsageAutoAddPayload {
        return NotificationUsageAutoAddPayload(
            title = title ?: record.packageName,
            description = description ?: record.text,
            amount = amount ?: 0,
            dateTime = dateTime ?: Instant
                .fromEpochMilliseconds(record.postedAtEpochMillis)
                .toLocalDateTime(TimeZone.currentSystemDefault()),
            subCategoryId = subCategoryId,
        )
    }
}

internal data class NotificationUsageAutoAddPayload(
    val title: String,
    val description: String,
    val amount: Int,
    val dateTime: kotlinx.datetime.LocalDateTime,
    val subCategoryId: MoneyUsageSubCategoryId?,
)

internal interface NotificationUsageAutoAddApi {
    suspend fun addUsage(payload: NotificationUsageAutoAddPayload): MoneyUsageId?
}

internal class NotificationUsageAutoAddGraphqlApi(
    private val graphqlClient: GraphqlClient,
) : NotificationUsageAutoAddApi {
    override suspend fun addUsage(payload: NotificationUsageAutoAddPayload): MoneyUsageId? {
        return runCatching {
            graphqlClient.apolloClient
                .mutation(
                    AddMoneyUsageMutation(
                        query = AddUsageQuery(
                            title = payload.title,
                            description = payload.description,
                            subCategoryId = Optional.present(payload.subCategoryId),
                            amount = payload.amount,
                            date = payload.dateTime,
                            importedMailId = Optional.absent(),
                            imageIds = Optional.absent(),
                        ),
                    ),
                )
                .execute()
                .data?.userMutation?.addUsage?.id
        }.getOrNull()
    }
}
