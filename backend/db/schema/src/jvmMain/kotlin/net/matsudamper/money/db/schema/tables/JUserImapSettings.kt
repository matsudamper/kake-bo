/*
 * This file is generated by jOOQ.
 */
package net.matsudamper.money.db.schema.tables


import java.util.function.Function

import net.matsudamper.money.db.schema.JMoney
import net.matsudamper.money.db.schema.keys.KEY_USER_IMAP_SETTINGS_PRIMARY
import net.matsudamper.money.db.schema.tables.records.JUserImapSettingsRecord

import org.jooq.Field
import org.jooq.ForeignKey
import org.jooq.Name
import org.jooq.Record
import org.jooq.Records
import org.jooq.Row5
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
open class JUserImapSettings(
    alias: Name,
    child: Table<out Record>?,
    path: ForeignKey<out Record, JUserImapSettingsRecord>?,
    aliased: Table<JUserImapSettingsRecord>?,
    parameters: Array<Field<*>?>?
): TableImpl<JUserImapSettingsRecord>(
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
         * The reference instance of <code>money.user_imap_settings</code>
         */
        val USER_IMAP_SETTINGS: JUserImapSettings = JUserImapSettings()
    }

    /**
     * The class holding records for this type
     */
    override fun getRecordType(): Class<JUserImapSettingsRecord> = JUserImapSettingsRecord::class.java

    /**
     * The column <code>money.user_imap_settings.user_id</code>.
     */
    val USER_ID: TableField<JUserImapSettingsRecord, Int?> = createField(DSL.name("user_id"), SQLDataType.INTEGER.nullable(false), this, "")

    /**
     * The column <code>money.user_imap_settings.host</code>.
     */
    val HOST: TableField<JUserImapSettingsRecord, String?> = createField(DSL.name("host"), SQLDataType.VARCHAR(500).defaultValue(DSL.field(DSL.raw("NULL"), SQLDataType.VARCHAR)), this, "")

    /**
     * The column <code>money.user_imap_settings.port</code>.
     */
    val PORT: TableField<JUserImapSettingsRecord, Int?> = createField(DSL.name("port"), SQLDataType.INTEGER.defaultValue(DSL.field(DSL.raw("NULL"), SQLDataType.INTEGER)), this, "")

    /**
     * The column <code>money.user_imap_settings.use_name</code>.
     */
    val USE_NAME: TableField<JUserImapSettingsRecord, String?> = createField(DSL.name("use_name"), SQLDataType.VARCHAR(500).defaultValue(DSL.field(DSL.raw("NULL"), SQLDataType.VARCHAR)), this, "")

    /**
     * The column <code>money.user_imap_settings.password</code>.
     */
    val PASSWORD: TableField<JUserImapSettingsRecord, String?> = createField(DSL.name("password"), SQLDataType.VARCHAR(500).defaultValue(DSL.field(DSL.raw("NULL"), SQLDataType.VARCHAR)), this, "")

    private constructor(alias: Name, aliased: Table<JUserImapSettingsRecord>?): this(alias, null, null, aliased, null)
    private constructor(alias: Name, aliased: Table<JUserImapSettingsRecord>?, parameters: Array<Field<*>?>?): this(alias, null, null, aliased, parameters)

    /**
     * Create an aliased <code>money.user_imap_settings</code> table reference
     */
    constructor(alias: String): this(DSL.name(alias))

    /**
     * Create an aliased <code>money.user_imap_settings</code> table reference
     */
    constructor(alias: Name): this(alias, null)

    /**
     * Create a <code>money.user_imap_settings</code> table reference
     */
    constructor(): this(DSL.name("user_imap_settings"), null)

    constructor(child: Table<out Record>, key: ForeignKey<out Record, JUserImapSettingsRecord>): this(Internal.createPathAlias(child, key), child, key, USER_IMAP_SETTINGS, null)
    override fun getSchema(): Schema? = if (aliased()) null else JMoney.MONEY
    override fun getPrimaryKey(): UniqueKey<JUserImapSettingsRecord> = KEY_USER_IMAP_SETTINGS_PRIMARY
    override fun `as`(alias: String): JUserImapSettings = JUserImapSettings(DSL.name(alias), this)
    override fun `as`(alias: Name): JUserImapSettings = JUserImapSettings(alias, this)
    override fun `as`(alias: Table<*>): JUserImapSettings = JUserImapSettings(alias.getQualifiedName(), this)

    /**
     * Rename this table
     */
    override fun rename(name: String): JUserImapSettings = JUserImapSettings(DSL.name(name), null)

    /**
     * Rename this table
     */
    override fun rename(name: Name): JUserImapSettings = JUserImapSettings(name, null)

    /**
     * Rename this table
     */
    override fun rename(name: Table<*>): JUserImapSettings = JUserImapSettings(name.getQualifiedName(), null)

    // -------------------------------------------------------------------------
    // Row5 type methods
    // -------------------------------------------------------------------------
    override fun fieldsRow(): Row5<Int?, String?, Int?, String?, String?> = super.fieldsRow() as Row5<Int?, String?, Int?, String?, String?>

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Function)}.
     */
    fun <U> mapping(from: (Int?, String?, Int?, String?, String?) -> U): SelectField<U> = convertFrom(Records.mapping(from))

    /**
     * Convenience mapping calling {@link SelectField#convertFrom(Class,
     * Function)}.
     */
    fun <U> mapping(toType: Class<U>, from: (Int?, String?, Int?, String?, String?) -> U): SelectField<U> = convertFrom(toType, Records.mapping(from))
}