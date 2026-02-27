package net.matsudamper.money.backend.app.interfaces

import java.time.LocalDate
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.element.UserId

interface RecurringUsageRuleRepository {
    fun addRule(
        userId: UserId,
        title: String,
        description: String,
        amount: Int,
        subCategoryId: MoneyUsageSubCategoryId?,
        firstUsageDate: LocalDate,
        intervalIsoPeriod: String,
        leadTimeIsoPeriod: String,
    ): Result<Unit>

    fun processDueRules(nowDate: LocalDate): Result<Int>
}
