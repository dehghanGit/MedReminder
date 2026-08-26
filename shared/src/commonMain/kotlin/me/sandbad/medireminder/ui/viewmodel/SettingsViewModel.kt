package me.sandbad.medireminder.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.sandbad.medireminder.core.model.AppSettings
import me.sandbad.medireminder.core.reminder.ReminderScheduler
import me.sandbad.medireminder.core.repository.AppSettingsRepository
import me.sandbad.medireminder.core.service.MedicationService

data class SettingsState(
    val isLoading: Boolean = true,
    val settings: AppSettings = AppSettings(),
    val hasNotificationPermission: Boolean = true
)

class SettingsViewModel(
    private val repository: AppSettingsRepository,
    private val service: MedicationService,
    private val reminders: ReminderScheduler
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = SettingsState(
                isLoading = false,
                settings = repository.get(),
                hasNotificationPermission = reminders.hasPermission()
            )
        }
    }

    fun update(transform: (AppSettings) -> AppSettings) = viewModelScope.launch {
        val updated = transform(_state.value.settings)
        _state.update { it.copy(settings = updated) }
        repository.save(updated)
        service.syncUpcoming()
    }

    fun requestNotificationPermission() = viewModelScope.launch {
        reminders.requestPermission()
        _state.update { it.copy(hasNotificationPermission = reminders.hasPermission()) }
    }
}
