package me.sandbad.medireminder.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import me.sandbad.medireminder.core.model.AdherenceStats
import me.sandbad.medireminder.core.model.DoseWithMedication
import me.sandbad.medireminder.core.service.AdherenceService
import me.sandbad.medireminder.core.service.MedicationService
import me.sandbad.medireminder.core.service.plusDays
import me.sandbad.medireminder.core.today

enum class HistoryRange(val label: String, val days: Int) {
    WEEK("7 days", 7),
    MONTH("30 days", 30),
    QUARTER("90 days", 90)
}

data class HistoryState(
    val isLoading: Boolean = true,
    val range: HistoryRange = HistoryRange.WEEK,
    val stats: AdherenceStats = AdherenceStats(),
    val dailyRates: List<Pair<LocalDate, Float>> = emptyList(),
    val streakDays: Int = 0,
    val entriesByDate: Map<LocalDate, List<DoseWithMedication>> = emptyMap()
)

class HistoryViewModel(
    private val adherence: AdherenceService,
    private val service: MedicationService
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    init {
        load()
    }

    fun setRange(range: HistoryRange) {
        _state.update { it.copy(range = range) }
        load()
    }

    fun load() = viewModelScope.launch {
        val range = _state.value.range
        _state.update { it.copy(isLoading = true) }
        val end = today()
        val start = end.plusDays(-(range.days - 1))
        // Newest day first — the History list reads top-down from today.
        val entries = linkedMapOf<LocalDate, List<DoseWithMedication>>()
        for (offset in (range.days - 1) downTo 0) {
            val date = start.plusDays(offset)
            val doses = service.dosesFor(date)
            if (doses.isNotEmpty()) entries[date] = doses
        }
        _state.update {
            it.copy(
                isLoading = false,
                stats = adherence.statsBetween(start, end),
                dailyRates = adherence.dailyRates(range.days),
                streakDays = adherence.currentStreak(),
                entriesByDate = entries
            )
        }
    }
}
