package net.matsudamper.money.frontend.common.base.nav.user

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual

public class ScreenNavControllerImplTest : DescribeSpec(
    {
        fun createStructure(text: String, groupId: Any): IScreenStructure {
            return object : IScreenStructure {
                override val direction: Direction = object : Direction {
                    override val placeholderUrl: String = ""
                    override val title: String = text
                }
                override val stackGroupId: Any = groupId

                override fun toString(): String {
                    return direction.title
                }
            }
        }

        val rootA = createStructure("A1", "A")
        val rootB = createStructure("B1", "B")
        val rootC = createStructure("C1", "C")
        describe("navigationのRootでの入れ替えができるかを確認する") {
            context("先頭のグループへの入れ替え") {
                it("入れ替えができる") {
                    val controller = ScreenNavControllerImpl(initial = rootA).apply {
                        navigate(
                            navigation = createStructure("A2", "A"),
                            savedState = false,
                            isRoot = false,
                        )
                        navigate(
                            navigation = rootB,
                            savedState = false,
                            isRoot = true,
                        )
                        navigate(
                            navigation = rootA,
                            savedState = false,
                            isRoot = true,
                        )
                    }
                    controller.backstackEntries.map { it.structure.direction.title }
                        .shouldBeEqual(
                            listOf(
                                "B1",
                                "A1",
                                "A2",
                            ),
                        )
                }
            }
            context("中間のグループへの入れ替え") {
                it("入れ替えができる") {
                    val controller = ScreenNavControllerImpl(initial = rootA).apply {
                        navigate(
                            navigation = createStructure("A2", "A"),
                            savedState = false,
                            isRoot = false,
                        )
                        navigate(
                            navigation = rootB,
                            savedState = false,
                            isRoot = true,
                        )
                        navigate(
                            navigation = createStructure("B2", "B"),
                            savedState = false,
                            isRoot = false,
                        )
                        navigate(
                            navigation = rootC,
                            savedState = false,
                            isRoot = true,
                        )
                        navigate(
                            navigation = createStructure("C2", "C"),
                            savedState = false,
                            isRoot = false,
                        )
                        navigate(
                            navigation = rootB,
                            savedState = false,
                            isRoot = true,
                        )
                    }
                    controller.backstackEntries.map { it.structure.direction.title }
                        .shouldBeEqual(
                            listOf(
                                "A1",
                                "A2",
                                "C1",
                                "C2",
                                "B1",
                                "B2",
                            ),
                        )
                }
            }
        }
    },
)
