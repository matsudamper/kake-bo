package net.matsudamper.money.backend.datasource.db.repository

import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import net.matsudamper.money.backend.app.interfaces.RecurringUsageRuleRepository
import net.matsudamper.money.backend.datasource.db.DbConnectionImpl
import net.matsudamper.money.db.schema.tables.JMoneyUsageSubCategories
import net.matsudamper.money.db.schema.tables.JMoneyUsages
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.element.UserId
import org.jooq.impl.DSL

class DbRecurringUsageRuleRepository : RecurringUsageRuleRepository {
    private val jMoneyUsages = JMoneyUsages.MONEY_USAGES
    private val jSubCategory = JMoneyUsageSubCategories.MONEY_USAGE_SUB_CATEGORIES

    private val recurringRulesTable = DSL.table("recurring_usage_rules")
    private val idField = DSL.field("recurring_usage_rule_id", Int::class.java)
    private val userIdField = DSL.field("user_id", Int::class.java)
    private val titleField = DSL.field("title", String::class.java)
    private val descriptionField = DSL.field("description", String::class.java)
    private val amountField = DSL.field("amount", Int::class.java)
    private val subCategoryField = DSL.field("money_usage_sub_category_id", Int::class.java)
    private val nextUsageDateField = DSL.field("next_usage_date", LocalDate::class.java)
    private val intervalIsoField = DSL.field("interval_iso", String::class.java)
    private val leadTimeIsoField = DSL.field("lead_time_iso", String::class.java)

    override fun addRule(
        userId: UserId,
        title: String,
        description: String,
        amount: Int,
        subCategoryId: MoneyUsageSubCategoryId?,
        firstUsageDate: LocalDate,
        intervalIsoPeriod: String,
        leadTimeIsoPeriod: String,
    ): Result<Unit> {
        return runCatching {
            val interval = parseInterval(intervalIsoPeriod)
            val leadTime = parseLeadTime(leadTimeIsoPeriod)
            validateLeadTime(interval = interval, leadTime = leadTime)

            DbConnectionImpl.use { connection ->
                val context = DSL.using(connection)
                if (subCategoryId != null) {
                    val count = context
                        .selectCount()
                        .from(jSubCategory)
                        .where(jSubCategory.USER_ID.eq(userId.value))
                        .and(jSubCategory.MONEY_USAGE_SUB_CATEGORY_ID.eq(subCategoryId.id))
                        .fetchOne(0, Int::class.java)
                    if (count != 1) {
                        throw IllegalArgumentException("subCategory is not found")
                    }
                }

                context
                    .insertInto(recurringRulesTable)
                    .set(userIdField, userId.value)
                    .set(titleField, title)
                    .set(descriptionField, description)
                    .set(amountField, amount)
                    .set(subCategoryField, subCategoryId?.id)
                    .set(nextUsageDateField, firstUsageDate)
                    .set(intervalIsoField, intervalIsoPeriod)
                    .set(leadTimeIsoField, leadTimeIsoPeriod)
                    .execute()
            }
        }
    }

    override fun processDueRules(nowDate: LocalDate): Result<Int> {
        return runCatching {
            DbConnectionImpl.use { connection ->
                val context = DSL.using(connection)
                val rules = context
                    .select(
                        idField,
                        userIdField,
                        titleField,
                        descriptionField,
                        amountField,
                        subCategoryField,
                        nextUsageDateField,
                        intervalIsoField,
                        leadTimeIsoField,
                    )
                    .from(recurringRulesTable)
                    .fetch()

                var insertedCount = 0
                for (rule in rules) {
                    val ruleId = rule.get(idField) ?: continue
                    val usageUserId = rule.get(userIdField) ?: continue
                    val usageTitle = rule.get(titleField) ?: continue
                    val usageDescription = rule.get(descriptionField) ?: continue
                    val usageAmount = rule.get(amountField) ?: continue
                    val usageSubCategoryId = rule.get(subCategoryField)
                    val interval = parseInterval(rule.get(intervalIsoField) ?: continue)
                    val leadTime = parseLeadTime(rule.get(leadTimeIsoField) ?: continue)
                    var nextUsageDate = rule.get(nextUsageDateField) ?: continue

                    while (!nowDate.isBefore(nextUsageDate.minus(leadTime))) {
                        context
                            .insertInto(jMoneyUsages)
                            .set(jMoneyUsages.USER_ID, usageUserId)
                            .set(jMoneyUsages.TITLE, usageTitle)
                            .set(jMoneyUsages.DESCRIPTION, usageDescription)
                            .set(jMoneyUsages.AMOUNT, usageAmount)
                            .set(jMoneyUsages.MONEY_USAGE_SUB_CATEGORY_ID, usageSubCategoryId)
                            .set(jMoneyUsages.DATETIME, nextUsageDate.atStartOfDay())
                            .execute()
                        insertedCount += 1
                        nextUsageDate = nextUsageDate.plus(interval)
                    }

                    context
                        .update(recurringRulesTable)
                        .set(nextUsageDateField, nextUsageDate)
                        .where(idField.eq(ruleId))
                        .and(userIdField.eq(usageUserId))
                        .execute()
                }
                insertedCount
            }
        }
    }

    private fun parseInterval(value: String): Period {
        val period = Period.parse(value)
        if (period.isNegative || period.isZero) {
            throw IllegalArgumentException("interval must be positive")
        }

        val isWeek = period.years == 0 && period.months == 0 && period.days % 7 == 0
        val isMonth = period.years == 0 && period.months > 0 && period.days == 0
        if (!isWeek && !isMonth) {
            throw IllegalArgumentException("interval must be only n weeks or n months")
        }

        return period
    }

    private fun parseLeadTime(value: String): Period {
        val period = Period.parse(value)
        if (period.isNegative || period.isZero) {
            throw IllegalArgumentException("lead time must be positive")
        }
        return period
    }

    private fun validateLeadTime(interval: Period, leadTime: Period) {
        val baseDate = LocalDate.of(2000, 1, 1)
        val intervalDays = ChronoUnit.DAYS.between(baseDate, baseDate.plus(interval))
        val leadTimeDays = ChronoUnit.DAYS.between(baseDate, baseDate.plus(leadTime))

        if (leadTimeDays > intervalDays && leadTimeDays > 7) {
            throw IllegalArgumentException("lead time cannot exceed interval except up to 1 week")
        }
    }
}
