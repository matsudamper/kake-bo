import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import net.matsudamper.money.backend.logic.CategoryColorValidator

class CategoryColorValidatorTest : DescribeSpec({
    describe("カテゴリ色のバリデーション") {
        it("nullは許可される") {
            CategoryColorValidator.isValid(null) shouldBe true
        }

        it("#から始まる6桁16進数は許可される") {
            CategoryColorValidator.isValid("#A1b2C3") shouldBe true
        }

        it("#なしは拒否される") {
            CategoryColorValidator.isValid("A1b2C3") shouldBe false
        }

        it("桁数不足は拒否される") {
            CategoryColorValidator.isValid("#ABC") shouldBe false
        }

        it("16進数以外を含む場合は拒否される") {
            CategoryColorValidator.isValid("#ZZZZZZ") shouldBe false
        }
    }
})
