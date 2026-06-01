package net.matsudamper.money.backend.datasource.db

import java.sql.Connection
import java.sql.DriverManager
import java.sql.Types
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import net.matsudamper.money.db.schema.JMoney
import org.jooq.DataType
import org.jooq.Table
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class SchemaConsistencyTest {

    companion object {
        @Container
        @JvmStatic
        val mariadb: MariaDBContainer<*> = MariaDBContainer("mariadb:10.11")
            .withDatabaseName("money")
            .withUsername("test")
            .withPassword("test")

        private lateinit var connection: Connection

        private val sqlFilesInOrder = listOf(
            "master.sql",
            "2023-08-11.sql",
            "2023-12-31.sql",
            "2024-03-04.sql",
            "2026-02-10.sql",
            "2026-02-14.sql",
            "2026-02-19.sql",
            "2026-02-24.sql",
            "2026-05-05.sql",
        )

        val sqlApplyErrors: MutableList<Pair<String, String>> = mutableListOf()

        @BeforeAll
        @JvmStatic
        fun setup() {
            connection = DriverManager.getConnection(mariadb.jdbcUrl, "test", "test")
            applySqlFiles()
        }

        @AfterAll
        @JvmStatic
        fun teardown() {
            connection.close()
        }

        private fun applySqlFiles() {
            sqlFilesInOrder.forEach { fileName ->
                val sql = SchemaConsistencyTest::class.java
                    .getResourceAsStream("/sql/$fileName")
                    ?.readBytes()
                    ?.toString(Charsets.UTF_8)
                    ?: error("SQLファイルが見つかりません: $fileName")

                val statements = sql
                    .split(";")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                statements.forEach { stmt ->
                    try {
                        connection.createStatement().use { it.execute(stmt) }
                    } catch (e: Exception) {
                        sqlApplyErrors.add(fileName to "${e.message} [SQL: ${stmt.take(120)}]")
                    }
                }
            }
        }
    }

    data class ColumnInfo(
        val columnName: String,
        val dataType: String,
        val isNullable: Boolean,
    )

    private fun fetchActualColumns(): Map<String, List<ColumnInfo>> {
        val result = mutableMapOf<String, MutableList<ColumnInfo>>()
        connection.createStatement().use { stmt ->
            stmt.executeQuery(
                """
                SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, IS_NULLABLE
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = 'money'
                ORDER BY TABLE_NAME, ORDINAL_POSITION
                """.trimIndent(),
            ).use { rs ->
                while (rs.next()) {
                    val tableName = rs.getString("TABLE_NAME")
                    val col = ColumnInfo(
                        columnName = rs.getString("COLUMN_NAME"),
                        dataType = rs.getString("DATA_TYPE"),
                        isNullable = rs.getString("IS_NULLABLE") == "YES",
                    )
                    result.getOrPut(tableName) { mutableListOf() }.add(col)
                }
            }
        }
        return result
    }

    private fun jooqDataTypeToMariadb(dataType: DataType<*>): String {
        return when (dataType.sqlType) {
            Types.INTEGER, Types.SMALLINT -> "int"
            Types.BIGINT -> "bigint"
            Types.VARCHAR -> "varchar"
            Types.CHAR -> "char"
            Types.BINARY, Types.VARBINARY -> "binary"
            Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> "datetime"
            Types.BOOLEAN, Types.BIT, Types.TINYINT -> "tinyint"
            Types.CLOB, Types.LONGVARCHAR -> {
                val length = dataType.length()
                if (length > 0 && length <= 65535) "text" else "longtext"
            }
            else -> dataType.typeName.lowercase()
        }
    }

    private fun expectedColumnsFromJooq(table: Table<*>): List<ColumnInfo> {
        return table.fields().map { field ->
            ColumnInfo(
                columnName = field.name,
                dataType = jooqDataTypeToMariadb(field.dataType),
                isNullable = field.dataType.nullable(),
            )
        }
    }

    @Test
    fun `全SQLファイルがエラーなく適用できる`() {
        withClue(
            "SQLファイル適用エラー:\n" + sqlApplyErrors.joinToString("\n") { (file, msg) -> "  [$file] $msg" },
        ) {
            sqlApplyErrors.isEmpty() shouldBe true
        }
    }

    @Test
    fun `jOOQコードの全テーブルがDBに存在する`() {
        val actualColumns = fetchActualColumns()
        val actualTableNames = actualColumns.keys

        val missingTables = JMoney.MONEY.tables
            .map { it.name }
            .filter { it !in actualTableNames }

        withClue("DBに存在しないテーブル: $missingTables") {
            missingTables.isEmpty() shouldBe true
        }
    }

    @Test
    fun `jOOQコードの全カラムがDBに存在しnullable状態が一致する`() {
        val actualColumns = fetchActualColumns()
        val differences = mutableListOf<String>()

        JMoney.MONEY.tables.forEach { table ->
            val actualCols = actualColumns[table.name]
            if (actualCols == null) {
                differences.add("テーブル '${table.name}' がDBに存在しない")
                return@forEach
            }

            val actualColMap = actualCols.associateBy { it.columnName }
            expectedColumnsFromJooq(table).forEach { expected ->
                val actual = actualColMap[expected.columnName]
                if (actual == null) {
                    differences.add("${table.name}.${expected.columnName}: DBにカラムが存在しない (期待型: ${expected.dataType})")
                } else {
                    if (actual.dataType != expected.dataType) {
                        differences.add(
                            "${table.name}.${expected.columnName}: 型不一致 (DB=${actual.dataType}, jOOQ=${expected.dataType})",
                        )
                    }
                    if (actual.isNullable != expected.isNullable) {
                        differences.add(
                            "${table.name}.${expected.columnName}: nullable不一致 (DB=${actual.isNullable}, jOOQ=${expected.isNullable})",
                        )
                    }
                }
            }
        }

        withClue("スキーマの差異:\n" + differences.joinToString("\n") { "  $it" }) {
            differences.isEmpty() shouldBe true
        }
    }

    @Test
    fun `DBに存在するがjOOQコードに存在しないテーブルを報告する`() {
        val actualColumns = fetchActualColumns()
        val jooqTableNames = JMoney.MONEY.tables.map { it.name }.toSet()

        // 主キーなしのためjOOQがコード生成しないテーブルは除外する
        val knownNonJooqTables = setOf("user_received_mails")

        val extraTables = actualColumns.keys.filter { it !in jooqTableNames && it !in knownNonJooqTables }

        withClue("jOOQコードに存在しないDBテーブル: $extraTables") {
            extraTables.isEmpty() shouldBe true
        }
    }
}
