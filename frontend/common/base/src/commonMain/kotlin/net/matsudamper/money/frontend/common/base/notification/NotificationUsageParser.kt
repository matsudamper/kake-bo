package net.matsudamper.money.frontend.common.base.notification

public interface NotificationUsageParser {
    public val filterDefinition: NotificationUsageFilterDefinition

    public fun parse(record: NotificationUsageRecord): NotificationUsageDraft?
}
