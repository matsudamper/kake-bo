/*
 * This file is generated by jOOQ.
 */
package net.matsudamper.money.db.schema.tables.records


import java.time.LocalDateTime

import net.matsudamper.money.db.schema.tables.JMoneyUsageSubCategories

import org.jooq.Record1
import org.jooq.impl.UpdatableRecordImpl


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class JMoneyUsageSubCategoriesRecord() : UpdatableRecordImpl<JMoneyUsageSubCategoriesRecord>(JMoneyUsageSubCategories.MONEY_USAGE_SUB_CATEGORIES) {

    open var moneyUsageSubCategoryId: Int?
        set(value): Unit = set(0, value)
        get(): Int? = get(0) as Int?

    open var userId: Int?
        set(value): Unit = set(1, value)
        get(): Int? = get(1) as Int?

    open var moneyUsageCategoryId: Int?
        set(value): Unit = set(2, value)
        get(): Int? = get(2) as Int?

    open var name: String?
        set(value): Unit = set(3, value)
        get(): String? = get(3) as String?

    open var createdDatetime: LocalDateTime?
        set(value): Unit = set(4, value)
        get(): LocalDateTime? = get(4) as LocalDateTime?

    open var updateDatetime: LocalDateTime?
        set(value): Unit = set(5, value)
        get(): LocalDateTime? = get(5) as LocalDateTime?

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    override fun key(): Record1<Int?> = super.key() as Record1<Int?>

    /**
     * Create a detached, initialised JMoneyUsageSubCategoriesRecord
     */
    constructor(moneyUsageSubCategoryId: Int? = null, userId: Int? = null, moneyUsageCategoryId: Int? = null, name: String? = null, createdDatetime: LocalDateTime? = null, updateDatetime: LocalDateTime? = null): this() {
        this.moneyUsageSubCategoryId = moneyUsageSubCategoryId
        this.userId = userId
        this.moneyUsageCategoryId = moneyUsageCategoryId
        this.name = name
        this.createdDatetime = createdDatetime
        this.updateDatetime = updateDatetime
        resetChangedOnNotNull()
    }
}
