package me.sandbad.medireminder.core.database

import me.sandbad.medireminder.core.adapter.IntColumnAdapter
import me.sandbad.medireminder.core.adapter.LocalDateAdapter
import me.sandbad.medireminder.core.adapter.LocalDateTimeAdapter
import me.sandbad.medireminder.database.TblAppSettings
import me.sandbad.medireminder.database.TblDoseLog
import me.sandbad.medireminder.database.TblMedication
import me.sandbad.medireminder.database.TblSchedule
import me.sandbad.medireminder.sqldelight.AppDatabase

fun createDatabase(factory: DatabaseDriverFactory): AppDatabase {
    val driver = factory.createDriver()
    return AppDatabase(
        driver = driver,
        TblMedicationAdapter = TblMedication.Adapter(
            startDateAdapter = LocalDateAdapter,
            endDateAdapter = LocalDateAdapter,
            createdAtAdapter = LocalDateTimeAdapter,
            updatedAtAdapter = LocalDateTimeAdapter
        ),
        TblScheduleAdapter = TblSchedule.Adapter(
            intervalDaysAdapter = IntColumnAdapter,
            createdAtAdapter = LocalDateTimeAdapter,
            updatedAtAdapter = LocalDateTimeAdapter
        ),
        TblDoseLogAdapter = TblDoseLog.Adapter(
            dateAdapter = LocalDateAdapter,
            scheduledAtAdapter = LocalDateTimeAdapter,
            takenAtAdapter = LocalDateTimeAdapter,
            createdAtAdapter = LocalDateTimeAdapter,
            updatedAtAdapter = LocalDateTimeAdapter
        ),
        TblAppSettingsAdapter = TblAppSettings.Adapter(
            snoozeMinutesAdapter = IntColumnAdapter,
            missedAfterMinutesAdapter = IntColumnAdapter,
            updatedAtAdapter = LocalDateTimeAdapter
        )
    )
}
