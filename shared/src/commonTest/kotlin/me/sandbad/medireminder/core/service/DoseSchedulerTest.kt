package me.sandbad.medireminder.core.service

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import me.sandbad.medireminder.core.model.Medication
import me.sandbad.medireminder.core.model.Schedule
import me.sandbad.medireminder.core.model.ScheduleType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DoseSchedulerTest {

    private val start = LocalDate(2026, 1, 5) // a Monday

    private fun medication(end: LocalDate? = null) = Medication(
        id = 1,
        name = "Metformin",
        startDate = start,
        endDate = end
    )

    private fun schedule(
        type: ScheduleType,
        times: List<LocalTime> = listOf(LocalTime(8, 0), LocalTime(20, 0)),
        days: Set<Int> = emptySet(),
        monthDays: Set<Int> = emptySet(),
        interval: Int? = null
    ) = Schedule(
        id = 1,
        medicationId = 1,
        scheduleType = type,
        timesOfDay = times,
        daysOfWeek = days,
        daysOfMonth = monthDays,
        intervalDays = interval
    )

    @Test
    fun dailyScheduleProducesEveryConfiguredTime() {
        val times = DoseScheduler.timesOn(medication(), schedule(ScheduleType.DAILY), start)
        assertEquals(
            listOf(
                LocalDateTime(2026, 1, 5, 8, 0),
                LocalDateTime(2026, 1, 5, 20, 0)
            ),
            times
        )
    }

    @Test
    fun dosesDoNotStartBeforeTheStartDate() {
        assertFalse(DoseScheduler.occursOn(medication(), schedule(ScheduleType.DAILY), LocalDate(2026, 1, 4)))
    }

    @Test
    fun dosesStopAfterTheEndDate() {
        val med = medication(end = LocalDate(2026, 1, 6))
        assertTrue(DoseScheduler.occursOn(med, schedule(ScheduleType.DAILY), LocalDate(2026, 1, 6)))
        assertFalse(DoseScheduler.occursOn(med, schedule(ScheduleType.DAILY), LocalDate(2026, 1, 7)))
    }

    @Test
    fun specificDaysOnlyFireOnListedWeekdays() {
        val monWedFri = schedule(ScheduleType.SPECIFIC_DAYS, days = setOf(1, 3, 5))
        assertTrue(DoseScheduler.occursOn(medication(), monWedFri, LocalDate(2026, 1, 5)))  // Mon
        assertFalse(DoseScheduler.occursOn(medication(), monWedFri, LocalDate(2026, 1, 6))) // Tue
        assertTrue(DoseScheduler.occursOn(medication(), monWedFri, LocalDate(2026, 1, 7)))  // Wed
    }

    @Test
    fun intervalSchedulesCountFromTheStartDate() {
        val everyThirdDay = schedule(ScheduleType.INTERVAL_DAYS, interval = 3)
        assertTrue(DoseScheduler.occursOn(medication(), everyThirdDay, start))
        assertFalse(DoseScheduler.occursOn(medication(), everyThirdDay, LocalDate(2026, 1, 6)))
        assertTrue(DoseScheduler.occursOn(medication(), everyThirdDay, LocalDate(2026, 1, 8)))
    }

    @Test
    fun monthlySchedulesFireOnListedCalendarDays() {
        val firstAndFifteenth = schedule(ScheduleType.MONTHLY_DAYS, monthDays = setOf(1, 15))
        assertTrue(DoseScheduler.occursOn(medication(), firstAndFifteenth, LocalDate(2026, 1, 15)))
        assertTrue(DoseScheduler.occursOn(medication(), firstAndFifteenth, LocalDate(2026, 2, 1)))
        assertFalse(DoseScheduler.occursOn(medication(), firstAndFifteenth, LocalDate(2026, 1, 14)))
    }

    @Test
    fun asNeededMedicationsNeverGenerateDoses() {
        assertTrue(DoseScheduler.timesOn(medication(), schedule(ScheduleType.AS_NEEDED), start).isEmpty())
    }

    @Test
    fun nextOccurrenceSkipsPastTimesOnTheCurrentDay() {
        val next = DoseScheduler.nextOccurrence(
            medication(),
            schedule(ScheduleType.DAILY),
            from = LocalDateTime(2026, 1, 5, 9, 0)
        )
        assertEquals(LocalDateTime(2026, 1, 5, 20, 0), next)
    }

    @Test
    fun nextOccurrenceRollsOverToTheFollowingDay() {
        val next = DoseScheduler.nextOccurrence(
            medication(),
            schedule(ScheduleType.DAILY),
            from = LocalDateTime(2026, 1, 5, 21, 0)
        )
        assertEquals(LocalDateTime(2026, 1, 6, 8, 0), next)
    }
}
