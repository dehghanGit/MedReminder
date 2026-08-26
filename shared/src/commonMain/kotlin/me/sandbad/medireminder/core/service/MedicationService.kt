package me.sandbad.medireminder.core.service

import kotlinx.datetime.LocalDate
import me.sandbad.medireminder.core.currentDateTime
import me.sandbad.medireminder.core.model.DoseLog
import me.sandbad.medireminder.core.model.DoseStatus
import me.sandbad.medireminder.core.model.DoseWithMedication
import me.sandbad.medireminder.core.model.Medication
import me.sandbad.medireminder.core.model.Schedule
import me.sandbad.medireminder.core.plusMinutes
import me.sandbad.medireminder.core.reminder.ReminderScheduler
import me.sandbad.medireminder.core.repository.AppSettingsRepository
import me.sandbad.medireminder.core.repository.DoseLogRepository
import me.sandbad.medireminder.core.repository.MedicationRepository
import me.sandbad.medireminder.core.repository.ScheduleRepository
import me.sandbad.medireminder.core.today

/**
 * The one place that knows how medications, schedules and dose logs fit together.
 * ViewModels talk to this rather than to repositories directly.
 */
class MedicationService(
    private val medications: MedicationRepository,
    private val schedules: ScheduleRepository,
    private val doses: DoseLogRepository,
    private val settings: AppSettingsRepository,
    private val reminders: ReminderScheduler
) {

    /** How many days of dose rows are materialised ahead of today. */
    private val horizonDays = 7

    suspend fun saveMedication(medication: Medication, schedules: List<Schedule>): Long {
        val medicationId = if (medication.id == 0L) {
            medications.insert(medication)
        } else {
            medications.update(medication)
            medication.id
        }
        replaceSchedules(medicationId, schedules)
        syncUpcoming()
        return medicationId
    }

    private suspend fun replaceSchedules(medicationId: Long, incoming: List<Schedule>) {
        val existing = schedules.getForMedication(medicationId)
        val keptIds = incoming.mapNotNull { it.id.takeIf { id -> id != 0L } }.toSet()
        existing.filter { it.id !in keptIds }.forEach { stale ->
            doses.deleteFuturePendingForSchedule(stale.id, currentDateTime())
            schedules.delete(stale.id)
        }
        incoming.forEach { schedule ->
            val withOwner = schedule.copy(medicationId = medicationId)
            if (withOwner.id == 0L) {
                schedules.insert(withOwner)
            } else {
                doses.deleteFuturePendingForSchedule(withOwner.id, currentDateTime())
                schedules.update(withOwner)
            }
        }
    }

    suspend fun archiveMedication(id: Long) {
        medications.setArchived(id, true)
        schedules.getForMedication(id).forEach {
            doses.deleteFuturePendingForSchedule(it.id, currentDateTime())
        }
        reminders.cancelAll()
        syncUpcoming()
    }

    suspend fun deleteMedication(id: Long) {
        medications.delete(id)
        reminders.cancelAll()
        syncUpcoming()
    }

    /**
     * Materialise dose rows for today plus the next [horizonDays], flip anything long
     * overdue to MISSED, and hand the resulting pending doses to the platform scheduler.
     */
    suspend fun syncUpcoming() {
        val config = settings.get()
        val now = currentDateTime()
        doses.markOverdueAsMissed(now.plusMinutes(-config.missedAfterMinutes))

        val activeMeds = medications.getAllActive()
        val schedulesByMed = schedules.getAllActive().groupBy { it.medicationId }
        var date = today()
        repeat(horizonDays + 1) {
            activeMeds.forEach { med ->
                schedulesByMed[med.id].orEmpty().forEach { schedule ->
                    DoseScheduler.timesOn(med, schedule, date).forEach { at ->
                        doses.insertIfAbsent(
                            DoseLog(
                                medicationId = med.id,
                                scheduleId = schedule.id,
                                date = date,
                                scheduledAt = at,
                                status = DoseStatus.PENDING,
                                quantity = schedule.quantity
                            )
                        )
                    }
                }
            }
            date = date.plusDays(1)
        }

        if (!config.remindersEnabled) {
            reminders.cancelAll()
            return
        }
        val medsById = activeMeds.associateBy { it.id }
        val upcoming = doses.getBetween(today(), today().plusDays(horizonDays))
            .filter { it.status == DoseStatus.PENDING && it.scheduledAt >= now }
            .mapNotNull { dose -> medsById[dose.medicationId]?.let { DoseWithMedication(dose, it) } }
        reminders.scheduleAll(upcoming, config)
    }

    suspend fun dosesFor(date: LocalDate): List<DoseWithMedication> {
        val medsById = medications.getAll().associateBy { it.id }
        return doses.getByDate(date)
            .mapNotNull { dose -> medsById[dose.medicationId]?.let { DoseWithMedication(dose, it) } }
            .sortedBy { it.dose.scheduledAt }
    }

    suspend fun markTaken(doseId: Long) {
        val dose = doses.getById(doseId) ?: return
        doses.updateStatus(doseId, DoseStatus.TAKEN, currentDateTime(), dose.note)
        medications.adjustStock(dose.medicationId, dose.quantity)
        reminders.cancel(doseId)
    }

    suspend fun markSkipped(doseId: Long, note: String? = null) {
        doses.updateStatus(doseId, DoseStatus.SKIPPED, null, note)
        reminders.cancel(doseId)
    }

    suspend fun undo(doseId: Long) {
        val dose = doses.getById(doseId) ?: return
        if (dose.status == DoseStatus.TAKEN) {
            medications.adjustStock(dose.medicationId, -dose.quantity)
        }
        doses.updateStatus(doseId, DoseStatus.PENDING, null, dose.note)
        syncUpcoming()
    }

    /** Push a dose out by the configured snooze interval. */
    suspend fun snooze(doseId: Long) {
        val dose = doses.getById(doseId) ?: return
        val minutes = settings.get().snoozeMinutes
        doses.reschedule(doseId, currentDateTime().plusMinutes(minutes))
        reminders.cancel(doseId)
        syncUpcoming()
    }

    suspend fun lowStockMedications(): List<Medication> =
        if (settings.get().refillAlertsEnabled) medications.getLowStock() else emptyList()
}
