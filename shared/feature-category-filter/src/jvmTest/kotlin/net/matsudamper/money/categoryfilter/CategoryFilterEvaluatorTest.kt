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
            matchers: List<CategoryFilterMatcher>,
            matchExpression: String? = null,
        ) = CategoryFilter(
            orderNumber = orderNumber,
            operator = operator,
            matchExpression = matchExpression,
            subCategoryId = subCategoryId,
            matchers = matchers,
        )

        fun matcher(
            text: String,
            dataSourceType: CategoryFilterDataSourceType,
            matcherType: CategoryFilterMatcherType,
            matcherKey: String = "id1",
        ) = CategoryFilterMatcher(
            matcherKey = matcherKey,
            text = text,
            dataSourceType = dataSourceType,
            matcherType = matcherType,
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
                                matchers = listOf(
                                    matcher("コンビニ", CategoryFilterDataSourceType.Title, CategoryFilterMatcherType.Include),
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
                                matchers = listOf(
                                    matcher("コンビニ", CategoryFilterDataSourceType.Title, CategoryFilterMatcherType.Include),
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
                                matchers = listOf(
                                    matcher("コンビニ", CategoryFilterDataSourceType.Title, CategoryFilterMatcherType.NotInclude),
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
                                matchers = listOf(
                                    matcher("コンビニ", CategoryFilterDataSourceType.Title, CategoryFilterMatcherType.Equal),
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
                                matchers = listOf(
                                    matcher("コンビニ", CategoryFilterDataSourceType.Title, CategoryFilterMatcherType.Equal),
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
                                matchers = listOf(
                                    matcher("コンビニ", CategoryFilterDataSourceType.Title, CategoryFilterMatcherType.NotEqual),
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
                                matchers = listOf(
                                    matcher("モバイルSuica", CategoryFilterDataSourceType.ServiceName, CategoryFilterMatcherType.Include),
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
                                matchers = listOf(
                                    matcher("any", CategoryFilterDataSourceType.MailTitle, CategoryFilterMatcherType.NotInclude),
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
                                matchers = listOf(
                                    matcher("交通", CategoryFilterDataSourceType.Title, CategoryFilterMatcherType.Include),
                                    matcher("Suica", CategoryFilterDataSourceType.ServiceName, CategoryFilterMatcherType.Include),
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
                                matchers = listOf(
                                    matcher("交通", CategoryFilterDataSourceType.Title, CategoryFilterMatcherType.Include),
                                    matcher("Suica", CategoryFilterDataSourceType.ServiceName, CategoryFilterMatcherType.Include),
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
                                matchers = listOf(
                                    matcher("交通", CategoryFilterDataSourceType.Title, CategoryFilterMatcherType.Include),
                                    matcher("Suica", CategoryFilterDataSourceType.ServiceName, CategoryFilterMatcherType.Include),
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
                                matchers = listOf(
                                    matcher("交通", CategoryFilterDataSourceType.Title, CategoryFilterMatcherType.Include),
                                    matcher("Suica", CategoryFilterDataSourceType.ServiceName, CategoryFilterMatcherType.Include),
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

            describe("式") {
                val matchers = listOf(
                    matcher("カード", CategoryFilterDataSourceType.ServiceName, CategoryFilterMatcherType.Include, matcherKey = "card"),
                    matcher("店舗A", CategoryFilterDataSourceType.Title, CategoryFilterMatcherType.Include, matcherKey = "shop-a"),
                    matcher("店舗B", CategoryFilterDataSourceType.Title, CategoryFilterMatcherType.Include, matcherKey = "shop_b"),
                )

                fun evaluate(matchExpression: String?, serviceName: String, title: String) = evaluateCategoryFilters(
                    filters = listOf(
                        filter(
                            orderNumber = 1,
                            operator = CategoryFilterOperator.OR,
                            subCategoryId = subCategoryId1,
                            matchers = matchers,
                            matchExpression = matchExpression,
                        ),
                    ),
                ) { type ->
                    when (type) {
                        CategoryFilterDataSourceType.ServiceName -> serviceName
                        CategoryFilterDataSourceType.Title -> title
                        else -> null
                    }
                }

                it("式が一致する場合はマッチする") {
                    evaluate("card AND (shop-a OR shop_b)", serviceName = "カード", title = "店舗Bで購入")
                        .shouldBe(subCategoryId1)
                }

                it("式が一致しない場合はマッチしない") {
                    evaluate("card AND (shop-a OR shop_b)", serviceName = "銀行", title = "店舗Bで購入")
                        .shouldBe(null)
                }

                it("式がnullの場合はoperatorで評価する") {
                    evaluate(null, serviceName = "銀行", title = "店舗Bで購入")
                        .shouldBe(subCategoryId1)
                }

                it("式が空白のみの場合はoperatorで評価する") {
                    evaluate(" \n ", serviceName = "銀行", title = "店舗Bで購入")
                        .shouldBe(subCategoryId1)
                }

                it("式の文法が壊れている場合はマッチしない") {
                    evaluate("card AND", serviceName = "カード", title = "店舗Aで購入")
                        .shouldBe(null)
                }

                it("存在しないmatcher_keyを参照している場合はマッチしない") {
                    evaluate("card OR unknown", serviceName = "カード", title = "店舗Aで購入")
                        .shouldBe(null)
                }

                it("matcherが無くても式がTRUEならマッチする") {
                    evaluateCategoryFilters(
                        filters = listOf(
                            filter(
                                orderNumber = 1,
                                operator = CategoryFilterOperator.AND,
                                subCategoryId = subCategoryId1,
                                matchers = listOf(),
                                matchExpression = "TRUE",
                            ),
                        ),
                    ) { null }.shouldBe(subCategoryId1)
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
                                matchers = listOf(),
                            ),
                            filter(
                                orderNumber = 2,
                                operator = CategoryFilterOperator.AND,
                                subCategoryId = subCategoryId2,
                                matchers = listOf(
                                    matcher("交通", CategoryFilterDataSourceType.Title, CategoryFilterMatcherType.Include),
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
                                matchers = listOf(
                                    matcher("交通", CategoryFilterDataSourceType.Title, CategoryFilterMatcherType.Include),
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
                                matchers = listOf(
                                    matcher("交通", CategoryFilterDataSourceType.Title, CategoryFilterMatcherType.Include),
                                ),
                            ),
                            filter(
                                orderNumber = 1,
                                operator = CategoryFilterOperator.AND,
                                subCategoryId = subCategoryId1,
                                matchers = listOf(
                                    matcher("交通", CategoryFilterDataSourceType.Title, CategoryFilterMatcherType.Include),
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
