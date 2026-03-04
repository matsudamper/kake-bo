package net.matsudamper.money.backend.graphql.service

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

internal class TranslateService {
    private val httpClient = HttpClient.newHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    data class TranslateResult(
        val translatedText: String,
        val sourceLanguage: String,
        val targetLanguage: String,
    )

    fun translate(
        text: String,
        targetLanguage: String,
    ): TranslateResult {
        val truncatedText = text.take(MAX_TEXT_LENGTH)
        val encodedText = URLEncoder.encode(truncatedText, "UTF-8")
        val url = "https://translate.googleapis.com/translate_a/single" +
            "?client=gtx&sl=auto&tl=$targetLanguage&dt=t&q=$encodedText"

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", "Mozilla/5.0")
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        val body = response.body()
        val jsonElement = json.parseToJsonElement(body).jsonArray

        val detectedSourceLang = runCatching {
            jsonElement[2].jsonPrimitive.content
        }.getOrDefault("unknown")

        val translatedText = buildString {
            runCatching {
                jsonElement[0].jsonArray.forEach { segment ->
                    val segmentText = segment.jsonArray[0].jsonPrimitive.contentOrNull
                    if (segmentText != null) append(segmentText)
                }
            }
        }

        return TranslateResult(
            translatedText = translatedText,
            sourceLanguage = detectedSourceLang,
            targetLanguage = targetLanguage,
        )
    }

    private companion object {
        const val MAX_TEXT_LENGTH = 5000
    }
}
