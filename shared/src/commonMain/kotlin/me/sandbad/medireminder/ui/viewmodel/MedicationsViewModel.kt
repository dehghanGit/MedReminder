package me.sandbad.medireminder.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.sandbad.medireminder.core.model.Medication
import me.sandbad.medireminder.core.model.Schedule
import me.sandbad.medireminder.core.repository.MedicationRepository
import me.sandbad.medireminder.core.repository.ScheduleRepository
import me.sandbad.medireminder.core.service.MedicationService

data class MedicationsState(
    val isLoading: Boolean = true,
    val showArchived: Boolean = false,
    val medications: List<Medication> = emptyList(),
    val schedulesByMedication: Map<Long, List<Schedule>> = emptyMap()
)

class MedicationsViewModel(
    private val medications: MedicationRepository,
    private val schedules: ScheduleRepository,
    private val service: MedicationService
) : ViewModel() {

    private val _state = MutableStateFlow(MedicationsState())
    val state: StateFlow<MedicationsState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true) }
        val all = if (_state.value.showArchived) medications.getAll() else medications.getAllActive()
        val bySchedule = all.associate { it.id to schedules.getForMedication(it.id) }
        _state.update { it.copy(isLoading = false, medications = all, schedulesByMedication = bySchedule) }
    }

    fun toggleArchivedVisible() {
        _state.update { it.copy(showArchived = !it.showArchived) }
        load()
    }

    fun archive(id: Long) = viewModelScope.launch {
        service.archiveMedication(id)
        load()
    }

    fun unarchive(id: Long) = viewModelScope.launch {
        medications.setArchived(id, false)
        service.syncUpcoming()
        load()
    }

    fun delete(id: Long) = viewModelScope.launch {
        service.deleteMedication(id)
        load()
    }
}
