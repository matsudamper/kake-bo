package net.matsudamper.money.backend.graphql.resolver

import net.matsudamper.money.backend.app.interfaces.MailFilterRepository
import net.matsudamper.money.backend.app.interfaces.element.ImportedMailCategoryFilterConditionType
import net.matsudamper.money.backend.app.interfaces.element.ImportedMailCategoryFilterDatasourceType
import net.matsudamper.money.backend.app.interfaces.element.ImportedMailFilterCategoryConditionOperator
import net.matsudamper.money.categoryfilter.CategoryFilter
import net.matsudamper.money.categoryfilter.CategoryFilterCondition
import net.matsudamper.money.categoryfilter.CategoryFilterConditionType
import net.matsudamper.money.categoryfilter.CategoryFilterDataSourceType
import net.matsudamper.money.categoryfilter.CategoryFilterOperator

object CategoryFilterFactory {
    fun create(
        filters: List<MailFilterRepository.MailFilter>,
        conditions: List<MailFilterRepository.Condition>,
    ): List<CategoryFilter> {
        val conditionsMap = conditions.groupBy { it.filterId }
        return filters.map { filter ->
            CategoryFilter(
                orderNumber = filter.orderNumber,
                operator = toShared(filter.operator),
                subCategoryId = filter.moneyUsageSubCategoryId,
                descriptionSuffix = filter.descriptionSuffix,
                conditions = conditionsMap[filter.importedMailCategoryFilterId].orEmpty().map { condition ->
                    CategoryFilterCondition(
                        text = condition.text,
                        dataSourceType = toShared(condition.dataSourceType),
                        conditionType = toShared(condition.conditionType),
                    )
                },
            )
        }
    }

    private fun toShared(operator: ImportedMailFilterCategoryConditionOperator): CategoryFilterOperator {
        return when (operator) {
            ImportedMailFilterCategoryConditionOperator.AND -> CategoryFilterOperator.AND
            ImportedMailFilterCategoryConditionOperator.OR -> CategoryFilterOperator.OR
        }
    }

    private fun toShared(dataSourceType: ImportedMailCategoryFilterDatasourceType): CategoryFilterDataSourceType {
        return when (dataSourceType) {
            ImportedMailCategoryFilterDatasourceType.MailTitle -> CategoryFilterDataSourceType.MailTitle
            ImportedMailCategoryFilterDatasourceType.MailFrom -> CategoryFilterDataSourceType.MailFrom
            ImportedMailCategoryFilterDatasourceType.MailHTML -> CategoryFilterDataSourceType.MailHtml
            ImportedMailCategoryFilterDatasourceType.MailPlain -> CategoryFilterDataSourceType.MailPlain
            ImportedMailCategoryFilterDatasourceType.Title -> CategoryFilterDataSourceType.Title
            ImportedMailCategoryFilterDatasourceType.ServiceName -> CategoryFilterDataSourceType.ServiceName
        }
    }

    private fun toShared(conditionType: ImportedMailCategoryFilterConditionType): CategoryFilterConditionType {
        return when (conditionType) {
            ImportedMailCategoryFilterConditionType.Include -> CategoryFilterConditionType.Include
            ImportedMailCategoryFilterConditionType.NotInclude -> CategoryFilterConditionType.NotInclude
            ImportedMailCategoryFilterConditionType.Equal -> CategoryFilterConditionType.Equal
            ImportedMailCategoryFilterConditionType.NotEqual -> CategoryFilterConditionType.NotEqual
        }
    }
}
