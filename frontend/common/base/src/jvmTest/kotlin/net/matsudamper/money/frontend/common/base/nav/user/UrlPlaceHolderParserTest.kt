package net.matsudamper.money.frontend.common.base.nav.user

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

public class UrlPlaceHolderParserTest : DescribeSpec(
    {
        describe("途中でPlaceHolderがあるURLと無いURLのパース") {
            val url1 = "hoge/{id}".toDirectionUrl()
            val url2 = "hoge/{id}/fuga".toDirectionUrl()
            val value = 1000
            run {
                val testUrl = url2.placeholderUrl.replace("{id}", value.toString())
                it("${testUrl}で${url2}とid=${value}が取得できる") {
                    val result =
                        UrlPlaceHolderParser(listOf(url1, url2))
                            .parse(testUrl)

                    result.screen.shouldBe(url2)
                    result.pathParams.shouldBe(mapOf("id" to value.toString()))
                }
            }
            run {
                val testUrl = url1.placeholderUrl.replace("{id}", value.toString())
                it("${testUrl}で${url1}とid=${value}が取得できる") {
                    val result =
                        UrlPlaceHolderParser(listOf(url1, url2))
                            .parse(testUrl)
                    result.screen.shouldBe(url1)
                    result.pathParams.shouldBe(mapOf("id" to value.toString()))
                }
            }
        }
    },
)

private fun String.toDirectionUrl(): DirectionUrl {
    return object : DirectionUrl {
        override val placeholderUrl: String = this@toDirectionUrl

        override fun toString(): String {
            return "DirectionUrl(placeholderUrl='$placeholderUrl')"
        }
    }
}
