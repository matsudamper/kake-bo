package net.matsudamper.money.backend.graphql.resolver.mutation

import java.time.DateTimeException
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class SettingsMutationTimezoneTest : DescribeSpec({
    describe("parseTimezoneOffset") {
        context("有効な値") {
            it("UTC+00:00（0分）を解析できる") {
                val result = SettingsMutationResolverResolverImpl.parseTimezoneOffset(0)
                result.isSuccess shouldBe true
                result.getOrNull()?.totalSeconds shouldBe 0
            }

            it("UTC+09:00（540分）を解析できる") {
                val result = SettingsMutationResolverResolverImpl.parseTimezoneOffset(540)
                result.isSuccess shouldBe true
                result.getOrNull()?.totalSeconds shouldBe 540 * 60
            }

            it("UTC-05:00（-300分）を解析できる") {
                val result = SettingsMutationResolverResolverImpl.parseTimezoneOffset(-300)
                result.isSuccess shouldBe true
                result.getOrNull()?.totalSeconds shouldBe -300 * 60
            }

            it("最大値 UTC+14:00（840分）を解析できる") {
                val result = SettingsMutationResolverResolverImpl.parseTimezoneOffset(840)
                result.isSuccess shouldBe true
            }

            it("最小値 UTC-12:00（-720分）を解析できる") {
                val result = SettingsMutationResolverResolverImpl.parseTimezoneOffset(-720)
                result.isSuccess shouldBe true
            }
        }

        context("Intオーバーフロー") {
            it("Int.MAX_VALUEはArithmeticExceptionで失敗する") {
                val result = SettingsMutationResolverResolverImpl.parseTimezoneOffset(Int.MAX_VALUE)
                result.isFailure shouldBe true
                result.exceptionOrNull().shouldBeInstanceOf<ArithmeticException>()
            }

            it("Int.MIN_VALUEはArithmeticExceptionで失敗する") {
                val result = SettingsMutationResolverResolverImpl.parseTimezoneOffset(Int.MIN_VALUE)
                result.isFailure shouldBe true
                result.exceptionOrNull().shouldBeInstanceOf<ArithmeticException>()
            }
        }

        context("範囲外の値") {
            it("UTC+14:01（841分）はDateTimeExceptionで失敗する") {
                val result = SettingsMutationResolverResolverImpl.parseTimezoneOffset(841)
                result.isFailure shouldBe true
                result.exceptionOrNull().shouldBeInstanceOf<DateTimeException>()
            }

            it("UTC-12:01（-721分）はDateTimeExceptionで失敗する") {
                val result = SettingsMutationResolverResolverImpl.parseTimezoneOffset(-721)
                result.isFailure shouldBe true
                result.exceptionOrNull().shouldBeInstanceOf<DateTimeException>()
            }
        }
    }
})
