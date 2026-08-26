package me.sandbad.medireminder.core.repository.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import me.sandbad.medireminder.core.currentDateTime
import me.sandbad.medireminder.core.model.DoseLog
import me.sandbad.medireminder.core.model.DoseStatus
import me.sandbad.medireminder.core.repository.DoseLogRepository
import me.sandbad.medireminder.sqldelight.AppDatabase

class DoseLogRepositoryImpl(db: AppDatabase) : DoseLogRepository {
    private val queries = db.doseLogQueries

    override fun observeByDate(date: LocalDate): Flow<List<DoseLog>> =
        queries.selectByDate(date).asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map { it.toModel() } }

    override suspend fun getByDate(date: LocalDate): List<DoseLog> = withContext(Dispatchers.Default) {
        queries.selectByDate(date).executeAsList().map { it.toModel() }
    }

    override suspend fun getBetween(from: LocalDate, to: LocalDate): List<DoseLog> =
        withContext(Dispatchers.Default) {
            queries.selectBetween(from, to).executeAsList().map { it.toModel() }
        }

    override suspend fun getForMedicationBetween(
        medicationId: Long,
        from: LocalDate,
        to: LocalDate
    ): List<DoseLog> = withContext(Dispatchers.Default) {
        queries.selectByMedicationBetween(medicationId, from, to).executeAsList().map { it.toModel() }
    }

    override suspend fun getById(id: Long): DoseLog? = withContext(Dispatchers.Default) {
        queries.selectById(id).executeAsOneOrNull()?.toModel()
    }

    override suspend fun getPendingBefore(at: LocalDateTime): List<DoseLog> = withContext(Dispatchers.Default) {
        queries.selectPendingBefore(at).executeAsList().map { it.toModel() }
    }

    override suspend fun insertIfAbsent(dose: DoseLog) = withContext(Dispatchers.Default) {
        queries.insert(
            medicationId = dose.medicationId,
            scheduleId = dose.scheduleId,
            date = dose.date,
            scheduledAt = dose.scheduledAt,
            status = dose.status.name,
            quantity = dose.quantity,
            takenAt = dose.takenAt,
            note = dose.note,
            createdAt = dose.createdAt,
            updatedAt = dose.updatedAt
        )
    }

    override suspend fun updateStatus(
        id: Long,
        status: DoseStatus,
        takenAt: LocalDateTime?,
        note: String?
    ) = withContext(Dispatchers.Default) {
        queries.updateStatus(status.name, takenAt, note, currentDateTime(), id)
    }

    override suspend fun reschedule(id: Long, at: LocalDateTime) = withContext(Dispatchers.Default) {
        queries.reschedule(at, currentDateTime(), id)
    }

    override suspend fun markOverdueAsMissed(before: LocalDateTime) = withContext(Dispatchers.Default) {
        queries.markOverdueAsMissed(currentDateTime(), before)
    }

    override suspend fun deleteFuturePendingForSchedule(scheduleId: Long, from: LocalDateTime) =
        withContext(Dispatchers.Default) {
            queries.deleteFutureForSchedule(scheduleId, from)
        }
}
