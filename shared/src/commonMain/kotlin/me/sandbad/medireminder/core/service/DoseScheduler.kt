package me.sandbad.medireminder.core.service

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import me.sandbad.medireminder.core.at
import me.sandbad.medireminder.core.model.Medication
import me.sandbad.medireminder.core.model.Schedule
import me.sandbad.medireminder.core.model.ScheduleType

/**
 * Pure calendar maths: which dose times a schedule produces on a given day.
 * Kept free of repositories so it can be unit tested without a database.
 */
object DoseScheduler {

    fun occursOn(medication: Medication, schedule: Schedule, date: LocalDate): Boolean {
        if (!schedule.isActive || medication.isArchived) return false
        if (date < medication.startDate) return false
        medication.endDate?.let { if (date > it) return false }

        return when (schedule.scheduleType) {
            ScheduleType.DAILY -> true
            ScheduleType.SPECIFIC_DAYS -> date.dayOfWeek.isoDayNumber in schedule.daysOfWeek
            ScheduleType.INTERVAL_DAYS -> {
                val step = schedule.intervalDays ?: return false
                if (step <= 0) false else medication.startDate.daysUntil(date) % step == 0
            }
            ScheduleType.AS_NEEDED -> false
        }
    }

    /** Every scheduled instant for [date], in chronological order. */
    fun timesOn(medication: Medication, schedule: Schedule, date: LocalDate): List<LocalDateTime> =
        if (!occursOn(medication, schedule, date)) emptyList()
        else schedule.timesOfDay.sorted().map { date.at(it) }

    /** The next moment this schedule fires at or after [from], searching up to [horizonDays] ahead. */
    fun nextOccurrence(
        medication: Medication,
        schedule: Schedule,
        from: LocalDateTime,
        horizonDays: Int = 60
    ): LocalDateTime? {
        var date = from.date
        repeat(horizonDays) {
            timesOn(medication, schedule, date).firstOrNull { it >= from }?.let { return it }
            date = date.plusDays(1)
        }
        return null
    }
}

internal fun LocalDate.plusDays(days: Int): LocalDate =
    LocalDate.fromEpochDays(toEpochDays() + days)
