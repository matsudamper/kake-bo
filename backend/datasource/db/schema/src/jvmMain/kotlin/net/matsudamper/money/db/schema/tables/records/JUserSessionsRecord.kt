/*
 * This file is generated by jOOQ.
 */
package net.matsudamper.money.db.schema.tables.records


import java.time.LocalDateTime

import net.matsudamper.money.db.schema.tables.JUserSessions

import org.jooq.Field
import org.jooq.Record1
import org.jooq.Record5
import org.jooq.Row5
import org.jooq.impl.UpdatableRecordImpl


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class JUserSessionsRecord() : UpdatableRecordImpl<JUserSessionsRecord>(JUserSessions.USER_SESSIONS), Record5<String?, Int?, LocalDateTime?, LocalDateTime?, String?> {

    open var sessionId: String?
        set(value): Unit = set(0, value)
        get(): String? = get(0) as String?

    open var userId: Int?
        set(value): Unit = set(1, value)
        get(): Int? = get(1) as Int?

    open var createdDate: LocalDateTime?
        set(value): Unit = set(2, value)
        get(): LocalDateTime? = get(2) as LocalDateTime?

    open var latestAccessedAt: LocalDateTime?
        set(value): Unit = set(3, value)
        get(): LocalDateTime? = get(3) as LocalDateTime?

    open var name: String?
        set(value): Unit = set(4, value)
        get(): String? = get(4) as String?

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    override fun key(): Record1<String?> = super.key() as Record1<String?>

    // -------------------------------------------------------------------------
    // Record5 type implementation
    // -------------------------------------------------------------------------

    override fun fieldsRow(): Row5<String?, Int?, LocalDateTime?, LocalDateTime?, String?> = super.fieldsRow() as Row5<String?, Int?, LocalDateTime?, LocalDateTime?, String?>
    override fun valuesRow(): Row5<String?, Int?, LocalDateTime?, LocalDateTime?, String?> = super.valuesRow() as Row5<String?, Int?, LocalDateTime?, LocalDateTime?, String?>
    override fun field1(): Field<String?> = JUserSessions.USER_SESSIONS.SESSION_ID
    override fun field2(): Field<Int?> = JUserSessions.USER_SESSIONS.USER_ID
    override fun field3(): Field<LocalDateTime?> = JUserSessions.USER_SESSIONS.CREATED_DATE
    override fun field4(): Field<LocalDateTime?> = JUserSessions.USER_SESSIONS.LATEST_ACCESSED_AT
    override fun field5(): Field<String?> = JUserSessions.USER_SESSIONS.NAME
    override fun component1(): String? = sessionId
    override fun component2(): Int? = userId
    override fun component3(): LocalDateTime? = createdDate
    override fun component4(): LocalDateTime? = latestAccessedAt
    override fun component5(): String? = name
    override fun value1(): String? = sessionId
    override fun value2(): Int? = userId
    override fun value3(): LocalDateTime? = createdDate
    override fun value4(): LocalDateTime? = latestAccessedAt
    override fun value5(): String? = name

    override fun value1(value: String?): JUserSessionsRecord {
        set(0, value)
        return this
    }

    override fun value2(value: Int?): JUserSessionsRecord {
        set(1, value)
        return this
    }

    override fun value3(value: LocalDateTime?): JUserSessionsRecord {
        set(2, value)
        return this
    }

    override fun value4(value: LocalDateTime?): JUserSessionsRecord {
        set(3, value)
        return this
    }

    override fun value5(value: String?): JUserSessionsRecord {
        set(4, value)
        return this
    }

    override fun values(value1: String?, value2: Int?, value3: LocalDateTime?, value4: LocalDateTime?, value5: String?): JUserSessionsRecord {
        this.value1(value1)
        this.value2(value2)
        this.value3(value3)
        this.value4(value4)
        this.value5(value5)
        return this
    }

    /**
     * Create a detached, initialised JUserSessionsRecord
     */
    constructor(sessionId: String? = null, userId: Int? = null, createdDate: LocalDateTime? = null, latestAccessedAt: LocalDateTime? = null, name: String? = null): this() {
        this.sessionId = sessionId
        this.userId = userId
        this.createdDate = createdDate
        this.latestAccessedAt = latestAccessedAt
        this.name = name
        resetChangedOnNotNull()
    }
}
