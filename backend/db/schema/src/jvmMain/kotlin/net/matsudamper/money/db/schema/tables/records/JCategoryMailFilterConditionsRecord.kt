/*
 * This file is generated by jOOQ.
 */
package net.matsudamper.money.db.schema.tables.records


import java.time.LocalDateTime

import net.matsudamper.money.db.schema.tables.JCategoryMailFilterConditions

import org.jooq.Field
import org.jooq.Record1
import org.jooq.Record8
import org.jooq.Row8
import org.jooq.impl.UpdatableRecordImpl


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class JCategoryMailFilterConditionsRecord() : UpdatableRecordImpl<JCategoryMailFilterConditionsRecord>(JCategoryMailFilterConditions.CATEGORY_MAIL_FILTER_CONDITIONS), Record8<Int?, Int?, Int?, String?, Int?, Int?, LocalDateTime?, LocalDateTime?> {

    open var categoryMailFilterConditionId: Int?
        set(value): Unit = set(0, value)
        get(): Int? = get(0) as Int?

    open var categoryMailFilterConditionGroupId: Int?
        set(value): Unit = set(1, value)
        get(): Int? = get(1) as Int?

    open var userId: Int?
        set(value): Unit = set(2, value)
        get(): Int? = get(2) as Int?

    open var text: String?
        set(value): Unit = set(3, value)
        get(): String? = get(3) as String?

    open var categoryMailFilterDatasourceTypeId: Int?
        set(value): Unit = set(4, value)
        get(): Int? = get(4) as Int?

    open var categoryMailFilterConditionTypeId: Int?
        set(value): Unit = set(5, value)
        get(): Int? = get(5) as Int?

    open var createdDatetime: LocalDateTime?
        set(value): Unit = set(6, value)
        get(): LocalDateTime? = get(6) as LocalDateTime?

    open var updateDatetime: LocalDateTime?
        set(value): Unit = set(7, value)
        get(): LocalDateTime? = get(7) as LocalDateTime?

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    override fun key(): Record1<Int?> = super.key() as Record1<Int?>

    // -------------------------------------------------------------------------
    // Record8 type implementation
    // -------------------------------------------------------------------------

    override fun fieldsRow(): Row8<Int?, Int?, Int?, String?, Int?, Int?, LocalDateTime?, LocalDateTime?> = super.fieldsRow() as Row8<Int?, Int?, Int?, String?, Int?, Int?, LocalDateTime?, LocalDateTime?>
    override fun valuesRow(): Row8<Int?, Int?, Int?, String?, Int?, Int?, LocalDateTime?, LocalDateTime?> = super.valuesRow() as Row8<Int?, Int?, Int?, String?, Int?, Int?, LocalDateTime?, LocalDateTime?>
    override fun field1(): Field<Int?> = JCategoryMailFilterConditions.CATEGORY_MAIL_FILTER_CONDITIONS.CATEGORY_MAIL_FILTER_CONDITION_ID
    override fun field2(): Field<Int?> = JCategoryMailFilterConditions.CATEGORY_MAIL_FILTER_CONDITIONS.CATEGORY_MAIL_FILTER_CONDITION_GROUP_ID
    override fun field3(): Field<Int?> = JCategoryMailFilterConditions.CATEGORY_MAIL_FILTER_CONDITIONS.USER_ID
    override fun field4(): Field<String?> = JCategoryMailFilterConditions.CATEGORY_MAIL_FILTER_CONDITIONS.TEXT
    override fun field5(): Field<Int?> = JCategoryMailFilterConditions.CATEGORY_MAIL_FILTER_CONDITIONS.CATEGORY_MAIL_FILTER_DATASOURCE_TYPE_ID
    override fun field6(): Field<Int?> = JCategoryMailFilterConditions.CATEGORY_MAIL_FILTER_CONDITIONS.CATEGORY_MAIL_FILTER_CONDITION_TYPE_ID
    override fun field7(): Field<LocalDateTime?> = JCategoryMailFilterConditions.CATEGORY_MAIL_FILTER_CONDITIONS.CREATED_DATETIME
    override fun field8(): Field<LocalDateTime?> = JCategoryMailFilterConditions.CATEGORY_MAIL_FILTER_CONDITIONS.UPDATE_DATETIME
    override fun component1(): Int? = categoryMailFilterConditionId
    override fun component2(): Int? = categoryMailFilterConditionGroupId
    override fun component3(): Int? = userId
    override fun component4(): String? = text
    override fun component5(): Int? = categoryMailFilterDatasourceTypeId
    override fun component6(): Int? = categoryMailFilterConditionTypeId
    override fun component7(): LocalDateTime? = createdDatetime
    override fun component8(): LocalDateTime? = updateDatetime
    override fun value1(): Int? = categoryMailFilterConditionId
    override fun value2(): Int? = categoryMailFilterConditionGroupId
    override fun value3(): Int? = userId
    override fun value4(): String? = text
    override fun value5(): Int? = categoryMailFilterDatasourceTypeId
    override fun value6(): Int? = categoryMailFilterConditionTypeId
    override fun value7(): LocalDateTime? = createdDatetime
    override fun value8(): LocalDateTime? = updateDatetime

    override fun value1(value: Int?): JCategoryMailFilterConditionsRecord {
        set(0, value)
        return this
    }

    override fun value2(value: Int?): JCategoryMailFilterConditionsRecord {
        set(1, value)
        return this
    }

    override fun value3(value: Int?): JCategoryMailFilterConditionsRecord {
        set(2, value)
        return this
    }

    override fun value4(value: String?): JCategoryMailFilterConditionsRecord {
        set(3, value)
        return this
    }

    override fun value5(value: Int?): JCategoryMailFilterConditionsRecord {
        set(4, value)
        return this
    }

    override fun value6(value: Int?): JCategoryMailFilterConditionsRecord {
        set(5, value)
        return this
    }

    override fun value7(value: LocalDateTime?): JCategoryMailFilterConditionsRecord {
        set(6, value)
        return this
    }

    override fun value8(value: LocalDateTime?): JCategoryMailFilterConditionsRecord {
        set(7, value)
        return this
    }

    override fun values(value1: Int?, value2: Int?, value3: Int?, value4: String?, value5: Int?, value6: Int?, value7: LocalDateTime?, value8: LocalDateTime?): JCategoryMailFilterConditionsRecord {
        this.value1(value1)
        this.value2(value2)
        this.value3(value3)
        this.value4(value4)
        this.value5(value5)
        this.value6(value6)
        this.value7(value7)
        this.value8(value8)
        return this
    }

    /**
     * Create a detached, initialised JCategoryMailFilterConditionsRecord
     */
    constructor(categoryMailFilterConditionId: Int? = null, categoryMailFilterConditionGroupId: Int? = null, userId: Int? = null, text: String? = null, categoryMailFilterDatasourceTypeId: Int? = null, categoryMailFilterConditionTypeId: Int? = null, createdDatetime: LocalDateTime? = null, updateDatetime: LocalDateTime? = null): this() {
        this.categoryMailFilterConditionId = categoryMailFilterConditionId
        this.categoryMailFilterConditionGroupId = categoryMailFilterConditionGroupId
        this.userId = userId
        this.text = text
        this.categoryMailFilterDatasourceTypeId = categoryMailFilterDatasourceTypeId
        this.categoryMailFilterConditionTypeId = categoryMailFilterConditionTypeId
        this.createdDatetime = createdDatetime
        this.updateDatetime = updateDatetime
        resetChangedOnNotNull()
    }
}