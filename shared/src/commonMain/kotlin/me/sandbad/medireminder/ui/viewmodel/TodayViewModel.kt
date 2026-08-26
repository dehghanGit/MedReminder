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
import me.sandbad.medireminder.core.model.DoseStatus
import me.sandbad.medireminder.core.model.DoseWithMedication
import me.sandbad.medireminder.core.model.Medication
import me.sandbad.medireminder.core.service.AdherenceService
import me.sandbad.medireminder.core.service.MedicationService
import me.sandbad.medireminder.core.service.plusDays
import me.sandbad.medireminder.core.today

data class TodayState(
    val isLoading: Boolean = true,
    val date: LocalDate = me.sandbad.medireminder.core.today(),
    val doses: List<DoseWithMedication> = emptyList(),
    val stats: AdherenceStats = AdherenceStats(),
    val streakDays: Int = 0,
    val lowStock: List<Medication> = emptyList()
) {
    val upcoming: List<DoseWithMedication> get() = doses.filter { it.dose.status == DoseStatus.PENDING }
    val done: List<DoseWithMedication> get() = doses.filter { it.dose.status != DoseStatus.PENDING }
    val isToday: Boolean get() = date == me.sandbad.medireminder.core.today()
}

class TodayViewModel(
    private val service: MedicationService,
    private val adherence: AdherenceService
) : ViewModel() {

    private val _state = MutableStateFlow(TodayState())
    val state: StateFlow<TodayState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            service.syncUpcoming()
            load(today())
        }
    }

    fun refresh() = viewModelScope.launch {
        service.syncUpcoming()
        load(_state.value.date)
    }

    fun showDate(date: LocalDate) = viewModelScope.launch { load(date) }

    fun showPreviousDay() = showDate(_state.value.date.plusDays(-1))

    fun showNextDay() = showDate(_state.value.date.plusDays(1))

    fun markTaken(doseId: Long) = viewModelScope.launch {
        service.markTaken(doseId)
        load(_state.value.date)
    }

    fun markSkipped(doseId: Long) = viewModelScope.launch {
        service.markSkipped(doseId)
        load(_state.value.date)
    }

    fun snooze(doseId: Long) = viewModelScope.launch {
        service.snooze(doseId)
        load(_state.value.date)
    }

    fun undo(doseId: Long) = viewModelScope.launch {
        service.undo(doseId)
        load(_state.value.date)
    }

    private suspend fun load(date: LocalDate) {
        _state.update { it.copy(isLoading = true, date = date) }
        val doses = service.dosesFor(date)
        _state.update {
            it.copy(
                isLoading = false,
                doses = doses,
                stats = adherence.statsBetween(date, date),
                streakDays = adherence.currentStreak(),
                lowStock = service.lowStockMedications()
            )
        }
    }
}
