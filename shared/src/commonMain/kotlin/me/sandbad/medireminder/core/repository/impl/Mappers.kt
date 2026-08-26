package me.sandbad.medireminder.core.repository.impl

import me.sandbad.medireminder.core.decodeDays
import me.sandbad.medireminder.core.decodeTimes
import me.sandbad.medireminder.core.model.AppSettings
import me.sandbad.medireminder.core.model.DoseLog
import me.sandbad.medireminder.core.model.DoseStatus
import me.sandbad.medireminder.core.model.MedColor
import me.sandbad.medireminder.core.model.Medication
import me.sandbad.medireminder.core.model.MedicationForm
import me.sandbad.medireminder.core.model.Schedule
import me.sandbad.medireminder.core.model.ScheduleType
import me.sandbad.medireminder.core.model.StrengthUnit
import me.sandbad.medireminder.database.TblAppSettings
import me.sandbad.medireminder.database.TblDoseLog
import me.sandbad.medireminder.database.TblMedication
import me.sandbad.medireminder.database.TblSchedule

internal fun TblMedication.toModel() = Medication(
    id = id,
    name = name,
    form = enumOrDefault(form, MedicationForm.OTHER),
    strength = strength,
    strengthUnit = enumOrDefault(strengthUnit, StrengthUnit.NONE),
    color = MedColor.fromName(color),
    instructions = instructions,
    notes = notes,
    stockCount = stockCount,
    refillAt = refillAt,
    startDate = startDate,
    endDate = endDate,
    isArchived = isArchived,
    createdAt = createdAt,
    updatedAt = updatedAt
)

internal fun TblSchedule.toModel() = Schedule(
    id = id,
    medicationId = medicationId,
    scheduleType = enumOrDefault(scheduleType, ScheduleType.DAILY),
    timesOfDay = timesOfDay.decodeTimes(),
    quantity = quantity,
    daysOfWeek = daysOfWeek.decodeDays(),
    intervalDays = intervalDays,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

internal fun TblDoseLog.toModel() = DoseLog(
    id = id,
    medicationId = medicationId,
    scheduleId = scheduleId,
    date = date,
    scheduledAt = scheduledAt,
    status = enumOrDefault(status, DoseStatus.PENDING),
    quantity = quantity,
    takenAt = takenAt,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt
)

internal fun TblAppSettings.toModel() = AppSettings(
    ownerName = ownerName,
    remindersEnabled = remindersEnabled,
    soundEnabled = soundEnabled,
    vibrationEnabled = vibrationEnabled,
    snoozeMinutes = snoozeMinutes,
    missedAfterMinutes = missedAfterMinutes,
    refillAlertsEnabled = refillAlertsEnabled,
    onboardingDone = onboardingDone,
    updatedAt = updatedAt
)

/** Enum values are stored as text, so tolerate rows written by an older/newer build. */
private inline fun <reified T : Enum<T>> enumOrDefault(value: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: fallback
