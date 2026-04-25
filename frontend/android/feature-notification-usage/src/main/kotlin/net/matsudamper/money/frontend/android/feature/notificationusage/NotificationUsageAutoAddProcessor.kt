package net.matsudamper.money.frontend.android.feature.notificationusage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.frontend.common.base.AppSettingsRepository
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageDraft
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageParser
import net.matsudamper.money.frontend.common.base.notification.NotificationUsageRecord

internal class NotificationUsageAutoAddProcessor(
    private val dao: NotificationUsageDao,
    private val parsers: List<NotificationUsageParser>,
    private val appSettingsRepository: AppSettingsRepository,
    private val api: NotificationUsageAutoAddApi,
    private val categoryFilterRepository: NotificationUsageCategoryFilterRepository,
) {
    private val inFlightKeys = MutableStateFlow<Set<String>>(emptySet())

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
            val parser = parserAndDraft.first
            val draft = parserAndDraft.second
            val subCategoryId = categoryFilterRepository.getMatchingSubCategoryId(
                title = draft.title,
                serviceName = parser.filterDefinition.title,
            )
            val moneyUsageId = api.addUsage(draftToPayload(draft, subCategoryId))
            if (moneyUsageId != null) {
                dao.markAsAdded(notificationKey, moneyUsageId.id)
            }
        } finally {
            finishProcessing(notificationKey)
        }
    }

    private fun startProcessing(notificationKey: String): Boolean {
        val previous = inFlightKeys.getAndUpdate { it + notificationKey }
        return notificationKey !in previous
    }

    private fun finishProcessing(notificationKey: String) {
        inFlightKeys.update { it - notificationKey }
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

    private fun draftToPayload(
        draft: NotificationUsageDraft,
        subCategoryId: MoneyUsageSubCategoryId?,
    ): NotificationUsageAutoAddPayload {
        return NotificationUsageAutoAddPayload(
            title = draft.title,
            description = draft.description,
            amount = draft.amount ?: 0,
            dateTime = draft.dateTime,
            subCategoryId = subCategoryId,
        )
    }
}
