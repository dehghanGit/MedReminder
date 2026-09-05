package me.sandbad.medireminder.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import me.sandbad.medireminder.core.model.MedColor
import me.sandbad.medireminder.core.model.Medication
import me.sandbad.medireminder.core.model.MedicationForm
import me.sandbad.medireminder.core.model.Schedule
import me.sandbad.medireminder.core.model.ScheduleType
import me.sandbad.medireminder.core.model.StrengthUnit
import me.sandbad.medireminder.core.repository.MedicationRepository
import me.sandbad.medireminder.core.repository.ScheduleRepository
import me.sandbad.medireminder.core.service.MedicationService
import me.sandbad.medireminder.core.today

data class MedicationEditState(
    val id: Long = 0,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val name: String = "",
    val form: MedicationForm = MedicationForm.TABLET,
    val strength: String = "",
    val strengthUnit: StrengthUnit = StrengthUnit.MG,
    val color: MedColor = MedColor.BLUE,
    val instructions: String = "",
    val notes: String = "",
    val stockCount: String = "",
    val refillAt: String = "",
    val startDate: LocalDate = today(),
    val endDate: LocalDate? = null,
    val scheduleType: ScheduleType = ScheduleType.DAILY,
    val times: List<LocalTime> = listOf(LocalTime(8, 0)),
    val quantity: String = "1",
    val daysOfWeek: Set<Int> = setOf(today().dayOfWeek.isoDayNumber),
    val daysOfMonth: Set<Int> = setOf(today().dayOfMonth),
    val intervalDays: String = "2",
    val scheduleId: Long = 0,
    val nameError: String? = null,
    val saved: Boolean = false
) {
    val isEditing: Boolean get() = id != 0L
    val needsTimes: Boolean get() = scheduleType != ScheduleType.AS_NEEDED

    /** The whole-days repeat interval a DAILY/INTERVAL_DAYS schedule uses (1 = every day). */
    val dailyInterval: Int
        get() = if (scheduleType == ScheduleType.INTERVAL_DAYS) intervalDays.toIntOrNull()?.coerceAtLeast(2) ?: 2 else 1
}

