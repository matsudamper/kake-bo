package net.matsudamper.money.backend.app.interfaces

interface GeminiGateway {
    /**
     * @param thinking trueの場合はモデルに思考させてから回答させる。falseの場合はモデルのデフォルトに任せる
     * @param responseSchemaJson Gemini APIのresponseSchemaに渡すJSON
     * @return 生成されたJSON文字列
     */
    fun generateJson(
        apiKey: String,
        model: Model,
        thinking: Boolean,
        systemInstruction: String,
        userText: String,
        responseSchemaJson: String,
    ): Result<String>

    enum class Model {
        FlashLite,
        Flash,
    }
}
