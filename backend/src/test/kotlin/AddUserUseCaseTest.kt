import io.kotest.core.spec.style.DescribeSpec
import net.matsudamper.money.backend.app.interfaces.AdminRepository
import net.matsudamper.money.backend.logic.AddUserUseCase
import net.matsudamper.money.backend.logic.IPasswordManager

class AddUserUseCaseTest : DescribeSpec(
    {
        describe("パスワードのバリデーション") {
            context("記号が含まれている128文字") {
                it("パスワードが通る") {
                    val operation = FakeAdminRepository()
                    val useCase = AddUserUseCase(
                        adminRepository = operation,
                        passwordManager = object : IPasswordManager {
                            override fun create(password: String, keyByteLength: Int, iterationCount: Int, saltByteLength: Int, algorithm: IPasswordManager.Algorithm): IPasswordManager.CreateResult {
                                return IPasswordManager.CreateResult(
                                    salt = ByteArray(0),
                                    algorithm = "",
                                    iterationCount = 0,
                                    keyLength = 0,
                                    hashedPassword = password,
                                )
                            }

                            override fun create(password: String, keyByteLength: Int, iterationCount: Int, salt: ByteArray, algorithm: IPasswordManager.Algorithm): IPasswordManager.CreateResult {
                                TODO("Not yet implemented")
                            }

                            override fun getHashedPassword(password: String, salt: ByteArray, iterationCount: Int, keyLength: Int, algorithm: IPasswordManager.Algorithm): ByteArray {
                                return ByteArray(0)
                            }
                        },
                    )
                    val password = "Vovo@nHsP&YlL!XQWwgo8QL6fafpHVx!u#jtUEwKNKus1kxmx*f0*f1Z6B&bs#5Or\$MN56#xY3!ejzlJxizTUGxXpsgmN%JvG4V*HHzJx\$WrZTEA%Tr^2sx@YF1X*yN4"
                    when (val result = useCase.addUser("test", password)) {
                        is AddUserUseCase.Result.Failure -> {
                            val errorText = result.errors.joinToString {
                                when (it) {
                                    AddUserUseCase.Result.Errors.InternalServerError,
                                    AddUserUseCase.Result.Errors.PasswordLength,
                                    AddUserUseCase.Result.Errors.UserNameLength,
                                    AddUserUseCase.Result.Errors.UserNameValidation,
                                    -> it::class.java.name

                                    is AddUserUseCase.Result.Errors.PasswordValidation -> {
                                        "error char: ${it.errorChar.joinToString(",")}"
                                    }
                                }
                            }
                            throw IllegalStateException(errorText)
                        }

                        is AddUserUseCase.Result.Success -> Unit
                    }
                    check(operation.addUserCallCount == 1) {
                        "addUser should be called exactly once"
                    }
                }
            }
        }
    },
)

private class FakeAdminRepository : AdminRepository {
    var addUserCallCount = 0
        private set

    override fun addUser(
        userName: String,
        hashedPassword: String,
        algorithmName: String,
        salt: ByteArray,
        iterationCount: Int,
        keyLength: Int,
    ): AdminRepository.AddUserResult {
        addUserCallCount++
        return AdminRepository.AddUserResult.Success
    }
}
