package me.sandbad.medireminder.core.service

import kotlinx.datetime.LocalTime
import me.sandbad.medireminder.core.at
import me.sandbad.medireminder.core.currentDateTime
import me.sandbad.medireminder.core.model.DoseLog
import me.sandbad.medireminder.core.model.DoseStatus
import me.sandbad.medireminder.core.model.MedColor
import me.sandbad.medireminder.core.model.Medication
import me.sandbad.medireminder.core.model.MedicationForm
import me.sandbad.medireminder.core.model.Schedule
import me.sandbad.medireminder.core.model.ScheduleType
import me.sandbad.medireminder.core.model.StrengthUnit
import me.sandbad.medireminder.core.repository.DoseLogRepository
import me.sandbad.medireminder.core.repository.MedicationRepository
import me.sandbad.medireminder.core.repository.ScheduleRepository
import me.sandbad.medireminder.core.today

/**
 * Populates the database with a realistic set of medications, schedules and a couple
 * of weeks of dose history the first time the app is opened. This gives the Home,
 * History and Progress screens something to render out of the box.
 *
 * Seeding is a no-op once any medication exists, so it never clobbers real user data.
 */
class DemoDataSeeder(
    private val medications: MedicationRepository,
    private val schedules: ScheduleRepository,
    private val doses: DoseLogRepository,
    private val service: MedicationService
) {

    /** How many past days of "all taken" history to fabricate, to seed a streak. */
    private val historyDays = 14

    suspend fun seedIfEmpty() {
        if (medications.getAll().isNotEmpty()) return
        seed()
    }

    private data class Demo(
        val medication: Medication,
        val times: List<LocalTime>,
        val quantity: Double
    )

    private suspend fun seed() {
        val start = today().plusDays(-(historyDays + 1))

        val demos = listOf(
            Demo(
                Medication(
                    name = "Omega 3",
                    form = MedicationForm.CAPSULE,
                    strength = 1000.0,
                    strengthUnit = StrengthUnit.MG,
                    color = MedColor.BLUE,
                    instructions = "Take with breakfast",
                    stockCount = 42.0,
                    refillAt = 10.0,
                    startDate = start
                ),
                times = listOf(LocalTime(8, 0)),
                quantity = 1.0
            ),
            Demo(
                Medication(
                    name = "Vitamin D",
                    form = MedicationForm.TABLET,
                    strength = 2000.0,
                    strengthUnit = StrengthUnit.IU,
                    color = MedColor.AMBER,
                    instructions = "Take after lunch",
                    stockCount = 26.0,
                    refillAt = 10.0,
                    startDate = start
                ),
                times = listOf(LocalTime(14, 30)),
                quantity = 1.0
            ),
            Demo(
                Medication(
                    name = "Metformin",
                    form = MedicationForm.TABLET,
                    strength = 500.0,
                    strengthUnit = StrengthUnit.MG,
                    color = MedColor.TEAL,
                    instructions = "Morning and evening with food",
                    stockCount = 58.0,
                    refillAt = 14.0,
                    startDate = start
                ),
                times = listOf(LocalTime(8, 0), LocalTime(20, 0)),
                quantity = 1.0
            ),
            Demo(
                Medication(
                    name = "Calcium",
                    form = MedicationForm.TABLET,
                    strength = 600.0,
                    strengthUnit = StrengthUnit.MG,
                    color = MedColor.ROSE,
                    instructions = "Take at bedtime",
                    stockCount = 8.0,
                    refillAt = 10.0,
                    startDate = start
                ),
                times = listOf(LocalTime(20, 0)),
                quantity = 1.0
            )
        )

        val seeded = demos.map { demo ->
            val medId = medications.insert(demo.medication)
            val scheduleId = schedules.insert(
                Schedule(
                    medicationId = medId,
                    scheduleType = ScheduleType.DAILY,
                    timesOfDay = demo.times,
                    quantity = demo.quantity
                )
            )
            Triple(medId, scheduleId, demo)
        }

        // Materialise today + upcoming pending doses and arm reminders.
        service.syncUpcoming()

        // Fabricate a run of fully-adhered past days so the streak and history look real.
        for (offset in historyDays downTo 1) {
            val date = today().plusDays(-offset)
            seeded.forEach { (medId, scheduleId, demo) ->
                demo.times.forEach { time ->
                    val at = date.at(time)
                    doses.insertIfAbsent(
                        DoseLog(
                            medicationId = medId,
                            scheduleId = scheduleId,
                            date = date,
                            scheduledAt = at,
                            status = DoseStatus.TAKEN,
                            quantity = demo.quantity,
                            takenAt = at
                        )
                    )
                }
            }
        }

        // Mark today's doses whose time has already passed as taken, leaving the rest
        // pending so the Home screen shows genuine progress with a real "next" dose.
        val now = currentDateTime()
        doses.getByDate(today())
            .filter { it.status == DoseStatus.PENDING && it.scheduledAt < now }
            .forEach { dose ->
                doses.updateStatus(dose.id, DoseStatus.TAKEN, dose.scheduledAt, null)
                medications.adjustStock(dose.medicationId, dose.quantity)
            }
    }
}
