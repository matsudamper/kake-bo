package net.matsudamper.money.backend

import java.sql.Connection
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import net.matsudamper.money.backend.base.ServerEnv


internal object DbConnection {

    private val config = HikariConfig().also { config ->
        config.jdbcUrl = "jdbc:mariadb://${ServerEnv.dbHost}/${ServerEnv.dbSchema}"
        config.username = ServerEnv.dbUserName
        config.password = ServerEnv.dbPassword
        config.connectionTimeout = 5 * 1000
    }
    private val dataSource by lazy { HikariDataSource(config) }

    @OptIn(ExperimentalContracts::class)
    internal fun <R> use(connectionBlock: (Connection) -> R): R {
        contract {
            callsInPlace(connectionBlock, InvocationKind.EXACTLY_ONCE)
        }
        return dataSource.connection.use {
            connectionBlock(it)
        }
    }
}
