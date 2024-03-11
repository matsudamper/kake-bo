import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.matsudamper.money.backend.app.interfaces.AdminRepository
import net.matsudamper.money.backend.logic.AddUserUseCase

class AddUserUseCaseTest : DescribeSpec(
    {
        describe("パスワードのバリデーション") {
            context("記号が含まれている128文字") {
                it("パスワードが通る") {
                    val operation: AdminRepository =
                        mockk {
                            every { addUser(any(), any()) } answers {
                                AdminRepository.AddUserResult.Success
                            }
                        }
                    val useCase =
                        AddUserUseCase(
                            adminRepository = operation,
                        )
                    val password = "Vovo@nHsP&YlL!XQWwgo8QL6fafpHVx!u#jtUEwKNKus1kxmx*f0*f1Z6B&bs#5Or\$MN56#xY3!ejzlJxizTUGxXpsgmN%JvG4V*HHzJx\$WrZTEA%Tr^2sx@YF1X*yN4"
                    val result = useCase.addUser("test", password)
                    when (result) {
                        is AddUserUseCase.Result.Failure -> {
                            val errorText =
                                result.errors.joinToString {
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
                    verify { operation.addUser(any(), any()) }
                }
            }
        }
    },
)
