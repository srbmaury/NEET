package com.neet.backend.db

import com.neet.backend.config.AppConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.net.URI

/**
 * Neon (and most Postgres hosts) hand out connection strings in libpq/URI style —
 * `postgresql://user:password@host/db?sslmode=require` — but the JDBC driver only accepts
 * `jdbc:postgresql://host/db?params` with credentials passed separately. Normalize so the raw
 * string from a provider's dashboard can be pasted into DATABASE_URL as-is.
 */
private fun toJdbcDataSource(databaseUrl: String): HikariDataSource {
    val uri = URI(databaseUrl.removePrefix("jdbc:"))
    val userInfo = uri.userInfo?.split(":", limit = 2)
    // Without reWriteBatchedInserts, the PostgreSQL JDBC driver's executeBatch() still sends one
    // round-trip per row even for a "batched" statement (a well-known driver gotcha) — this is
    // what makes Exposed's batchUpsert actually collapse a multi-row upsert into one statement,
    // which is the whole point of using it against a remote/serverless Postgres instance.
    val query = (uri.rawQuery?.let { "$it&" }.orEmpty()) + "reWriteBatchedInserts=true"
    val jdbcUrl = "jdbc:postgresql://${uri.host}${uri.port.takeIf { it != -1 }?.let { ":$it" }.orEmpty()}${uri.path}?$query"

    val hikariConfig = HikariConfig().apply {
        this.jdbcUrl = jdbcUrl
        driverClassName = "org.postgresql.Driver"
        username = userInfo?.getOrNull(0)
        password = userInfo?.getOrNull(1)
        // Solo-scale app on serverless Postgres — keep the pool small regardless of what the
        // provider's own pooler allows, to stay well under any concurrent-connection limit.
        maximumPoolSize = 5
    }
    return HikariDataSource(hikariConfig)
}

fun initDatabase(config: AppConfig) {
    val dataSource = toJdbcDataSource(config.databaseUrl)
    Database.connect(dataSource)
    transaction {
        SchemaUtils.createMissingTablesAndColumns(
            Users, AnsweredQuestions, MockTestSessions, MockTestQuestions, TopicNotes,
        )
    }
}
