package net.matsudamper.money.categoryfilter

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import net.matsudamper.money.element.MoneyUsageSubCategoryId

class CategoryFilterEvaluatorTest : DescribeSpec(
    {
        val subCategoryId1 = MoneyUsageSubCategoryId(1)
        val subCategoryId2 = MoneyUsageSubCategoryId(2)

        fun filter(
            orderNumber: Int,
            operator: CategoryFilterOperator,
            subCategoryId: MoneyUsageSubCategoryId?,
            conditions: List<CategoryFilterCondition>,
        ) = CategoryFilter(
            orderNumber = orderNumber,
            operator = operator,
            subCategoryId = subCategoryId,
            conditions = conditions,
        )

        fun condition(
            text: String,
            dataSourceType: CategoryFilterDataSourceType,
            conditionType: CategoryFilterConditionType,
        ) = CategoryFilterCondition(
            text = text,
            dataSourceType = dataSourceType,
            conditionType = conditionType,
        )

        describe("evaluateCategoryFilters") {
            describe("Title条件") {
                it("Include: タイトルが条件テキストを含む場合はマッチする") {
                    val result = evaluateCategoryFilters(
                        filters = listOf(
                            filter(
                                orderNumber = 1,
                                operator = CategoryFilterOperator.AND,
                                subCategoryId = subCategoryId1,
                                conditions = listOf(
                                    condition("コンビニ", CategoryFilterDataSourceType.Title, CategoryFilterConditionType.Include),
                                ),
                            ),
                        ),
                    ) { type ->
                        when (type) {
                            CategoryFilterDataSourceType.Title -> "コンビニで購入"
                            else -> null
                        }
                    }
                    result.shouldBe(subCategoryId1)
                }

                it("Include: タイトルが条件テキストを含まない場合はマッチしない") {
                    val result = evaluateCategoryFilters(
                        filters = listOf(
                            filter(
                                orderNumber = 1,
                                operator = CategoryFilterOperator.AND,
                                subCategoryId = subCategoryId1,
                                conditions = listOf(
                                    condition("コンビニ", CategoryFilterDataSourceType.Title, CategoryFilterConditionType.Include),
                                ),
                            ),
                        ),
                    ) { type ->
                        when (type) {
                            CategoryFilterDataSourceType.Title -> "スーパーで購入"
                            else -> null
                        }
                    }
                    result.shouldBe(null)
                }

                it("NotInclude: タイトルが条件テキストを含まない場合はマッチする") {
                    val result = evaluateCategoryFilters(
                        filters = listOf(
                            filter(
                                orderNumber = 1,
                                operator = CategoryFilterOperator.AND,
                                subCategoryId = subCategoryId1,
                                conditions = listOf(
                                    condition("コンビニ", CategoryFilterDataSourceType.Title, CategoryFilterConditionType.NotInclude),
                                ),
                            ),
                        ),
                    ) { type ->
                        when (type) {
                            CategoryFilterDataSourceType.Title -> "スーパーで購入"
                            else -> null
                        }
                    }
                    result.shouldBe(subCategoryId1)
                }

                it("Equal: タイトルが条件テキストと完全一致する場合はマッチする") {
                    val result = evaluateCategoryFilters(
                        filters = listOf(
                            filter(
                                orderNumber = 1,
                                operator = CategoryFilterOperator.AND,
                                subCategoryId = subCategoryId1,
                                conditions = listOf(
                                    condition("コンビニ", CategoryFilterDataSourceType.Title, CategoryFilterConditionType.Equal),
                                ),
                            ),
                        ),
                    ) { type ->
                        when (type) {
                            CategoryFilterDataSourceType.Title -> "コンビニ"
                            else -> null
                        }
                    }
                    result.shouldBe(subCategoryId1)
                }

                it("Equal: タイトルが部分一致のみの場合はマッチしない") {
                    val result = evaluateCategoryFilters(
                        filters = listOf(
                            filter(
                                orderNumber = 1,
                                operator = CategoryFilterOperator.AND,
                                subCategoryId = subCategoryId1,
                                conditions = listOf(
                                    condition("コンビニ", CategoryFilterDataSourceType.Title, CategoryFilterConditionType.Equal),
                                ),
                            ),
                        ),
                    ) { type ->
                        when (type) {
                            CategoryFilterDataSourceType.Title -> "コンビニで購入"
                            else -> null
                        }
                    }
                    result.shouldBe(null)
                }

                it("NotEqual: タイトルが条件テキストと異なる場合はマッチする") {
                    val result = evaluateCategoryFilters(
                        filters = listOf(
                            filter(
                                orderNumber = 1,
                                operator = CategoryFilterOperator.AND,
                                subCategoryId = subCategoryId1,
                                conditions = listOf(
                                    condition("コンビニ", CategoryFilterDataSourceType.Title, CategoryFilterConditionType.NotEqual),
                                ),
                            ),
                        ),
                    ) { type ->
                        when (type) {
                            CategoryFilterDataSourceType.Title -> "スーパー"
                            else -> null
                        }
                    }
                    result.shouldBe(subCategoryId1)
                }
            }

            describe("ServiceName条件") {
                it("サービス名が条件にマッチする場合はマッチする") {
                    val result = evaluateCategoryFilters(
                        filters = listOf(
                            filter(
                                orderNumber = 1,
                                operator = CategoryFilterOperator.AND,
                                subCategoryId = subCategoryId1,
                                conditions = listOf(
                                    condition("モバイルSuica", CategoryFilterDataSourceType.ServiceName, CategoryFilterConditionType.Include),
                                ),
                            ),
                        ),
                    ) { type ->
                        when (type) {
                            CategoryFilterDataSourceType.ServiceName -> "モバイルSuica"
                            else -> null
                        }
                    }
                    result.shouldBe(subCategoryId1)
                }
            }

            describe("メール系データソース") {
                it("MailTitle はデータ未提供の場合に評価不能としてマッチしない") {
                    val result = evaluateCategoryFilters(
                        filters = listOf(
                            filter(
                                orderNumber = 1,
                                operator = CategoryFilterOperator.AND,
                                subCategoryId = subCategoryId1,
                                conditions = listOf(
                                    condition("any", CategoryFilterDataSourceType.MailTitle, CategoryFilterConditionType.NotInclude),
                                ),
                            ),
                        ),
                    ) { _ -> null }
                    result.shouldBe(null)
                }
            }

            describe("演算子") {
                it("AND: 全条件がマッチする場合はマッチする") {
                    val result = evaluateCategoryFilters(
                        filters = listOf(
                            filter(
                                orderNumber = 1,
                                operator = CategoryFilterOperator.AND,
                                subCategoryId = subCategoryId1,
                                conditions = listOf(
                                    condition("交通", CategoryFilterDataSourceType.Title, CategoryFilterConditionType.Include),
                                    condition("Suica", CategoryFilterDataSourceType.ServiceName, CategoryFilterConditionType.Include),
                                ),
                            ),
                        ),
                    ) { type ->
                        when (type) {
                            CategoryFilterDataSourceType.Title -> "交通費"
                            CategoryFilterDataSourceType.ServiceName -> "Suica"
                            else -> null
                        }
                    }
                    result.shouldBe(subCategoryId1)
                }

                it("AND: 一部の条件しかマッチしない場合はマッチしない") {
                    val result = evaluateCategoryFilters(
                        filters = listOf(
                            filter(
                                orderNumber = 1,
                                operator = CategoryFilterOperator.AND,
                                subCategoryId = subCategoryId1,
                                conditions = listOf(
                                    condition("交通", CategoryFilterDataSourceType.Title, CategoryFilterConditionType.Include),
                                    condition("Suica", CategoryFilterDataSourceType.ServiceName, CategoryFilterConditionType.Include),
                                ),
                            ),
                        ),
                    ) { type ->
                        when (type) {
                            CategoryFilterDataSourceType.Title -> "食費"
                            CategoryFilterDataSourceType.ServiceName -> "Suica"
                            else -> null
                        }
                    }
                    result.shouldBe(null)
                }

                it("OR: いずれかの条件がマッチする場合はマッチする") {
                    val result = evaluateCategoryFilters(
                        filters = listOf(
                            filter(
                                orderNumber = 1,
                                operator = CategoryFilterOperator.OR,
                                subCategoryId = subCategoryId1,
                                conditions = listOf(
                                    condition("交通", CategoryFilterDataSourceType.Title, CategoryFilterConditionType.Include),
                                    condition("Suica", CategoryFilterDataSourceType.ServiceName, CategoryFilterConditionType.Include),
                                ),
                            ),
                        ),
                    ) { type ->
                        when (type) {
                            CategoryFilterDataSourceType.Title -> "食費"
                            CategoryFilterDataSourceType.ServiceName -> "Suica"
                            else -> null
                        }
                    }
                    result.shouldBe(subCategoryId1)
                }

                it("OR: 全条件がマッチしない場合はマッチしない") {
                    val result = evaluateCategoryFilters(
                        filters = listOf(
                            filter(
                                orderNumber = 1,
                                operator = CategoryFilterOperator.OR,
                                subCategoryId = subCategoryId1,
                                conditions = listOf(
                                    condition("交通", CategoryFilterDataSourceType.Title, CategoryFilterConditionType.Include),
                                    condition("Suica", CategoryFilterDataSourceType.ServiceName, CategoryFilterConditionType.Include),
                                ),
                            ),
                        ),
                    ) { type ->
                        when (type) {
                            CategoryFilterDataSourceType.Title -> "食費"
                            CategoryFilterDataSourceType.ServiceName -> "クレジットカード"
                            else -> null
                        }
                    }
                    result.shouldBe(null)
                }
            }

            describe("フィルター評価の特殊ケース") {
                it("条件が空のフィルターはスキップして次のフィルターを評価する") {
                    val result = evaluateCategoryFilters(
                        filters = listOf(
                            filter(
                                orderNumber = 1,
                                operator = CategoryFilterOperator.AND,
                                subCategoryId = subCategoryId1,
                                conditions = listOf(),
                            ),
                            filter(
                                orderNumber = 2,
                                operator = CategoryFilterOperator.AND,
                                subCategoryId = subCategoryId2,
                                conditions = listOf(
                                    condition("交通", CategoryFilterDataSourceType.Title, CategoryFilterConditionType.Include),
                                ),
                            ),
                        ),
                    ) { type ->
                        when (type) {
                            CategoryFilterDataSourceType.Title -> "交通費"
                            else -> null
                        }
                    }
                    result.shouldBe(subCategoryId2)
                }

                it("subCategoryId が null のフィルターがマッチしてもnullを返す") {
                    val result = evaluateCategoryFilters(
                        filters = listOf(
                            filter(
                                orderNumber = 1,
                                operator = CategoryFilterOperator.AND,
                                subCategoryId = null,
                                conditions = listOf(
                                    condition("交通", CategoryFilterDataSourceType.Title, CategoryFilterConditionType.Include),
                                ),
                            ),
                        ),
                    ) { type ->
                        when (type) {
                            CategoryFilterDataSourceType.Title -> "交通費"
                            else -> null
                        }
                    }
                    result.shouldBe(null)
                }

                it("orderNumber が小さいフィルターが優先される") {
                    val result = evaluateCategoryFilters(
                        filters = listOf(
                            filter(
                                orderNumber = 2,
                                operator = CategoryFilterOperator.AND,
                                subCategoryId = subCategoryId2,
                                conditions = listOf(
                                    condition("交通", CategoryFilterDataSourceType.Title, CategoryFilterConditionType.Include),
                                ),
                            ),
                            filter(
                                orderNumber = 1,
                                operator = CategoryFilterOperator.AND,
                                subCategoryId = subCategoryId1,
                                conditions = listOf(
                                    condition("交通", CategoryFilterDataSourceType.Title, CategoryFilterConditionType.Include),
                                ),
                            ),
                        ),
                    ) { type ->
                        when (type) {
                            CategoryFilterDataSourceType.Title -> "交通費"
                            else -> null
                        }
                    }
                    result.shouldBe(subCategoryId1)
                }

                it("フィルターが空の場合はnullを返す") {
                    val result = evaluateCategoryFilters(
                        filters = listOf(),
                    ) { null }
                    result.shouldBe(null)
                }
            }
        }
    },
)
