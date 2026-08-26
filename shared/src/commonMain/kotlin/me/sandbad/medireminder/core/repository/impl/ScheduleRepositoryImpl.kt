package me.sandbad.medireminder.core.repository.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.sandbad.medireminder.core.currentDateTime
import me.sandbad.medireminder.core.encodeDays
import me.sandbad.medireminder.core.encodeTimes
import me.sandbad.medireminder.core.model.Schedule
import me.sandbad.medireminder.core.repository.ScheduleRepository
import me.sandbad.medireminder.sqldelight.AppDatabase

class ScheduleRepositoryImpl(db: AppDatabase) : ScheduleRepository {
    private val queries = db.scheduleQueries

    override fun observeForMedication(medicationId: Long): Flow<List<Schedule>> =
        queries.selectByMedication(medicationId).asFlow().mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toModel() } }

    override suspend fun getById(id: Long): Schedule? = withContext(Dispatchers.Default) {
        queries.selectById(id).executeAsOneOrNull()?.toModel()
    }

    override suspend fun getForMedication(medicationId: Long): List<Schedule> = withContext(Dispatchers.Default) {
        queries.selectByMedication(medicationId).executeAsList().map { it.toModel() }
    }

    override suspend fun getAllActive(): List<Schedule> = withContext(Dispatchers.Default) {
        queries.selectAllActive().executeAsList().map { it.toModel() }
    }

    override suspend fun insert(schedule: Schedule): Long = withContext(Dispatchers.Default) {
        queries.transactionWithResult {
            queries.insert(
                medicationId = schedule.medicationId,
                scheduleType = schedule.scheduleType.name,
                timesOfDay = schedule.timesOfDay.encodeTimes(),
                quantity = schedule.quantity,
                daysOfWeek = schedule.daysOfWeek.encodeDays(),
                intervalDays = schedule.intervalDays,
                isActive = schedule.isActive,
                createdAt = schedule.createdAt,
                updatedAt = schedule.updatedAt
            )
            queries.lastInsertedId().executeAsOne()
        }
    }

    override suspend fun update(schedule: Schedule) = withContext(Dispatchers.Default) {
        queries.update(
            scheduleType = schedule.scheduleType.name,
            timesOfDay = schedule.timesOfDay.encodeTimes(),
            quantity = schedule.quantity,
            daysOfWeek = schedule.daysOfWeek.encodeDays(),
            intervalDays = schedule.intervalDays,
            isActive = schedule.isActive,
            updatedAt = currentDateTime(),
            id = schedule.id
        )
    }

    override suspend fun delete(id: Long) = withContext(Dispatchers.Default) {
        queries.deleteById(id)
    }

    override suspend fun deleteForMedication(medicationId: Long) = withContext(Dispatchers.Default) {
        queries.deleteByMedication(medicationId)
    }
}
