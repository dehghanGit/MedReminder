package me.sandbad.medireminder.core.service

import kotlinx.datetime.LocalDate
import me.sandbad.medireminder.core.model.AdherenceStats
import me.sandbad.medireminder.core.model.DoseStatus
import me.sandbad.medireminder.core.repository.DoseLogRepository
import me.sandbad.medireminder.core.repository.MedicationRepository
import me.sandbad.medireminder.core.today

class AdherenceService(
    private val doses: DoseLogRepository,
    private val medications: MedicationRepository
) {

    suspend fun statsBetween(from: LocalDate, to: LocalDate): AdherenceStats =
        doses.getBetween(from, to).toStats()

    suspend fun statsForMedication(medicationId: Long, from: LocalDate, to: LocalDate): AdherenceStats =
        doses.getForMedicationBetween(medicationId, from, to).toStats()

    /** Per-day adherence rate for the last [days] days, oldest first — feeds the history chart. */
    suspend fun dailyRates(days: Int): List<Pair<LocalDate, Float>> {
        val end = today()
        val start = end.plusDays(-(days - 1))
        val byDate = doses.getBetween(start, end).groupBy { it.date }
        return (0 until days).map { offset ->
            val date = start.plusDays(offset)
            date to byDate[date].orEmpty().toStats().rate
        }
    }

    /** Consecutive days, ending yesterday-or-today, where every scored dose was taken. */
    suspend fun currentStreak(maxLookBack: Int = 365): Int {
        val end = today()
        val start = end.plusDays(-maxLookBack)
        val byDate = doses.getBetween(start, end).groupBy { it.date }
        var streak = 0
        var date = end
        while (date >= start) {
            val stats = byDate[date].orEmpty().toStats()
            when {
                stats.scored == 0 && date == end -> Unit // today may simply not be due yet
                stats.scored == 0 -> return streak
                stats.taken == stats.scored -> streak++
                else -> return streak
            }
            date = date.plusDays(-1)
        }
        return streak
    }

    suspend fun medicationNames(): Map<Long, String> =
        medications.getAllActive().associate { it.id to it.displayName }
}

private fun List<me.sandbad.medireminder.core.model.DoseLog>.toStats() = AdherenceStats(
    taken = count { it.status == DoseStatus.TAKEN },
    skipped = count { it.status == DoseStatus.SKIPPED },
    missed = count { it.status == DoseStatus.MISSED },
    pending = count { it.status == DoseStatus.PENDING }
)
