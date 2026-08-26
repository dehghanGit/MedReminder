package me.sandbad.medireminder.ui

import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.sandbad.medireminder.core.database.DatabaseDriverFactory
import me.sandbad.medireminder.core.reminder.IosReminderScheduler
import me.sandbad.medireminder.core.reminder.ReminderScheduler
import me.sandbad.medireminder.core.service.MedicationService
import me.sandbad.medireminder.di.appModule
import me.sandbad.medireminder.ui.theme.MediReminderTheme
import org.koin.core.context.startKoin
import org.koin.dsl.module
import platform.UIKit.UIViewController

private var started = false

/** Entry point called from `iosApp` — starts Koin once, then hands SwiftUI the Compose view. */
fun MainViewController(): UIViewController {
    if (!started) {
        started = true
        val koin = startKoin {
            modules(
                module {
                    single { DatabaseDriverFactory() }
                    single<ReminderScheduler> { IosReminderScheduler() }
                },
                appModule
            )
        }.koin
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            koin.get<MedicationService>().syncUpcoming()
        }
    }
    return ComposeUIViewController {
        MediReminderTheme {
            MediReminderApp()
        }
    }
}
