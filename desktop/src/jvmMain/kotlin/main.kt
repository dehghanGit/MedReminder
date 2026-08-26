import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.sandbad.medireminder.core.database.DatabaseDriverFactory
import me.sandbad.medireminder.core.reminder.DesktopReminderScheduler
import me.sandbad.medireminder.core.reminder.ReminderScheduler
import me.sandbad.medireminder.core.service.MedicationService
import me.sandbad.medireminder.di.appModule
import me.sandbad.medireminder.ui.MediReminderApp
import me.sandbad.medireminder.ui.theme.MediReminderTheme
import org.koin.core.context.startKoin

fun main() = application {
    val koin = startKoin {
        modules(
            org.koin.dsl.module {
                single { DatabaseDriverFactory() }
                single<ReminderScheduler> { DesktopReminderScheduler() }
            },
            appModule
        )
    }.koin

    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
        koin.get<MedicationService>().syncUpcoming()
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "MediReminder",
        state = rememberWindowState(width = 420.dp, height = 860.dp)
    ) {
        MediReminderTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                MediReminderApp()
            }
        }
    }
}
