package me.sandbad.medireminder.di

import me.sandbad.medireminder.core.database.createDatabase
import me.sandbad.medireminder.core.repository.AppSettingsRepository
import me.sandbad.medireminder.core.repository.DoseLogRepository
import me.sandbad.medireminder.core.repository.MedicationRepository
import me.sandbad.medireminder.core.repository.ScheduleRepository
import me.sandbad.medireminder.core.repository.impl.AppSettingsRepositoryImpl
import me.sandbad.medireminder.core.repository.impl.DoseLogRepositoryImpl
import me.sandbad.medireminder.core.repository.impl.MedicationRepositoryImpl
import me.sandbad.medireminder.core.repository.impl.ScheduleRepositoryImpl
import me.sandbad.medireminder.core.service.AdherenceService
import me.sandbad.medireminder.core.service.DemoDataSeeder
import me.sandbad.medireminder.core.service.MedicationService
import me.sandbad.medireminder.ui.viewmodel.HistoryViewModel
import me.sandbad.medireminder.ui.viewmodel.MedicationEditViewModel
import me.sandbad.medireminder.ui.viewmodel.MedicationsViewModel
import me.sandbad.medireminder.ui.viewmodel.OnboardingViewModel
import me.sandbad.medireminder.ui.viewmodel.SettingsViewModel
import me.sandbad.medireminder.ui.viewmodel.TodayViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Shared graph. Each platform additionally provides a `DatabaseDriverFactory`
 * and a `ReminderScheduler` binding — see the platform module in each app target.
 */
val appModule = module {
    single { createDatabase(get()) }

    single<MedicationRepository> { MedicationRepositoryImpl(get()) }
    single<ScheduleRepository> { ScheduleRepositoryImpl(get()) }
    single<DoseLogRepository> { DoseLogRepositoryImpl(get()) }
    single<AppSettingsRepository> { AppSettingsRepositoryImpl(get()) }

    single { MedicationService(get(), get(), get(), get(), get()) }
    single { AdherenceService(get(), get()) }
    single { DemoDataSeeder(get(), get(), get(), get()) }

    viewModel { OnboardingViewModel(get(), get()) }
    viewModel { TodayViewModel(get(), get(), get()) }
    viewModel { MedicationsViewModel(get(), get(), get()) }
    viewModel { MedicationEditViewModel(get(), get(), get()) }
    viewModel { HistoryViewModel(get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get()) }
}
