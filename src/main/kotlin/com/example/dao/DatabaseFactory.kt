package com.example.dao

import com.example.models.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.*
import org.jetbrains.exposed.sql.transactions.experimental.*
import java.io.File

object DatabaseFactory {
    fun init(config: ApplicationConfig) {
        val driverClassName = config.property("storage.driverClassName").getString()
        val jdbcURL = config.property(
            "storage.jdbcURL"
        ).getString() + (config.propertyOrNull("storage.dbFilePath")?.getString()?.let {
            File(it).canonicalFile.absoluteFile
        } ?: "")

        val database = Database.connect(createHikariDataSource(url = jdbcURL, driver = driverClassName))
        transaction(database) {
            SchemaUtils.create(Articles)
        }
    }

    private fun createHikariDataSource(
        url: String,
        driver: String
    ) = HikariDataSource(HikariConfig().apply {
        driverClassName = driver
        jdbcUrl = url
        maximumPoolSize = 3
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    })

    suspend fun <T> dbQuery(bloc: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { bloc() }
}