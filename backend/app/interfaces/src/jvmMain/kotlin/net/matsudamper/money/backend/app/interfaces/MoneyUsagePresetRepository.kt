package net.matsudamper.money.backend.app.interfaces

import net.matsudamper.money.element.MoneyUsagePresetId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.element.UserId

interface MoneyUsagePresetRepository {
    fun getPresets(userId: UserId): List<PresetResult>

    fun getPreset(
        userId: UserId,
        presetId: MoneyUsagePresetId,
    ): PresetResult?

    fun addPreset(
        userId: UserId,
        name: String,
        subCategoryId: MoneyUsageSubCategoryId?,
    ): AddPresetResult

    fun updatePreset(
        userId: UserId,
        presetId: MoneyUsagePresetId,
        name: String?,
        subCategoryId: MoneyUsageSubCategoryId?,
        updateSubCategoryId: Boolean,
    ): PresetResult?

    fun deletePreset(
        userId: UserId,
        presetId: MoneyUsagePresetId,
    ): Boolean

    data class PresetResult(
        val presetId: MoneyUsagePresetId,
        val userId: UserId,
        val name: String,
        val subCategoryId: MoneyUsageSubCategoryId?,
    )

    sealed interface AddPresetResult {
        data class Success(val result: PresetResult) : AddPresetResult

        data class Failed(val error: Throwable) : AddPresetResult
    }
}
