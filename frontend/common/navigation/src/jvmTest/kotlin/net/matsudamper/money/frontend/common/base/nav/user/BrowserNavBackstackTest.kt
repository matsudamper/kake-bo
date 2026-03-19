package net.matsudamper.money.frontend.common.base.nav.user

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf

public class BrowserNavBackstackTest : DescribeSpec(
    {
        fun createScreen(text: String, groupId: String? = null): IScreenStructure {
            return object : IScreenStructure {
                override val direction: Direction = object : Direction {
                    override val placeholderUrl: String = "/$text"
                    override val title: String = text
                }
                override val stackGroupId: String? = groupId
                override val sameScreenId: String = text

                override fun toString(): String = direction.title
            }
        }

        val home = ScreenStructure.Root.Settings.Root
        val screenA = createScreen("A")
        val screenB = createScreen("B")
        val screenC = createScreen("C")

        describe("navigate") {
            context("通常の遷移") {
                it("異なるsameScreenIdの場合はPushが返りエントリが追加される") {
                    val backstack = BrowserNavBackstack(home)
                    val result = backstack.navigate(screenA)

                    result.shouldBeInstanceOf<BrowserNavBackstack.NavigationResult.Push>()
                    backstack.entries.map { it.sameScreenId }
                        .shouldBeEqual(listOf(home.sameScreenId, screenA.sameScreenId))
                }

                it("同じsameScreenIdの場合はReplaceが返り末尾エントリが置き換わる") {
                    val screenA2 = createScreen("A")
                    val backstack = BrowserNavBackstack(home)
                    backstack.navigate(screenA)
                    val result = backstack.navigate(screenA2)

                    result.shouldBeInstanceOf<BrowserNavBackstack.NavigationResult.Replace>()
                    backstack.entries.map { it.sameScreenId }
                        .shouldBeEqual(listOf(home.sameScreenId, screenA.sameScreenId))
                }
            }

            context("stackGroupIdによるグループ管理") {
                it("先頭グループへの切り替えでグループが末尾に移動しPushが返る") {
                    val groupA1 = createScreen("A1", "A")
                    val groupA2 = createScreen("A2", "A")
                    val groupB1 = createScreen("B1", "B")

                    val backstack = BrowserNavBackstack(groupA1)
                    backstack.navigate(createScreen("A2-init", "A"))
                    backstack.navigate(groupB1)
                    val result = backstack.navigate(groupA1)

                    result.shouldBeInstanceOf<BrowserNavBackstack.NavigationResult.Push>()
                    backstack.entries.map { it.direction.title }
                        .shouldBeEqual(listOf("B1", "A1", "A2-init"))
                }

                it("中間グループへの切り替えで正しくグループが移動する") {
                    val groupA1 = createScreen("A1", "A")
                    val groupA2 = createScreen("A2", "A")
                    val groupB1 = createScreen("B1", "B")
                    val groupB2 = createScreen("B2", "B")
                    val groupC1 = createScreen("C1", "C")
                    val groupC2 = createScreen("C2", "C")

                    val backstack = BrowserNavBackstack(groupA1)
                    backstack.navigate(groupA2)
                    backstack.navigate(groupB1)
                    backstack.navigate(groupB2)
                    backstack.navigate(groupC1)
                    backstack.navigate(groupC2)
                    backstack.navigate(groupB1)

                    backstack.entries.map { it.direction.title }
                        .shouldBeEqual(listOf("A1", "A2", "C1", "C2", "B1", "B2"))
                }
            }
        }

        describe("navigateReplace") {
            it("末尾エントリが置き換わる") {
                val backstack = BrowserNavBackstack(home)
                backstack.navigate(screenA)
                backstack.navigateReplace(screenB)

                backstack.entries.map { it.sameScreenId }
                    .shouldBeEqual(listOf(home.sameScreenId, screenB.sameScreenId))
            }

            it("エントリが1件の場合も置き換わる") {
                val backstack = BrowserNavBackstack(home)
                backstack.navigateReplace(screenA)

                backstack.entries.map { it.sameScreenId }
                    .shouldBeEqual(listOf(screenA.sameScreenId))
            }
        }

        describe("handlePopState") {
            context("戻る（delta < 0）") {
                it("1ステップ戻ると末尾エントリが1件削除される") {
                    val backstack = BrowserNavBackstack(home)
                    backstack.navigate(screenA)
                    backstack.navigate(screenB)
                    backstack.handlePopState(delta = -1, currentScreen = screenA)

                    backstack.entries.map { it.sameScreenId }
                        .shouldBeEqual(listOf(home.sameScreenId, screenA.sameScreenId))
                }

                it("2ステップ戻ると末尾エントリが2件削除される") {
                    val backstack = BrowserNavBackstack(home)
                    backstack.navigate(screenA)
                    backstack.navigate(screenB)
                    backstack.navigate(screenC)
                    backstack.handlePopState(delta = -2, currentScreen = screenA)

                    backstack.entries.map { it.sameScreenId }
                        .shouldBeEqual(listOf(home.sameScreenId, screenA.sameScreenId))
                }

                it("エントリ数を超えるステップ数でも最初の1件が残る") {
                    val backstack = BrowserNavBackstack(home)
                    backstack.navigate(screenA)
                    backstack.handlePopState(delta = -10, currentScreen = home)

                    backstack.entries.map { it.sameScreenId }
                        .shouldBeEqual(listOf(home.sameScreenId))
                }
            }

            context("進む（delta > 0）") {
                it("currentScreenがエントリに追加される") {
                    val backstack = BrowserNavBackstack(home)
                    backstack.navigate(screenA)
                    backstack.handlePopState(delta = -1, currentScreen = home)
                    backstack.handlePopState(delta = 1, currentScreen = screenA)

                    backstack.entries.map { it.sameScreenId }
                        .shouldBeEqual(listOf(home.sameScreenId, screenA.sameScreenId))
                }
            }

            context("delta == 0") {
                it("エントリは変化しない") {
                    val backstack = BrowserNavBackstack(home)
                    backstack.navigate(screenA)
                    backstack.handlePopState(delta = 0, currentScreen = screenB)

                    backstack.entries.map { it.sameScreenId }
                        .shouldBeEqual(listOf(home.sameScreenId, screenA.sameScreenId))
                }
            }
        }

        describe("stepsToHome") {
            it("Rootが末尾にいる場合は0が返る") {
                val backstack = BrowserNavBackstack(home)
                backstack.stepsToHome().shouldBeEqual(0)
            }

            it("RootのあとにN画面積まれている場合は-Nが返る") {
                val backstack = BrowserNavBackstack(home)
                backstack.navigate(screenA)
                backstack.navigate(screenB)
                backstack.navigate(screenC)
                backstack.stepsToHome().shouldBeEqual(-3)
            }

            it("中間にRootがある場合は最後のRootからの距離を返す") {
                val home2 = ScreenStructure.Root.Settings.Api
                val backstack = BrowserNavBackstack(home)
                backstack.navigate(screenA)
                backstack.navigate(home2)
                backstack.navigate(screenB)
                backstack.stepsToHome().shouldBeEqual(-1)
            }

            it("Rootが存在しない場合は先頭まで戻るステップ数を返す") {
                val backstack = BrowserNavBackstack(screenA)
                backstack.navigate(screenB)
                backstack.navigate(screenC)
                backstack.stepsToHome().shouldBeEqual(-2)
            }
        }
    },
)
