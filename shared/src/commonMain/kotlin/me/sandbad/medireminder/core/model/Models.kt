package me.sandbad.medireminder.core.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import me.sandbad.medireminder.core.currentDateTime

data class Medication(
    val id: Long = 0,
    val name: String,
    val form: MedicationForm = MedicationForm.TABLET,
    val strength: Double? = null,
    val strengthUnit: StrengthUnit = StrengthUnit.MG,
    val color: MedColor = MedColor.BLUE,
    val instructions: String? = null,
    val notes: String? = null,
    val stockCount: Double? = null,
    val refillAt: Double? = null,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val isArchived: Boolean = false,
    val createdAt: LocalDateTime = currentDateTime(),
    val updatedAt: LocalDateTime = currentDateTime()
) {
    /** "Metformin 500 mg" — the label used in lists and reminders. */
    val displayName: String
        get() = when {
            strength == null -> name
            strengthUnit == StrengthUnit.NONE -> "$name ${strength.trimZeros()}"
            else -> "$name ${strength.trimZeros()} ${strengthUnit.label}"
        }

    val needsRefill: Boolean
        get() = stockCount != null && refillAt != null && stockCount <= refillAt
}

data class Schedule(
    val id: Long = 0,
    val medicationId: Long,
    val scheduleType: ScheduleType = ScheduleType.DAILY,
    val timesOfDay: List<LocalTime> = emptyList(),
    val quantity: Double = 1.0,
    /** ISO day numbers, 1 = Monday .. 7 = Sunday. Only used by [ScheduleType.SPECIFIC_DAYS]. */
    val daysOfWeek: Set<Int> = emptySet(),
    val intervalDays: Int? = null,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = currentDateTime(),
    val updatedAt: LocalDateTime = currentDateTime()
)

data class DoseLog(
    val id: Long = 0,
    val medicationId: Long,
    val scheduleId: Long?,
    val date: LocalDate,
    val scheduledAt: LocalDateTime,
    val status: DoseStatus = DoseStatus.PENDING,
    val quantity: Double = 1.0,
    val takenAt: LocalDateTime? = null,
    val note: String? = null,
    val createdAt: LocalDateTime = currentDateTime(),
    val updatedAt: LocalDateTime = currentDateTime()
)

/** A dose joined with its medication — what the Today screen actually renders. */
data class DoseWithMedication(
    val dose: DoseLog,
    val medication: Medication
)

data class AppSettings(
    val ownerName: String? = null,
    val remindersEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val snoozeMinutes: Int = 10,
    val missedAfterMinutes: Int = 60,
    val refillAlertsEnabled: Boolean = true,
    val onboardingDone: Boolean = false,
    val updatedAt: LocalDateTime = currentDateTime()
)

/** Adherence over a window, used by the History screen. */
data class AdherenceStats(
    val taken: Int = 0,
    val skipped: Int = 0,
    val missed: Int = 0,
    val pending: Int = 0
) {
    val scored: Int get() = taken + skipped + missed
    val rate: Float get() = if (scored == 0) 0f else taken.toFloat() / scored
}

internal fun Double.trimZeros(): String {
    val rounded = (this * 100).toLong() / 100.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
}
