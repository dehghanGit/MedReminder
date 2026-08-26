package me.sandbad.medireminder.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.sandbad.medireminder.core.reminder.ReminderScheduler
import me.sandbad.medireminder.core.repository.AppSettingsRepository

data class OnboardingState(
    val isLoading: Boolean = true,
    val needsOnboarding: Boolean = false,
    val ownerName: String = ""
)

class OnboardingViewModel(
    private val settings: AppSettingsRepository,
    private val reminders: ReminderScheduler
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val current = settings.get()
            _state.value = OnboardingState(
                isLoading = false,
                needsOnboarding = !current.onboardingDone,
                ownerName = current.ownerName.orEmpty()
            )
        }
    }

    fun setOwnerName(value: String) = _state.update { it.copy(ownerName = value) }

    fun complete() = viewModelScope.launch {
        reminders.requestPermission()
        val current = settings.get()
        settings.save(
            current.copy(
                ownerName = _state.value.ownerName.trim().ifBlank { null },
                onboardingDone = true
            )
        )
        _state.update { it.copy(needsOnboarding = false) }
    }
}
