import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import net.matsudamper.money.backend.logic.ColorValidator

class ColorValidatorTest : DescribeSpec({
    describe("カテゴリ色のバリデーション") {
        it("nullは許可される") {
            ColorValidator.isValid(null) shouldBe true
        }

        it("#から始まる6桁16進数は許可される") {
            ColorValidator.isValid("#A1b2C3") shouldBe true
        }

        it("#なしは拒否される") {
            ColorValidator.isValid("A1b2C3") shouldBe false
        }

        it("桁数不足は拒否される") {
            ColorValidator.isValid("#ABC") shouldBe false
        }

        it("16進数以外を含む場合は拒否される") {
            ColorValidator.isValid("#ZZZZZZ") shouldBe false
        }
    }
})
