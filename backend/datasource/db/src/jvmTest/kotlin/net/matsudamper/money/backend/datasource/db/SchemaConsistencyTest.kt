package net.matsudamper.money.backend.datasource.db

import java.io.File
import java.sql.Connection
import java.sql.DriverManager

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.containers.MariaDBContainer

class SchemaConsistencyTest {

    companion object {
        private val mariadb: MariaDBContainer<*> = MariaDBContainer("mariadb:10.11")
            .withDatabaseName("money")
            .withUsername("test")
            .withPassword("test")

        private lateinit var connection: Connection

        private val sqlFilesInOrder: List<String> by lazy {
            val sqlDirUrl = SchemaConsistencyTest::class.java.getResource("/sql/")
                ?: error("SQL ディレクトリが見つかりません")
            val sqlDir = File(sqlDirUrl.toURI())
            val migrations = (sqlDir.listFiles() ?: error("SQL ファイルを列挙できません"))
                .filter { it.extension == "sql" && it.name != "master.sql" }
                .map { it.name }
                .sorted()
            listOf("master.sql") + migrations
        }

        val sqlApplyErrors: MutableList<Pair<String, String>> = mutableListOf()

        @BeforeAll
        @JvmStatic
        fun setup() {
            mariadb.start()
            connection = DriverManager.getConnection(mariadb.jdbcUrl, "test", "test")
            applySqlFiles()
        }

        @AfterAll
        @JvmStatic
        fun teardown() {
            connection.close()
            mariadb.stop()
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

    @Test
    fun `全SQLファイルがエラーなく適用できる`() {
        withClue(
            "SQLファイル適用エラー:\n" + sqlApplyErrors.joinToString("\n") { (file, msg) -> "  [$file] $msg" },
        ) {
            sqlApplyErrors.isEmpty() shouldBe true
        }
    }
}
