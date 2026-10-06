package net.matsudamper.money.backend.feature.aimailparser

import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import net.matsudamper.money.backend.app.interfaces.GeminiGateway

class AiMailParser(
    private val geminiGateway: GeminiGateway,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(
        apiKey: String,
        input: AiMailParseInput,
    ): Result<List<AiParsedUsage>> {
        return geminiGateway.generateJson(
            apiKey = apiKey,
            systemInstruction = SYSTEM_INSTRUCTION,
            userText = MailBodyTextBuilder.build(input = input, maxBodyLength = MAX_BODY_LENGTH),
            responseSchemaJson = RESPONSE_SCHEMA_JSON,
        ).mapCatching { text ->
            parseResponse(text)
        }
    }

    private fun parseResponse(text: String): List<AiParsedUsage> {
        val result = json.decodeFromString<ParseResult>(text)
        return result.usages
            .filter { it.title.isNotBlank() }
            .map { usage ->
                AiParsedUsage(
                    title = usage.title.trim(),
                    description = usage.description.orEmpty().trim(),
                    amount = usage.amount,
                    dateTime = usage.dateTime?.let { parseDateTime(it) },
                    serviceName = usage.serviceName?.trim()?.ifEmpty { null },
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
        @SerialName("serviceName") val serviceName: String? = null,
    )

    private companion object {
        // 長い広告メールでトークンを使い過ぎないように本文を切り詰める
        private const val MAX_BODY_LENGTH = 30_000

        private val SYSTEM_INSTRUCTION = """
            あなたは家計簿アプリのためにメールから支出を抽出するアシスタントです。
            与えられたメールから、実際に支払いや購入、利用が発生した項目を抽出してください。
            - 1通のメールに複数の支払いがある場合は、支払いごとに分けて出力してください。
            - 同じ注文の商品明細は1つの支出にまとめ、商品名などは description に記載してください。
            - title は家計簿に表示する短い名前にしてください。店名やサービス名を含めてください。
            - amount は日本円の支払金額を整数で入れてください。返金の場合は負の値にしてください。不明な場合は null にしてください。
            - serviceName は支払先の店名やサービス名を入れてください。不明な場合は null にしてください。
            - dateTime は利用日時を yyyy-MM-ddTHH:mm:ss 形式で入れてください。時刻が不明な場合は 00:00:00、日付が不明な場合は null にしてください。年が書かれていない場合は受信日時から補ってください。
            - 広告、キャンペーン案内、ポイント付与のお知らせなど支払いが発生していないメールの場合は空の配列を返してください。
        """.trimIndent()

        private val RESPONSE_SCHEMA_JSON = buildJsonObject {
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
                            putJsonObject("serviceName") {
                                put("type", "STRING")
                                put("nullable", true)
                            }
                        }
                        putJsonArray("required") {
                            add("title")
                            add("description")
                            add("amount")
                            add("dateTime")
                            add("serviceName")
                        }
                    }
                }
            }
            putJsonArray("required") { add("usages") }
        }.toString()
    }
}
