
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import net.matsudamper.money.backend.lib.ChallengeModel

private val challengeSecretByteArray = "xzrdmy8S5hX5iCuh&#\$FR^NqWxeU8B6*3Uw9WvNsmSVNzKz9^L#zwrfY9a^Vk&UzHjfbqf*G!vJDCdYQnczJt9&ZQfjRTKU6MQU!wWnKrY5e8q4Xbq*DE&oo7dobpds@".toByteArray()

class ChallengeModelTest : DescribeSpec(
    {
        describe("challengeのcreateとvalidateを確認する") {
            val challengeModel = ChallengeModel(
                challengeSecretByteArray = challengeSecretByteArray,
            )
            it("challengeのcreateとvalidateができる") {
                val challenge = challengeModel.generateChallenge()
                challengeModel.validateChallenge(challenge)
                    .shouldBeTrue()
            }
            it("空のテキストでvalidateが通らない") {
                challengeModel.validateChallenge(
                    ".",
                ).shouldBeFalse()
            }
        }
    },
)
