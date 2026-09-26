package net.matsudamper.money.frontend.common.base.nav.user

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class BrowserScreenRouteParserTest : DescribeSpec(
    {
        describe("ブラウザURLルーティング") {
            it("通知系URLはブラウザルートとして解決しない") {
                listOf(
                    "/add/notification-usage" to "",
                    "/add/notification-usage/filters" to "",
                    "/add/notification-usage/debug" to "",
                    "/add/notification-usage/detail" to "?notification_usage_key=notification-key",
                ).forEach { (pathname, query) ->
                    parseBrowserScreenStructure(pathname = pathname, query = query) shouldBe ScreenStructure.NotFound
                }
            }

            it("既存の追加系URLはブラウザルートとして解決する") {
                parseBrowserScreenStructure(pathname = "/add", query = "")
                    .shouldBe(ScreenStructure.Root.Add.Root)
                parseBrowserScreenStructure(pathname = "/add/presets", query = "")
                    .shouldBe(ScreenStructure.Root.Add.Preset)
                parseBrowserScreenStructure(pathname = "/add/money-usage", query = "")
                    .shouldBe(ScreenStructure.AddMoneyUsage())
            }
        }
    },
)
