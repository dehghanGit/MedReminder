package me.sandbad.medireminder.core.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import me.sandbad.medireminder.sqldelight.AppDatabase
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val databaseFile = File(appDataDir(), "medireminder.db")
        val databaseAlreadyExists = databaseFile.exists()
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.path}")
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)
        if (!databaseAlreadyExists) {
            AppDatabase.Schema.create(driver)
            driver.setUserVersion(AppDatabase.Schema.version)
        } else {
            val currentVersion = driver.getUserVersion()
            if (currentVersion < AppDatabase.Schema.version) {
                AppDatabase.Schema.migrate(driver, currentVersion, AppDatabase.Schema.version)
                driver.setUserVersion(AppDatabase.Schema.version)
            }
        }
        return driver
    }
}

private fun appDataDir(): File {
    val home = System.getProperty("user.home") ?: "."
    val os = System.getProperty("os.name").orEmpty().lowercase()
    val base = when {
        os.contains("mac") -> File(home, "Library/Application Support/MediReminder")
        os.contains("win") -> File(System.getenv("APPDATA") ?: home, "MediReminder")
        else -> File(home, ".local/share/medireminder")
    }
    base.mkdirs()
    return base
}

private fun SqlDriver.getUserVersion(): Long =
    executeQuery(
        identifier = null,
        sql = "PRAGMA user_version",
        mapper = { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L) },
        parameters = 0
    ).value

private fun SqlDriver.setUserVersion(version: Long) {
    execute(identifier = null, sql = "PRAGMA user_version = $version", parameters = 0)
}
