package me.sandbad.medireminder.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import me.sandbad.medireminder.core.currentDateTime
import me.sandbad.medireminder.core.epochMillis
import me.sandbad.medireminder.core.model.AdherenceStats
import me.sandbad.medireminder.core.model.DoseStatus
import me.sandbad.medireminder.core.model.DoseWithMedication
import me.sandbad.medireminder.core.model.Medication
import me.sandbad.medireminder.core.service.AdherenceService
import me.sandbad.medireminder.core.service.DemoDataSeeder
import me.sandbad.medireminder.core.service.MedicationService
import me.sandbad.medireminder.core.service.plusDays
import me.sandbad.medireminder.core.today
import kotlin.math.abs
import kotlin.math.roundToLong

data class TodayState(
    val isLoading: Boolean = true,
    val date: LocalDate = me.sandbad.medireminder.core.today(),
    val doses: List<DoseWithMedication> = emptyList(),
    val stats: AdherenceStats = AdherenceStats(),
    val streakDays: Int = 0,
    val lowStock: List<Medication> = emptyList(),
    /** Set when a mark-taken action landed far from the scheduled time and needs the user to confirm. */
    val offScheduleConfirm: OffScheduleConfirm? = null
) {
    val upcoming: List<DoseWithMedication> get() = doses.filter { it.dose.status == DoseStatus.PENDING }
    val done: List<DoseWithMedication> get() = doses.filter { it.dose.status != DoseStatus.PENDING }
    val isToday: Boolean get() = date == me.sandbad.medireminder.core.today()
}

/**
 * A pending confirmation raised when the user logs a dose more than
 * [TodayViewModel.OFF_SCHEDULE_THRESHOLD_MINUTES] away from its scheduled time.
 */
data class OffScheduleConfirm(
    val dose: DoseWithMedication,
    /** Signed minutes between now and the scheduled time: negative = early, positive = late. */
    val offsetMinutes: Long
) {
    val isLate: Boolean get() = offsetMinutes > 0
    val absMinutes: Long get() = abs(offsetMinutes)
}

class TodayViewModel(
    private val service: MedicationService,
    private val adherence: AdherenceService,
    private val demoData: DemoDataSeeder
) : ViewModel() {

    private val _state = MutableStateFlow(TodayState())
    val state: StateFlow<TodayState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            demoData.seedIfEmpty()
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

    /**
     * Entry point for the "take" action (button or swipe). If the log lands far enough from the
     * scheduled time we raise a confirmation instead of logging straight away; otherwise we log.
     */
    fun requestMarkTaken(doseId: Long) {
        val dose = _state.value.doses.firstOrNull { it.dose.id == doseId } ?: return
        val offset = minutesFromSchedule(dose)
        if (abs(offset) > OFF_SCHEDULE_THRESHOLD_MINUTES) {
            _state.update { it.copy(offScheduleConfirm = OffScheduleConfirm(dose, offset)) }
        } else {
            markTaken(doseId)
        }
    }

    /** User confirmed logging despite the off-schedule warning. */
    fun confirmOffScheduleTake() {
        val pending = _state.value.offScheduleConfirm ?: return
        _state.update { it.copy(offScheduleConfirm = null) }
        markTaken(pending.dose.dose.id)
    }

    /** User backed out of the off-schedule warning. */
    fun dismissOffScheduleConfirm() {
        _state.update { it.copy(offScheduleConfirm = null) }
    }

    private fun markTaken(doseId: Long) = viewModelScope.launch {
        service.markTaken(doseId)
        load(_state.value.date)
    }

    /** Signed whole minutes from the scheduled time to now: negative = early, positive = late. */
    private fun minutesFromSchedule(dose: DoseWithMedication): Long {
        val deltaMillis = currentDateTime().epochMillis() - dose.dose.scheduledAt.epochMillis()
        return (deltaMillis / 60_000.0).roundToLong()
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

    fun archive(medicationId: Long) = viewModelScope.launch {
        service.archiveMedication(medicationId)
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

    companion object {
        /** Beyond this gap between the log action and the scheduled time we ask the user to confirm. */
        const val OFF_SCHEDULE_THRESHOLD_MINUTES = 30L
    }
}
