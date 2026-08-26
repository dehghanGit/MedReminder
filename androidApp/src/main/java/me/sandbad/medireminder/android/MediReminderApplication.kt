package me.sandbad.medireminder.android

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.sandbad.medireminder.core.database.DatabaseDriverFactory
import me.sandbad.medireminder.core.reminder.AndroidReminderScheduler
import me.sandbad.medireminder.core.reminder.ReminderScheduler
import me.sandbad.medireminder.core.service.MedicationService
import me.sandbad.medireminder.di.appModule
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class MediReminderApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MediReminderApplication)
            modules(
                module {
                    single { DatabaseDriverFactory(androidContext()) }
                    single<ReminderScheduler> { AndroidReminderScheduler(androidContext()) }
                },
                appModule
            )
        }

        // Re-arm alarms lost to a reboot or an app update, and close out missed doses.
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            get<MedicationService>().syncUpcoming()
        }
    }
}
