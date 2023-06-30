package net.matsudamper.money.backend

import java.sql.Connection
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import net.matsudamper.money.backend.base.ServerEnv


internal object DbConnection {

    private val config = HikariConfig().also { config ->
        config.jdbcUrl = "jdbc:mariadb://${ServerEnv.dbHost}/${ServerEnv.dbSchema}"
        config.username = ServerEnv.dbUserName
        config.password = ServerEnv.dbPassword
    }
    private val dataSource = HikariDataSource(config)

    internal fun get(): Connection {
        return dataSource.connection
    }
}
