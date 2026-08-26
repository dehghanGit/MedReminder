package me.sandbad.medireminder.core.repository.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.sandbad.medireminder.core.currentDateTime
import me.sandbad.medireminder.core.model.AppSettings
import me.sandbad.medireminder.core.repository.AppSettingsRepository
import me.sandbad.medireminder.sqldelight.AppDatabase

class AppSettingsRepositoryImpl(db: AppDatabase) : AppSettingsRepository {
    private val queries = db.appSettingsQueries

    override fun observe(): Flow<AppSettings> =
        queries.select().asFlow().mapToOneOrNull(Dispatchers.Default).map { it?.toModel() ?: AppSettings() }

    override suspend fun get(): AppSettings = withContext(Dispatchers.Default) {
        queries.select().executeAsOneOrNull()?.toModel() ?: AppSettings()
    }

    override suspend fun save(settings: AppSettings) = withContext(Dispatchers.Default) {
        queries.upsert(
            ownerName = settings.ownerName,
            remindersEnabled = settings.remindersEnabled,
            soundEnabled = settings.soundEnabled,
            vibrationEnabled = settings.vibrationEnabled,
            snoozeMinutes = settings.snoozeMinutes,
            missedAfterMinutes = settings.missedAfterMinutes,
            refillAlertsEnabled = settings.refillAlertsEnabled,
            onboardingDone = settings.onboardingDone,
            updatedAt = currentDateTime()
        )
    }
}
