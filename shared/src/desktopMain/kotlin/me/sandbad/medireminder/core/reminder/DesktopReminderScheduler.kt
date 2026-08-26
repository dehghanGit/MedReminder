package me.sandbad.medireminder.core.reminder

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.sandbad.medireminder.core.currentDateTime
import me.sandbad.medireminder.core.epochMillis
import me.sandbad.medireminder.core.model.AppSettings
import me.sandbad.medireminder.core.model.DoseWithMedication
import java.awt.SystemTray
import java.awt.TrayIcon

/**
 * Desktop has no alarm service, so reminders live in-process: one coroutine per dose
 * that sleeps until its time and then raises a tray notification. Anything scheduled
 * while the app is closed is caught by the missed-dose sweep on next launch.
 */
class DesktopReminderScheduler : ReminderScheduler {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val jobs = mutableMapOf<Long, Job>()

    private val trayIcon: TrayIcon? by lazy {
        if (!SystemTray.isSupported()) return@lazy null
        runCatching {
            val image = java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB)
            TrayIcon(image, "MediReminder").also { icon ->
                icon.isImageAutoSize = true
                SystemTray.getSystemTray().add(icon)
            }
        }.getOrNull()
    }

    override suspend fun scheduleAll(doses: List<DoseWithMedication>, settings: AppSettings) {
        cancelAll()
        if (!settings.remindersEnabled) return
        doses.forEach { item ->
            val waitMs = item.dose.scheduledAt.epochMillis() - currentDateTime().epochMillis()
            if (waitMs < 0) return@forEach
            jobs[item.dose.id] = scope.launch {
                delay(waitMs)
                trayIcon?.displayMessage(
                    ReminderCopy.title(item),
                    ReminderCopy.body(item),
                    TrayIcon.MessageType.INFO
                )
            }
        }
    }

    override suspend fun cancel(doseId: Long) {
        jobs.remove(doseId)?.cancel()
    }

    override suspend fun cancelAll() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
    }

    override suspend fun hasPermission(): Boolean = true

    override suspend fun requestPermission() = Unit
}
