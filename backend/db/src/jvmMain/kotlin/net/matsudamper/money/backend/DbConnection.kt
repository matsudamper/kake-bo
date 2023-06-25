package net.matsudamper.money.backend

import java.sql.DriverManager
import net.matsudamper.money.backend.base.ServerEnv

internal object DbConnection {
    private val connection = DriverManager.getConnection(
        "jdbc:mariadb://${ServerEnv.dbHost}/${ServerEnv.dbSchema}",
        ServerEnv.dbUserName,
        ServerEnv.dbPassword,
    )

    internal fun get() = connection
}