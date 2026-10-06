package net.matsudamper.money.backend.feature.gemini

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import net.matsudamper.money.backend.app.interfaces.GeminiGateway

class GeminiGatewayImpl : GeminiGateway {
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    override fun generateJson(
        apiKey: String,
        systemInstruction: String,
        userText: String,
        responseSchemaJson: String,
    ): Result<String> {
        return runCatching {
            val requestBody = createRequestBody(
                systemInstruction = systemInstruction,
                userText = userText,
                responseSchemaJson = responseSchemaJson,
            )
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$API_BASE_URL/models/$MODEL:generateContent"))
                // parseImportedMailWithAi の @longRunning(timeoutSeconds: 90) より先に切れるようにして、結果をエラーとして返せるようにする
                .timeout(Duration.ofSeconds(80))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) {
                throw GeminiApiException(
                    "Gemini APIがエラーを返しました(${response.statusCode()}): ${extractErrorMessage(response.body())}",
                )
            }
            extractText(response.body())
        }
    }

    private fun createRequestBody(
        systemInstruction: String,
        userText: String,
        responseSchemaJson: String,
    ): JsonObject {
        return buildJsonObject {
            putJsonObject("systemInstruction") {
                putJsonArray("parts") {
                    addJsonObject { put("text", systemInstruction) }
                }
            }
            putJsonArray("contents") {
                addJsonObject {
                    put("role", "user")
                    putJsonArray("parts") {
                        addJsonObject { put("text", userText) }
                    }
                }
            }
            putJsonObject("generationConfig") {
                put("temperature", 0)
                put("responseMimeType", "application/json")
                put("responseSchema", json.parseToJsonElement(responseSchemaJson))
            }
        }
    }

    private fun extractText(body: String): String {
        val response = json.decodeFromString<GenerateContentResponse>(body)
        val candidate = response.candidates.firstOrNull()
            ?: throw GeminiApiException("Gemini APIの応答に候補がありませんでした: ${response.promptFeedback?.blockReason.orEmpty()}")
        val text = candidate.content?.parts.orEmpty().joinToString("") { it.text.orEmpty() }
        if (text.isBlank()) {
            throw GeminiApiException("Gemini APIの応答が空でした: ${candidate.finishReason.orEmpty()}")
        }
        return text
    }

    private fun extractErrorMessage(body: String): String {
        return runCatching { json.decodeFromString<ErrorResponse>(body).error?.message }
            .getOrNull()
            ?: body.take(200)
    }

    class GeminiApiException(message: String) : Exception(message)

    @Serializable
    private data class GenerateContentResponse(
        @SerialName("candidates") val candidates: List<Candidate> = listOf(),
        @SerialName("promptFeedback") val promptFeedback: PromptFeedback? = null,
    )

    @Serializable
    private data class Candidate(
        @SerialName("content") val content: Content? = null,
        @SerialName("finishReason") val finishReason: String? = null,
    )

    @Serializable
    private data class Content(
        @SerialName("parts") val parts: List<Part> = listOf(),
    )

    @Serializable
    private data class Part(
        @SerialName("text") val text: String? = null,
    )

    @Serializable
    private data class PromptFeedback(
        @SerialName("blockReason") val blockReason: String? = null,
    )

    @Serializable
    private data class ErrorResponse(
        @SerialName("error") val error: ErrorBody? = null,
    )

    @Serializable
    private data class ErrorBody(
        @SerialName("message") val message: String? = null,
    )

    private companion object {
        private const val API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        private const val MODEL = "gemini-2.5-flash"
    }
}
