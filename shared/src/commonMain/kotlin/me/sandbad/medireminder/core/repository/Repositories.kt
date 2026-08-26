package me.sandbad.medireminder.core.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import me.sandbad.medireminder.core.model.AppSettings
import me.sandbad.medireminder.core.model.DoseLog
import me.sandbad.medireminder.core.model.DoseStatus
import me.sandbad.medireminder.core.model.Medication
import me.sandbad.medireminder.core.model.Schedule

interface MedicationRepository {
    fun observeActive(): Flow<List<Medication>>
    fun observeAll(): Flow<List<Medication>>
    suspend fun getById(id: Long): Medication?
    suspend fun getAllActive(): List<Medication>
    suspend fun getAll(): List<Medication>
    suspend fun getLowStock(): List<Medication>
    suspend fun insert(medication: Medication): Long
    suspend fun update(medication: Medication)
    suspend fun setArchived(id: Long, archived: Boolean)
    suspend fun adjustStock(id: Long, amount: Double)
    suspend fun delete(id: Long)
}

interface ScheduleRepository {
    fun observeForMedication(medicationId: Long): Flow<List<Schedule>>
    suspend fun getById(id: Long): Schedule?
    suspend fun getForMedication(medicationId: Long): List<Schedule>
    suspend fun getAllActive(): List<Schedule>
    suspend fun insert(schedule: Schedule): Long
    suspend fun update(schedule: Schedule)
    suspend fun delete(id: Long)
    suspend fun deleteForMedication(medicationId: Long)
}

interface DoseLogRepository {
    fun observeByDate(date: LocalDate): Flow<List<DoseLog>>
    suspend fun getByDate(date: LocalDate): List<DoseLog>
    suspend fun getBetween(from: LocalDate, to: LocalDate): List<DoseLog>
    suspend fun getForMedicationBetween(medicationId: Long, from: LocalDate, to: LocalDate): List<DoseLog>
    suspend fun getById(id: Long): DoseLog?
    suspend fun getPendingBefore(at: LocalDateTime): List<DoseLog>
    suspend fun insertIfAbsent(dose: DoseLog)
    suspend fun updateStatus(id: Long, status: DoseStatus, takenAt: LocalDateTime?, note: String?)
    suspend fun reschedule(id: Long, at: LocalDateTime)
    suspend fun markOverdueAsMissed(before: LocalDateTime)
    suspend fun deleteFuturePendingForSchedule(scheduleId: Long, from: LocalDateTime)
}

interface AppSettingsRepository {
    fun observe(): Flow<AppSettings>
    suspend fun get(): AppSettings
    suspend fun save(settings: AppSettings)
}
