package net.matsudamper.money.db.schema

import org.jooq.Decfloat
import org.jooq.Geography
import org.jooq.Geometry
import org.jooq.JSON
import org.jooq.JSONB
import org.jooq.Record
import org.jooq.Result
import org.jooq.RowId
import org.jooq.XML
import org.jooq.types.DayToSecond
import org.jooq.types.UByte
import org.jooq.types.UInteger
import org.jooq.types.ULong
import org.jooq.types.UShort
import org.jooq.types.YearToMonth
import org.jooq.types.YearToSecond

// GraalVM native image のために jOOQ カスタム型の配列クラスを image の type universe に含める。
// これらのフィールド宣言により、GraalVM の静的解析が各配列型を到達可能として認識する。
// DefaultDataType.<init> 内で Class.arrayType() を呼び出す際に null を返さないために必要。
@Suppress("unused")
private object JooqNativeImageTypeRegistrar {
    private val uByte: Array<UByte>? = null
    private val uShort: Array<UShort>? = null
    private val uInteger: Array<UInteger>? = null
    private val uLong: Array<ULong>? = null
    private val dayToSecond: Array<DayToSecond>? = null
    private val yearToMonth: Array<YearToMonth>? = null
    private val yearToSecond: Array<YearToSecond>? = null
    private val rowId: Array<RowId>? = null
    private val record: Array<Record>? = null
    private val result: Array<Result<*>>? = null
    private val json: Array<JSON>? = null
    private val jsonb: Array<JSONB>? = null
    private val xml: Array<XML>? = null
    private val geography: Array<Geography>? = null
    private val geometry: Array<Geometry>? = null
    private val decfloat: Array<Decfloat>? = null
}
