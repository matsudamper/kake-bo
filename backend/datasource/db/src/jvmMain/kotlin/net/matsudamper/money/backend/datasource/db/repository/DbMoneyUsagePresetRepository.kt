package net.matsudamper.money.backend.datasource.db.repository

import net.matsudamper.money.backend.app.interfaces.MoneyUsagePresetRepository
import net.matsudamper.money.backend.datasource.db.DbConnectionImpl
import net.matsudamper.money.db.schema.tables.JMoneyUsagePresets
import net.matsudamper.money.db.schema.tables.records.JMoneyUsagePresetsRecord
import net.matsudamper.money.element.MoneyUsagePresetId
import net.matsudamper.money.element.MoneyUsageSubCategoryId
import net.matsudamper.money.element.UserId
import org.jooq.impl.DSL

class DbMoneyUsagePresetRepository : MoneyUsagePresetRepository {
    private val presets = JMoneyUsagePresets.MONEY_USAGE_PRESETS

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
    ): MoneyUsagePresetRepository.AddPresetResult {
        return runCatching {
            DbConnectionImpl.use { connection ->
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
        name: String?,
        subCategoryId: MoneyUsageSubCategoryId?,
        updateSubCategoryId: Boolean,
    ): MoneyUsagePresetRepository.PresetResult? {
        return DbConnectionImpl.use { connection ->
            if (name == null && !updateSubCategoryId) {
                return@use getPreset(
                    connection = connection,
                    userId = userId,
                    presetId = presetId,
                )
            }

            val updatedCount = DSL.using(connection)
                .update(presets)
                .set(presets.MONEY_USAGE_PRESET_ID, presetId.value)
                .apply {
                    if (name != null) {
                        set(presets.NAME, name)
                    }
                    if (updateSubCategoryId) {
                        set(presets.MONEY_USAGE_SUB_CATEGORY_ID, subCategoryId?.id)
                    }
                }
                .where(
                    presets.USER_ID.eq(userId.value)
                        .and(presets.MONEY_USAGE_PRESET_ID.eq(presetId.value)),
                )
                .limit(1)
                .execute()

            if (updatedCount < 1) {
                null
            } else {
                getPreset(
                    connection = connection,
                    userId = userId,
                    presetId = presetId,
                )
            }
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
        )
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
