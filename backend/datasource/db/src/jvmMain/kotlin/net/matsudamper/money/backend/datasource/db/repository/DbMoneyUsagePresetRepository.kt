package net.matsudamper.money.backend.datasource.db.repository

import net.matsudamper.money.backend.app.interfaces.MoneyUsagePresetRepository
import net.matsudamper.money.backend.app.interfaces.UpdateValue
import net.matsudamper.money.backend.datasource.db.DbConnectionImpl
import net.matsudamper.money.db.schema.tables.JMoneyUsagePresets
import net.matsudamper.money.db.schema.tables.JMoneyUsageSubCategories
import net.matsudamper.money.db.schema.tables.records.JMoneyUsagePresetsRecord
import net.matsudamper.money.element.MoneyUsagePresetId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.element.UserId
import org.jooq.impl.DSL

class DbMoneyUsagePresetRepository : MoneyUsagePresetRepository {
    private val presets = JMoneyUsagePresets.MONEY_USAGE_PRESETS
    private val subCategories = JMoneyUsageSubCategories.MONEY_USAGE_SUB_CATEGORIES

    override fun getPresets(userId: UserId): List<MoneyUsagePresetRepository.PresetResult> {
        return DbConnectionImpl.use { connection ->
            DSL.using(connection)
                .selectFrom(presets)
                .where(presets.USER_ID.eq(userId.value))
                .orderBy(presets.ORDER_NUMBER.asc(), presets.MONEY_USAGE_PRESET_ID.asc())
                .fetch()
                .map { record ->
                    record.toPresetResult()
                }
        }
    }

    override fun getPreset(
        userId: UserId,
        presetId: MoneyUsagePresetId,
    ): MoneyUsagePresetRepository.PresetResult? {
        return DbConnectionImpl.use { connection ->
            getPreset(
                connection = connection,
                userId = userId,
                presetId = presetId,
            )
        }
    }

    override fun addPreset(
        userId: UserId,
        name: String,
        subCategoryId: MoneyUsageSubCategoryId?,
        amount: Int?,
        description: String?,
    ): MoneyUsagePresetRepository.AddPresetResult {
        return runCatching {
            DbConnectionImpl.use { connection ->
                if (subCategoryId != null && !isOwnedSubCategory(connection, userId, subCategoryId)) {
                    throw IllegalArgumentException("subCategoryId=$subCategoryId is not owned by userId=$userId")
                }

                val result = DSL.using(connection)
                    .insertInto(presets)
                    .set(
                        JMoneyUsagePresetsRecord(
                            userId = userId.value,
                            name = name,
                            moneyUsageSubCategoryId = subCategoryId?.id,
                        ),
                    )
                    .returning()
                    .fetchOne()!!

                result.toPresetResult()
            }
        }.fold(
            onSuccess = { MoneyUsagePresetRepository.AddPresetResult.Success(it) },
            onFailure = { MoneyUsagePresetRepository.AddPresetResult.Failed(it) },
        )
    }

    override fun updatePreset(
        userId: UserId,
        presetId: MoneyUsagePresetId,
        name: UpdateValue<String>,
        subCategoryId: UpdateValue<MoneyUsageSubCategoryId?>,
        amount: UpdateValue<Int?>,
        description: UpdateValue<String?>,
    ): MoneyUsagePresetRepository.PresetResult? {
        return DbConnectionImpl.use { connection ->
            val patchRecord = JMoneyUsagePresetsRecord()
            var hasUpdate = false

            when (name) {
                is UpdateValue.NotUpdate -> Unit
                is UpdateValue.Update -> {
                    patchRecord.set(presets.NAME, name.value)
                    hasUpdate = true
                }
            }

            when (subCategoryId) {
                is UpdateValue.NotUpdate -> Unit
                is UpdateValue.Update -> {
                    val updatedSubCategoryId = subCategoryId.value
                    if (updatedSubCategoryId != null && !isOwnedSubCategory(connection, userId, updatedSubCategoryId)) {
                        throw IllegalArgumentException("subCategoryId=$updatedSubCategoryId is not owned by userId=$userId")
                    }
                    patchRecord.set(presets.MONEY_USAGE_SUB_CATEGORY_ID, subCategoryId.value?.id)
                    hasUpdate = true
                }
            }

            if (hasUpdate) {
                DSL.using(connection)
                    .update(presets)
                    .set(patchRecord)
                    .where(
                        presets.USER_ID.eq(userId.value)
                            .and(presets.MONEY_USAGE_PRESET_ID.eq(presetId.value)),
                    )
                    .limit(1)
                    .execute()
            }

            getPreset(
                connection = connection,
                userId = userId,
                presetId = presetId,
            )
        }
    }

    override fun deletePreset(
        userId: UserId,
        presetId: MoneyUsagePresetId,
    ): Boolean {
        return DbConnectionImpl.use { connection ->
            DSL.using(connection)
                .deleteFrom(presets)
                .where(
                    presets.USER_ID.eq(userId.value)
                        .and(presets.MONEY_USAGE_PRESET_ID.eq(presetId.value)),
                )
                .execute() >= 1
        }
    }

    private fun JMoneyUsagePresetsRecord.toPresetResult(): MoneyUsagePresetRepository.PresetResult {
        return MoneyUsagePresetRepository.PresetResult(
            presetId = MoneyUsagePresetId(moneyUsagePresetId!!),
            userId = UserId(userId!!),
            name = name!!,
            subCategoryId = moneyUsageSubCategoryId?.let { MoneyUsageSubCategoryId(it) },
            // TODO: DBコード生成後にamountとdescriptionカラムを追加
            amount = null,
            description = null,
        )
    }

    private fun isOwnedSubCategory(
        connection: java.sql.Connection,
        userId: UserId,
        subCategoryId: MoneyUsageSubCategoryId,
    ): Boolean {
        return DSL.using(connection)
            .select(subCategories.MONEY_USAGE_SUB_CATEGORY_ID)
            .from(subCategories)
            .where(
                subCategories.USER_ID.eq(userId.value)
                    .and(subCategories.MONEY_USAGE_SUB_CATEGORY_ID.eq(subCategoryId.id)),
            )
            .limit(1)
            .fetchOne() != null
    }

    private fun getPreset(
        connection: java.sql.Connection,
        userId: UserId,
        presetId: MoneyUsagePresetId,
    ): MoneyUsagePresetRepository.PresetResult? {
        return DSL.using(connection)
            .selectFrom(presets)
            .where(
                presets.USER_ID.eq(userId.value)
                    .and(presets.MONEY_USAGE_PRESET_ID.eq(presetId.value)),
            )
            .limit(1)
            .fetchOne()
            ?.toPresetResult()
    }
}
