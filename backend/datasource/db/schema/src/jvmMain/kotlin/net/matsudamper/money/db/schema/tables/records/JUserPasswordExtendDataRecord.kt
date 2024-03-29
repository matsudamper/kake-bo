/*
 * This file is generated by jOOQ.
 */
package net.matsudamper.money.db.schema.tables.records


import java.time.LocalDateTime

import net.matsudamper.money.db.schema.tables.JUserPasswordExtendData

import org.jooq.Record1
import org.jooq.impl.UpdatableRecordImpl


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class JUserPasswordExtendDataRecord() : UpdatableRecordImpl<JUserPasswordExtendDataRecord>(JUserPasswordExtendData.USER_PASSWORD_EXTEND_DATA) {

    open var userId: Int?
        set(value): Unit = set(0, value)
        get(): Int? = get(0) as Int?

    open var salt: ByteArray?
        set(value): Unit = set(1, value)
        get(): ByteArray? = get(1) as ByteArray?

    open var iterationCount: Int?
        set(value): Unit = set(2, value)
        get(): Int? = get(2) as Int?

    open var algorithm: String?
        set(value): Unit = set(3, value)
        get(): String? = get(3) as String?

    open var keyLength: Int?
        set(value): Unit = set(4, value)
        get(): Int? = get(4) as Int?

    open var createdDatetime: LocalDateTime?
        set(value): Unit = set(5, value)
        get(): LocalDateTime? = get(5) as LocalDateTime?

    open var updateDatetime: LocalDateTime?
        set(value): Unit = set(6, value)
        get(): LocalDateTime? = get(6) as LocalDateTime?

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    override fun key(): Record1<Int?> = super.key() as Record1<Int?>

    /**
     * Create a detached, initialised JUserPasswordExtendDataRecord
     */
    constructor(userId: Int? = null, salt: ByteArray? = null, iterationCount: Int? = null, algorithm: String? = null, keyLength: Int? = null, createdDatetime: LocalDateTime? = null, updateDatetime: LocalDateTime? = null): this() {
        this.userId = userId
        this.salt = salt
        this.iterationCount = iterationCount
        this.algorithm = algorithm
        this.keyLength = keyLength
        this.createdDatetime = createdDatetime
        this.updateDatetime = updateDatetime
        resetChangedOnNotNull()
    }
}
