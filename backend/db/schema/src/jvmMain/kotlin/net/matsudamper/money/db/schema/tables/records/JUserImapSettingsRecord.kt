/*
 * This file is generated by jOOQ.
 */
package net.matsudamper.money.db.schema.tables.records


import net.matsudamper.money.db.schema.tables.JUserImapSettings

import org.jooq.Field
import org.jooq.Record1
import org.jooq.Record5
import org.jooq.Row5
import org.jooq.impl.UpdatableRecordImpl


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class JUserImapSettingsRecord() : UpdatableRecordImpl<JUserImapSettingsRecord>(JUserImapSettings.USER_IMAP_SETTINGS), Record5<Int?, String?, Int?, String?, String?> {

    open var userId: Int?
        set(value): Unit = set(0, value)
        get(): Int? = get(0) as Int?

    open var host: String?
        set(value): Unit = set(1, value)
        get(): String? = get(1) as String?

    open var port: Int?
        set(value): Unit = set(2, value)
        get(): Int? = get(2) as Int?

    open var useName: String?
        set(value): Unit = set(3, value)
        get(): String? = get(3) as String?

    open var password: String?
        set(value): Unit = set(4, value)
        get(): String? = get(4) as String?

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    override fun key(): Record1<Int?> = super.key() as Record1<Int?>

    // -------------------------------------------------------------------------
    // Record5 type implementation
    // -------------------------------------------------------------------------

    override fun fieldsRow(): Row5<Int?, String?, Int?, String?, String?> = super.fieldsRow() as Row5<Int?, String?, Int?, String?, String?>
    override fun valuesRow(): Row5<Int?, String?, Int?, String?, String?> = super.valuesRow() as Row5<Int?, String?, Int?, String?, String?>
    override fun field1(): Field<Int?> = JUserImapSettings.USER_IMAP_SETTINGS.USER_ID
    override fun field2(): Field<String?> = JUserImapSettings.USER_IMAP_SETTINGS.HOST
    override fun field3(): Field<Int?> = JUserImapSettings.USER_IMAP_SETTINGS.PORT
    override fun field4(): Field<String?> = JUserImapSettings.USER_IMAP_SETTINGS.USE_NAME
    override fun field5(): Field<String?> = JUserImapSettings.USER_IMAP_SETTINGS.PASSWORD
    override fun component1(): Int? = userId
    override fun component2(): String? = host
    override fun component3(): Int? = port
    override fun component4(): String? = useName
    override fun component5(): String? = password
    override fun value1(): Int? = userId
    override fun value2(): String? = host
    override fun value3(): Int? = port
    override fun value4(): String? = useName
    override fun value5(): String? = password

    override fun value1(value: Int?): JUserImapSettingsRecord {
        set(0, value)
        return this
    }

    override fun value2(value: String?): JUserImapSettingsRecord {
        set(1, value)
        return this
    }

    override fun value3(value: Int?): JUserImapSettingsRecord {
        set(2, value)
        return this
    }

    override fun value4(value: String?): JUserImapSettingsRecord {
        set(3, value)
        return this
    }

    override fun value5(value: String?): JUserImapSettingsRecord {
        set(4, value)
        return this
    }

    override fun values(value1: Int?, value2: String?, value3: Int?, value4: String?, value5: String?): JUserImapSettingsRecord {
        this.value1(value1)
        this.value2(value2)
        this.value3(value3)
        this.value4(value4)
        this.value5(value5)
        return this
    }

    /**
     * Create a detached, initialised JUserImapSettingsRecord
     */
    constructor(userId: Int? = null, host: String? = null, port: Int? = null, useName: String? = null, password: String? = null): this() {
        this.userId = userId
        this.host = host
        this.port = port
        this.useName = useName
        this.password = password
        resetChangedOnNotNull()
    }
}