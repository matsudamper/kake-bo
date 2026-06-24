package net.matsudamper.money.backend.datasource.db

import java.sql.Connection
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.opentelemetry.instrumentation.hikaricp.v3_0.HikariTelemetry
import io.opentelemetry.instrumentation.jdbc.datasource.JdbcTelemetry
import net.matsudamper.money.backend.base.OpenTelemetryInitializer
import net.matsudamper.money.backend.base.ServerEnv

interface DbConnection {
    fun <R> use(connectionBlock: (Connection) -> R): R
}

object DbConnectionImpl : DbConnection {
    private val config = HikariConfig().also { config ->
        config.jdbcUrl = "jdbc:mariadb://${ServerEnv.dbHost}:${ServerEnv.dbPort}/${ServerEnv.dbSchema}"
        config.username = ServerEnv.dbUserName
        config.password = ServerEnv.dbPassword
        config.connectionTimeout = 5 * 1000
    }
    private val dataSource by lazy {
        val openTelemetry = OpenTelemetryInitializer.get()
        config.metricsTrackerFactory = HikariTelemetry.create(openTelemetry)
            .createMetricsTrackerFactory(config.metricsTrackerFactory)
        val hikariDataSource = HikariDataSource(config)
        JdbcTelemetry.create(openTelemetry).wrap(hikariDataSource)
    }

    fun warmup() {
        use { connection ->
            connection.prepareStatement("SELECT 1").use { it.execute() }
        }
    }

    override fun <R> use(connectionBlock: (Connection) -> R): R {
        return dataSource.connection.use {
            connectionBlock(it)
        }
    }
}
