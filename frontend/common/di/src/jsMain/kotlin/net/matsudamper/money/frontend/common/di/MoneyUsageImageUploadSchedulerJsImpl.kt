package net.matsudamper.money.frontend.common.di

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import net.matsudamper.money.element.ImageId
import net.matsudamper.money.element.MoneyUsageId
import net.matsudamper.money.frontend.common.base.ImageUploadClient
import net.matsudamper.money.frontend.common.base.MoneyUsageImageUploadScheduler

internal class MoneyUsageImageUploadSchedulerJsImpl(
    private val imageUploadClient: ImageUploadClient,
) : MoneyUsageImageUploadScheduler {

    private val mutex = Mutex()

    override suspend fun scheduleUploadAndLink(
        bytes: ByteArray,
        contentType: String?,
        moneyUsageId: MoneyUsageId,
        currentImageIds: List<ImageId>,
    ): Boolean {
        val uploadResult = imageUploadClient.upload(bytes, contentType) ?: return false

        val updatedImageIds = (currentImageIds + uploadResult.imageId).distinctBy { it.value }

        return mutex.withLock {
            updateMoneyUsage(moneyUsageId, updatedImageIds)
        }
    }

    private suspend fun updateMoneyUsage(
        moneyUsageId: MoneyUsageId,
        imageIds: List<ImageId>,
    ): Boolean {
        val query = """
            mutation MoneyUsageScreenUpdateUsage(${"$"}query: UpdateUsageQuery!) {
                userMutation {
                    updateUsage(query: ${"$"}query) {
                        id
                    }
                }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            putJsonObject("query") {
                put("id", moneyUsageId.id)
                putJsonArray("imageIds") {
                    imageIds.forEach { add(JsonPrimitive(it.value)) }
                }
            }
        }

        val body = buildJsonObject {
            put("query", query)
            put("variables", variables)
        }.toString()

        return runCatching {
            val init = js("({})")
            init.method = "POST"
            init.body = body
            init.credentials = "include"
            val headers = js("({})")
            headers["Content-Type"] = "application/json"
            init.headers = headers

            val response = window.fetch("/query", init).await()
            val responseBody = response.text().await()
            val json = Json.parseToJsonElement(responseBody)
            json.toString().contains("\"updateUsage\"")
        }.getOrElse { false }
    }
}
