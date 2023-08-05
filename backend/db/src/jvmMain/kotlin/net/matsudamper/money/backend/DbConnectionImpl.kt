package net.matsudamper.money.backend

import java.sql.Connection
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
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
    private val dataSource by lazy { HikariDataSource(config) }

    override fun <R> use(connectionBlock: (Connection) -> R): R {
        return dataSource.connection.use {
            connectionBlock(it)
        }
    }
}
