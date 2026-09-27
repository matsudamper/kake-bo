package net.matsudamper.money.backend.app.interfaces

interface GeminiGateway {
    /**
     * @param responseSchemaJson Gemini APIのresponseSchemaに渡すJSON
     * @return 生成されたJSON文字列
     */
    fun generateJson(
        apiKey: String,
        systemInstruction: String,
        userText: String,
        responseSchemaJson: String,
    ): Result<String>
}
