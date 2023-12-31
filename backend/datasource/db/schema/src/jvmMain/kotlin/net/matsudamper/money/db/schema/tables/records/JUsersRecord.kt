/*
 * This file is generated by jOOQ.
 */
package net.matsudamper.money.db.schema.tables.records


import java.time.LocalDateTime

import net.matsudamper.money.db.schema.tables.JUsers

import org.jooq.Field
import org.jooq.Record1
import org.jooq.Record3
import org.jooq.Row3
import org.jooq.impl.UpdatableRecordImpl


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class JUsersRecord() : UpdatableRecordImpl<JUsersRecord>(JUsers.USERS), Record3<Int?, String?, LocalDateTime?> {

    open var userId: Int?
        set(value): Unit = set(0, value)
        get(): Int? = get(0) as Int?

    open var userName: String?
        set(value): Unit = set(1, value)
        get(): String? = get(1) as String?

    open var createdDatetime: LocalDateTime?
        set(value): Unit = set(2, value)
        get(): LocalDateTime? = get(2) as LocalDateTime?

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    override fun key(): Record1<Int?> = super.key() as Record1<Int?>

    // -------------------------------------------------------------------------
    // Record3 type implementation
    // -------------------------------------------------------------------------

    override fun fieldsRow(): Row3<Int?, String?, LocalDateTime?> = super.fieldsRow() as Row3<Int?, String?, LocalDateTime?>
    override fun valuesRow(): Row3<Int?, String?, LocalDateTime?> = super.valuesRow() as Row3<Int?, String?, LocalDateTime?>
    override fun field1(): Field<Int?> = JUsers.USERS.USER_ID
    override fun field2(): Field<String?> = JUsers.USERS.USER_NAME
    override fun field3(): Field<LocalDateTime?> = JUsers.USERS.CREATED_DATETIME
    override fun component1(): Int? = userId
    override fun component2(): String? = userName
    override fun component3(): LocalDateTime? = createdDatetime
    override fun value1(): Int? = userId
    override fun value2(): String? = userName
    override fun value3(): LocalDateTime? = createdDatetime

    override fun value1(value: Int?): JUsersRecord {
        set(0, value)
        return this
    }

    override fun value2(value: String?): JUsersRecord {
        set(1, value)
        return this
    }

    override fun value3(value: LocalDateTime?): JUsersRecord {
        set(2, value)
        return this
    }

    override fun values(value1: Int?, value2: String?, value3: LocalDateTime?): JUsersRecord {
        this.value1(value1)
        this.value2(value2)
        this.value3(value3)
        return this
    }

    /**
     * Create a detached, initialised JUsersRecord
     */
    constructor(userId: Int? = null, userName: String? = null, createdDatetime: LocalDateTime? = null): this() {
        this.userId = userId
        this.userName = userName
        this.createdDatetime = createdDatetime
        resetChangedOnNotNull()
    }
}
