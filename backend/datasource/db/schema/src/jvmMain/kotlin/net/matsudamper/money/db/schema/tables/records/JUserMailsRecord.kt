/*
 * This file is generated by jOOQ.
 */
package net.matsudamper.money.db.schema.tables.records


import java.time.LocalDateTime

import net.matsudamper.money.db.schema.tables.JUserMails

import org.jooq.Field
import org.jooq.Record1
import org.jooq.Record8
import org.jooq.Row8
import org.jooq.impl.UpdatableRecordImpl


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class JUserMailsRecord() : UpdatableRecordImpl<JUserMailsRecord>(JUserMails.USER_MAILS), Record8<Int?, Int?, String?, String?, LocalDateTime?, LocalDateTime?, String?, String?> {

    open var userMailId: Int?
        set(value): Unit = set(0, value)
        get(): Int? = get(0) as Int?

    open var userId: Int?
        set(value): Unit = set(1, value)
        get(): Int? = get(1) as Int?

    open var plain: String?
        set(value): Unit = set(2, value)
        get(): String? = get(2) as String?

    open var html: String?
        set(value): Unit = set(3, value)
        get(): String? = get(3) as String?

    open var datetime: LocalDateTime?
        set(value): Unit = set(4, value)
        get(): LocalDateTime? = get(4) as LocalDateTime?

    open var createdDatetime: LocalDateTime?
        set(value): Unit = set(5, value)
        get(): LocalDateTime? = get(5) as LocalDateTime?

    open var fromMail: String?
        set(value): Unit = set(6, value)
        get(): String? = get(6) as String?

    open var subject: String?
        set(value): Unit = set(7, value)
        get(): String? = get(7) as String?

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    override fun key(): Record1<Int?> = super.key() as Record1<Int?>

    // -------------------------------------------------------------------------
    // Record8 type implementation
    // -------------------------------------------------------------------------

    override fun fieldsRow(): Row8<Int?, Int?, String?, String?, LocalDateTime?, LocalDateTime?, String?, String?> = super.fieldsRow() as Row8<Int?, Int?, String?, String?, LocalDateTime?, LocalDateTime?, String?, String?>
    override fun valuesRow(): Row8<Int?, Int?, String?, String?, LocalDateTime?, LocalDateTime?, String?, String?> = super.valuesRow() as Row8<Int?, Int?, String?, String?, LocalDateTime?, LocalDateTime?, String?, String?>
    override fun field1(): Field<Int?> = JUserMails.USER_MAILS.USER_MAIL_ID
    override fun field2(): Field<Int?> = JUserMails.USER_MAILS.USER_ID
    override fun field3(): Field<String?> = JUserMails.USER_MAILS.PLAIN
    override fun field4(): Field<String?> = JUserMails.USER_MAILS.HTML
    override fun field5(): Field<LocalDateTime?> = JUserMails.USER_MAILS.DATETIME
    override fun field6(): Field<LocalDateTime?> = JUserMails.USER_MAILS.CREATED_DATETIME
    override fun field7(): Field<String?> = JUserMails.USER_MAILS.FROM_MAIL
    override fun field8(): Field<String?> = JUserMails.USER_MAILS.SUBJECT
    override fun component1(): Int? = userMailId
    override fun component2(): Int? = userId
    override fun component3(): String? = plain
    override fun component4(): String? = html
    override fun component5(): LocalDateTime? = datetime
    override fun component6(): LocalDateTime? = createdDatetime
    override fun component7(): String? = fromMail
    override fun component8(): String? = subject
    override fun value1(): Int? = userMailId
    override fun value2(): Int? = userId
    override fun value3(): String? = plain
    override fun value4(): String? = html
    override fun value5(): LocalDateTime? = datetime
    override fun value6(): LocalDateTime? = createdDatetime
    override fun value7(): String? = fromMail
    override fun value8(): String? = subject

    override fun value1(value: Int?): JUserMailsRecord {
        set(0, value)
        return this
    }

    override fun value2(value: Int?): JUserMailsRecord {
        set(1, value)
        return this
    }

    override fun value3(value: String?): JUserMailsRecord {
        set(2, value)
        return this
    }

    override fun value4(value: String?): JUserMailsRecord {
        set(3, value)
        return this
    }

    override fun value5(value: LocalDateTime?): JUserMailsRecord {
        set(4, value)
        return this
    }

    override fun value6(value: LocalDateTime?): JUserMailsRecord {
        set(5, value)
        return this
    }

    override fun value7(value: String?): JUserMailsRecord {
        set(6, value)
        return this
    }

    override fun value8(value: String?): JUserMailsRecord {
        set(7, value)
        return this
    }

    override fun values(value1: Int?, value2: Int?, value3: String?, value4: String?, value5: LocalDateTime?, value6: LocalDateTime?, value7: String?, value8: String?): JUserMailsRecord {
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
     * Create a detached, initialised JUserMailsRecord
     */
    constructor(userMailId: Int? = null, userId: Int? = null, plain: String? = null, html: String? = null, datetime: LocalDateTime? = null, createdDatetime: LocalDateTime? = null, fromMail: String? = null, subject: String? = null): this() {
        this.userMailId = userMailId
        this.userId = userId
        this.plain = plain
        this.html = html
        this.datetime = datetime
        this.createdDatetime = createdDatetime
        this.fromMail = fromMail
        this.subject = subject
        resetChangedOnNotNull()
    }
}