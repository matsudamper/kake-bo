package net.matsudamper.money.backend.graphql.resolver

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import net.matsudamper.money.element.ImageId

class AdminUnlinkedImagesCursorTest : DescribeSpec({
    describe("AdminUnlinkedImagesCursor") {
        it("文字列化したcursorを復元できる") {
            val cursor = AdminUnlinkedImagesCursor(ImageId(123))

            AdminUnlinkedImagesCursor.fromString(cursor.toCursorString()) shouldBe cursor
        }
    }
})
