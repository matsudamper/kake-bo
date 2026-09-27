package net.matsudamper.money.backend.feature.aimailparser

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class GeminiMailParser {
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(
        apiKey: String,
        input: AiMailParseInput,
    ): Result<List<AiParsedUsage>> {
        return runCatching {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$API_BASE_URL/models/$MODEL:generateContent"))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(createRequestBody(input).toString()))
                .build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) {
                throw GeminiApiException(
                    "Gemini APIがエラーを返しました(${response.statusCode()}): ${extractErrorMessage(response.body())}",
                )
            }
            parseResponse(response.body())
        }
    }

    private fun createRequestBody(input: AiMailParseInput): JsonObject {
        return buildJsonObject {
            putJsonObject("systemInstruction") {
                putJsonArray("parts") {
                    addJsonObject { put("text", SYSTEM_INSTRUCTION) }
                }
            }
            putJsonArray("contents") {
                addJsonObject {
                    put("role", "user")
                    putJsonArray("parts") {
                        addJsonObject {
                            put("text", MailBodyTextBuilder.build(input = input, maxBodyLength = MAX_BODY_LENGTH))
                        }
                    }
                }
            }
            putJsonObject("generationConfig") {
                put("temperature", 0)
                put("responseMimeType", "application/json")
                put("responseSchema", RESPONSE_SCHEMA)
            }
        }
    }

    private fun parseResponse(body: String): List<AiParsedUsage> {
        val response = json.decodeFromString<GenerateContentResponse>(body)
        val candidate = response.candidates.firstOrNull()
            ?: throw GeminiApiException("Gemini APIの応答に候補がありませんでした: ${response.promptFeedback?.blockReason.orEmpty()}")
        val text = candidate.content?.parts.orEmpty().joinToString("") { it.text.orEmpty() }
        if (text.isBlank()) {
            throw GeminiApiException("Gemini APIの応答が空でした: ${candidate.finishReason.orEmpty()}")
        }
        val result = json.decodeFromString<ParseResult>(text)
        return result.usages
            .filter { it.title.isNotBlank() }
            .map { usage ->
                AiParsedUsage(
                    title = usage.title.trim(),
                    description = usage.description.orEmpty().trim(),
                    amount = usage.amount,
                    dateTime = usage.dateTime?.let { parseDateTime(it) },
                )
            }
    }

    private fun parseDateTime(text: String): LocalDateTime? {
        return try {
            LocalDateTime.parse(text)
        } catch (_: DateTimeParseException) {
            null
        }
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

    @Serializable
    private data class ParseResult(
        @SerialName("usages") val usages: List<ParsedUsage> = listOf(),
    )

    @Serializable
    private data class ParsedUsage(
        @SerialName("title") val title: String,
        @SerialName("description") val description: String? = null,
        @SerialName("amount") val amount: Int? = null,
        @SerialName("dateTime") val dateTime: String? = null,
    )

    private companion object {
        private const val API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        private const val MODEL = "gemini-2.5-flash"

        // 長い広告メールでトークンを使い過ぎないように本文を切り詰める
        private const val MAX_BODY_LENGTH = 30_000

        private val SYSTEM_INSTRUCTION = """
            あなたは家計簿アプリのためにメールから支出を抽出するアシスタントです。
            与えられたメールから、実際に支払いや購入、利用が発生した項目を抽出してください。
            - 1通のメールに複数の支払いがある場合は、支払いごとに分けて出力してください。
            - 同じ注文の商品明細は1つの支出にまとめ、商品名などは description に記載してください。
            - title は家計簿に表示する短い名前にしてください。店名やサービス名を含めてください。
            - amount は日本円の支払金額を整数で入れてください。返金の場合は負の値にしてください。不明な場合は null にしてください。
            - dateTime は利用日時を yyyy-MM-ddTHH:mm:ss 形式で入れてください。時刻が不明な場合は 00:00:00、日付が不明な場合は null にしてください。年が書かれていない場合は受信日時から補ってください。
            - 広告、キャンペーン案内、ポイント付与のお知らせなど支払いが発生していないメールの場合は空の配列を返してください。
        """.trimIndent()

        private val RESPONSE_SCHEMA = buildJsonObject {
            put("type", "OBJECT")
            putJsonObject("properties") {
                putJsonObject("usages") {
                    put("type", "ARRAY")
                    putJsonObject("items") {
                        put("type", "OBJECT")
                        putJsonObject("properties") {
                            putJsonObject("title") { put("type", "STRING") }
                            putJsonObject("description") { put("type", "STRING") }
                            putJsonObject("amount") {
                                put("type", "INTEGER")
                                put("nullable", true)
                            }
                            putJsonObject("dateTime") {
                                put("type", "STRING")
                                put("nullable", true)
                            }
                        }
                        putJsonArray("required") {
                            add("title")
                            add("description")
                            add("amount")
                            add("dateTime")
                        }
                    }
                }
            }
            putJsonArray("required") { add("usages") }
        }
    }
}