/** Backs both "add medication" and "edit medication" — a single schedule per medication. */
class MedicationEditViewModel(
    private val medications: MedicationRepository,
    private val schedules: ScheduleRepository,
    private val service: MedicationService
) : ViewModel() {

    private val _state = MutableStateFlow(MedicationEditState())
    val state: StateFlow<MedicationEditState> = _state.asStateFlow()

    fun load(medicationId: Long) {
        if (medicationId == 0L) {
            _state.value = MedicationEditState()
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val med = medications.getById(medicationId)
            if (med == null) {
                _state.update { it.copy(isLoading = false) }
                return@launch
            }
            val schedule = schedules.getForMedication(medicationId).firstOrNull()
            _state.value = MedicationEditState(
                id = med.id,
                name = med.name,
                form = med.form,
                strength = med.strength?.toString().orEmpty(),
                strengthUnit = med.strengthUnit,
                color = med.color,
                instructions = med.instructions.orEmpty(),
                notes = med.notes.orEmpty(),
                stockCount = med.stockCount?.toString().orEmpty(),
                refillAt = med.refillAt?.toString().orEmpty(),
                startDate = med.startDate,
                endDate = med.endDate,
                scheduleType = schedule?.scheduleType ?: ScheduleType.DAILY,
                times = schedule?.timesOfDay?.takeIf { it.isNotEmpty() } ?: listOf(LocalTime(9, 0)),
                quantity = schedule?.quantity?.toString() ?: "1",
                daysOfWeek = schedule?.daysOfWeek?.takeIf { it.isNotEmpty() } ?: setOf(med.startDate.dayOfWeek.isoDayNumber),
                daysOfMonth = schedule?.daysOfMonth?.takeIf { it.isNotEmpty() } ?: setOf(med.startDate.dayOfMonth),
                intervalDays = schedule?.intervalDays?.toString() ?: "2",
                scheduleId = schedule?.id ?: 0L
            )
        }
    }

    fun setName(value: String) = _state.update { it.copy(name = value, nameError = null) }
    fun setForm(value: MedicationForm) = _state.update { it.copy(form = value) }
    fun setStrength(value: String) = _state.update { it.copy(strength = value) }
    fun setStrengthUnit(value: StrengthUnit) = _state.update { it.copy(strengthUnit = value) }
    fun setColor(value: MedColor) = _state.update { it.copy(color = value) }
    fun setInstructions(value: String) = _state.update { it.copy(instructions = value) }
    fun setNotes(value: String) = _state.update { it.copy(notes = value) }
    fun setStockCount(value: String) = _state.update { it.copy(stockCount = value) }
    fun setRefillAt(value: String) = _state.update { it.copy(refillAt = value) }
    fun setStartDate(value: LocalDate) = _state.update { it.copy(startDate = value) }
    fun setEndDate(value: LocalDate?) = _state.update { it.copy(endDate = value) }
    fun setScheduleType(value: ScheduleType) = _state.update { it.copy(scheduleType = value) }
    fun setQuantity(value: String) = _state.update { it.copy(quantity = value) }
    fun setIntervalDays(value: String) = _state.update { it.copy(intervalDays = value) }

    /** Switch to the "Daily" family, preserving any custom repeat interval. */
    fun selectDaily() = _state.update {
        it.copy(scheduleType = if ((it.intervalDays.toIntOrNull() ?: 1) > 1) ScheduleType.INTERVAL_DAYS else ScheduleType.DAILY)
    }

    fun selectWeekly() = _state.update {
        it.copy(
            scheduleType = ScheduleType.SPECIFIC_DAYS,
            daysOfWeek = it.daysOfWeek.ifEmpty { setOf(it.startDate.dayOfWeek.isoDayNumber) }
        )
    }

    fun selectMonthly() = _state.update {
        it.copy(
            scheduleType = ScheduleType.MONTHLY_DAYS,
            daysOfMonth = it.daysOfMonth.ifEmpty { setOf(it.startDate.dayOfMonth) }
        )
    }

    /** Repeat every [days] days (1 = every day, 2 = every other day, …). */
    fun setDailyInterval(days: Int) = _state.update {
        val n = days.coerceAtLeast(1)
        it.copy(
            scheduleType = if (n <= 1) ScheduleType.DAILY else ScheduleType.INTERVAL_DAYS,
            intervalDays = n.toString()
        )
    }

    fun toggleDay(isoDay: Int) = _state.update {
        val days = if (isoDay in it.daysOfWeek) it.daysOfWeek - isoDay else it.daysOfWeek + isoDay
        it.copy(daysOfWeek = days.ifEmpty { it.daysOfWeek }) // keep at least one day selected
    }

    fun toggleDayOfMonth(day: Int) = _state.update {
        val days = if (day in it.daysOfMonth) it.daysOfMonth - day else it.daysOfMonth + day
        it.copy(daysOfMonth = days.ifEmpty { it.daysOfMonth })
    }

    fun addTime(time: LocalTime) = _state.update {
        it.copy(times = (it.times + time).distinct().sorted())
    }

    fun removeTime(time: LocalTime) = _state.update {
        it.copy(times = it.times.filterNot { existing -> existing == time })
    }

    fun save() {
        val current = _state.value
        if (current.name.isBlank()) {
            _state.update { it.copy(nameError = "Name is required") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val medication = Medication(
                id = current.id,
                name = current.name.trim(),
                form = current.form,
                strength = current.strength.replace(',', '.').toDoubleOrNull(),
                strengthUnit = current.strengthUnit,
                color = current.color,
                instructions = current.instructions.trim().ifBlank { null },
                notes = current.notes.trim().ifBlank { null },
                stockCount = current.stockCount.replace(',', '.').toDoubleOrNull(),
                refillAt = current.refillAt.replace(',', '.').toDoubleOrNull(),
                startDate = current.startDate,
                endDate = current.endDate
            )
            val schedule = Schedule(
                id = current.scheduleId,
                medicationId = current.id,
                scheduleType = current.scheduleType,
                timesOfDay = if (current.needsTimes) current.times else emptyList(),
                quantity = current.quantity.replace(',', '.').toDoubleOrNull() ?: 1.0,
                daysOfWeek = if (current.scheduleType == ScheduleType.SPECIFIC_DAYS) current.daysOfWeek else emptySet(),
                daysOfMonth = if (current.scheduleType == ScheduleType.MONTHLY_DAYS) current.daysOfMonth else emptySet(),
                intervalDays = current.intervalDays.toIntOrNull()
                    ?.takeIf { current.scheduleType == ScheduleType.INTERVAL_DAYS }
            )
            service.saveMedication(medication, listOf(schedule))
            _state.update { it.copy(isSaving = false, saved = true) }
        }
    }

    fun consumeSaved() = _state.update { it.copy(saved = false) }
}
