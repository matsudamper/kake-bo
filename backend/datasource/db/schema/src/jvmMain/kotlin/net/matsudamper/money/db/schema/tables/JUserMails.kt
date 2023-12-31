/*
 * This file is generated by jOOQ.
 */
package net.matsudamper.money.db.schema.tables


import java.time.LocalDateTime
import java.util.function.Function

import kotlin.collections.List

import net.matsudamper.money.db.schema.JMoney
import net.matsudamper.money.db.schema.indexes.USER_MAILS_USER_ID
import net.matsudamper.money.db.schema.indexes.USER_MAILS_USER_INDEX
import net.matsudamper.money.db.schema.keys.KEY_USER_MAILS_PRIMARY
import net.matsudamper.money.db.schema.tables.records.JUserMailsRecord

import org.jooq.Field
import org.jooq.ForeignKey
import org.jooq.Identity
import org.jooq.Index
import org.jooq.Name
import org.jooq.Record
import org.jooq.Records
import org.jooq.Row8
import org.jooq.Schema
import org.jooq.SelectField
import org.jooq.Table
import org.jooq.TableField
import org.jooq.TableOptions
import org.jooq.UniqueKey
import org.jooq.impl.DSL
import org.jooq.impl.Internal
import org.jooq.impl.SQLDataType
import org.jooq.impl.TableImpl


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class JUserMails(
    alias: Name,
    child: Table<out Record>?,
    path: ForeignKey<out Record, JUserMailsRecord>?,
    aliased: Table<JUserMailsRecord>?,
    parameters: Array<Field<*>?>?
): TableImpl<JUserMailsRecord>(
    alias,
    JMoney.MONEY,
    child,
    path,
    aliased,
    parameters,
    DSL.comment(""),
    TableOptions.table()
) {
    companion object {

        /**
         * The reference instance of <code>money.user_mails</code>
         */
        val USER_MAILS: JUserMails = JUserMails()
    }

    /**
     * The class holding records for this type
     */
    override fun getRecordType(): Class<JUserMailsRecord> = JUserMailsRecord::class.java

    /**
     * The column <code>money.user_mails.user_mail_id</code>.
     */
    val USER_MAIL_ID: TableField<JUserMailsRecord, Int?> = createField(DSL.name("user_mail_id"), SQLDataType.INTEGER.nullable(false).identity(true), this, "")

    /**
     * The column <code>money.user_mails.user_id</code>.
     */
    val USER_ID: TableField<JUserMailsRecord, Int?> = createField(DSL.name("user_id"), SQLDataType.INTEGER.nullable(false), this, "")

    /**
     * The column <code>money.user_mails.plain</code>.
     */
    val PLAIN: TableField<JUserMailsRecord, String?> = createField(DSL.name("plain"), SQLDataType.CLOB.defaultValue(DSL.field(DSL.raw("NULL"), SQLDataType.CLOB)), this, "")

    /**
     * The column <code>money.user_mails.html</code>.
     */
    val HTML: TableField<JUserMailsRecord, String?> = createField(DSL.name("html"), SQLDataType.CLOB.defaultValue(DSL.field(DSL.raw("NULL"), SQLDataType.CLOB)), this, "")

    /**
     * The column <code>money.user_mails.datetime</code>.
     */
    val DATETIME: TableField<JUserMailsRecord, LocalDateTime?> = createField(DSL.name("datetime"), SQLDataType.LOCALDATETIME(0).nullable(false), this, "")

    /**
     * The column <code>money.user_mails.created_datetime</code>.
     */
    val CREATED_DATETIME: TableField<JUserMailsRecord, LocalDateTime?> = createField(DSL.name("created_datetime"), SQLDataType.LOCALDATETIME(0).nullable(false).defaultValue(DSL.field(DSL.raw("current_timestamp()"), SQLDataType.LOCALDATETIME)), this, "")

    /**
     * The column <code>money.user_mails.from_mail</code>.
     */
    val FROM_MAIL: TableField<JUserMailsRecord, String?> = createField(DSL.name("from_mail"), SQLDataType.VARCHAR(500).nullable(false), this, "")

    /**
     * The column <code>money.user_mails.subject</code>.
     */
    val SUBJECT: TableField<JUserMailsRecord, String?> = createField(DSL.name("subject"), SQLDataType.VARCHAR(500).nullable(false), this, "")

    private constructor(alias: Name, aliased: Table<JUserMailsRecord>?): this(alias, null, null, aliased, null)
    private constructor(alias: Name, aliased: Table<JUserMailsRecord>?, parameters: Array<Field<*>?>?): this(alias, null, null, aliased, parameters)

    /**
     * Create an aliased <code>money.user_mails</code> table reference
     */
    constructor(alias: String): this(DSL.name(alias))

    /**
     * Create an aliased <code>money.user_mails</code> table reference
     */
    constructor(alias: Name): this(alias, null)

    /**
     * Create a <code>money.user_mails</code> table reference
     */
    constructor(): this(DSL.name("user_mails"), null)

    constructor(child: Table<out Record>, key: ForeignKey<out Record, JUserMailsRecord>): this(Internal.createPathAlias(child, key), child, key, USER_MAILS, null)
    override fun getSchema(): Schema? = if (aliased()) null else JMoney.MONEY
    override fun getIndexes(): List<Index> = listOf(USER_MAILS_USER_ID, USER_MAILS_USER_INDEX)
    override fun getIdentity(): Identity<JUserMailsRecord, Int?> = super.getIdentity() as Identity<JUserMailsRecord, Int?>
    override fun getPrimaryKey(): UniqueKey<JUserMailsRecord> = KEY_USER_MAILS_PRIMARY
    override fun `as`(alias: String): JUserMails = JUserMails(DSL.name(alias), this)
    override fun `as`(alias: Name): JUserMails = JUserMails(alias, this)
    override fun `as`(alias: Table<*>): JUserMails = JUserMails(alias.getQualifiedName(), this)

    /**
     * Rename this table
     */
    override fun rename(name: String): JUserMails = JUserMails(DSL.name(name), null)

    /**
     * Rename this table
     */
    override fun rename(name: Name): JUserMails = JUserMails(name, null)

    /**
     * Rename this table
     */
    override fun rename(name: Table<*>): JUserMails = JUserMails(name.getQualifiedName(), null)

    // -------------------------------------------------------------------------
    // Row8 type methods
    // -------------------------------------------------------------------------
    override fun fieldsRow(): Row8<Int?, Int?, String?, String?, LocalDateTime?, LocalDateTime?, String?, String?> = super.fieldsRow() as Row8<Int?, Int?, String?, String?, LocalDateTime?, LocalDateTime?, String?, String?>

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    fun <U> mapping(from: (Int?, Int?, String?, String?, LocalDateTime?, LocalDateTime?, String?, String?) -> U): SelectField<U> = convertFrom(Records.mapping(from))

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    fun <U> mapping(toType: Class<U>, from: (Int?, Int?, String?, String?, LocalDateTime?, LocalDateTime?, String?, String?) -> U): SelectField<U> = convertFrom(toType, Records.mapping(from))
}
