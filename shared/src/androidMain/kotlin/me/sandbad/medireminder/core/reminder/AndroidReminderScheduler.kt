package me.sandbad.medireminder.core.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import me.sandbad.medireminder.core.epochMillis
import me.sandbad.medireminder.core.model.AppSettings
import me.sandbad.medireminder.core.model.DoseWithMedication

/**
 * Exact alarms, one per pending dose. Android caps how many alarms an app may hold,
 * so only the nearest [MAX_ALARMS] doses are armed; [MedicationService.syncUpcoming]
 * re-arms the rest whenever it runs.
 */
class AndroidReminderScheduler(private val context: Context) : ReminderScheduler {

    private val alarmManager: AlarmManager? = context.getSystemService()

    /** Ids currently armed, so [cancelAll] knows what to tear down. */
    private var armedIds: List<Long> = emptyList()

    override suspend fun scheduleAll(doses: List<DoseWithMedication>, settings: AppSettings) {
        ensureChannel(settings)
        cancelAll()
        if (!settings.remindersEnabled) return

        val armed = doses.sortedBy { it.dose.scheduledAt }.take(MAX_ALARMS)
        armedIds = armed.map { it.dose.id }
        armed.forEach { item ->
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_REMIND
                putExtra(EXTRA_DOSE_ID, item.dose.id)
                putExtra(EXTRA_TITLE, ReminderCopy.title(item))
                putExtra(EXTRA_BODY, ReminderCopy.body(item))
            }
            val pending = PendingIntent.getBroadcast(
                context,
                item.dose.id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val at = item.dose.scheduledAt.epochMillis()
            val manager = alarmManager ?: return@forEach
            if (canScheduleExact(manager)) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
            } else {
                manager.setWindow(AlarmManager.RTC_WAKEUP, at, INEXACT_WINDOW_MS, pending)
            }
        }
    }

    override suspend fun cancel(doseId: Long) {
        val intent = Intent(context, ReminderReceiver::class.java).apply { action = ACTION_REMIND }
        val pending = PendingIntent.getBroadcast(
            context,
            doseId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pending?.let {
            alarmManager?.cancel(it)
            it.cancel()
        }
        context.getSystemService<NotificationManager>()?.cancel(doseId.toInt())
        armedIds = armedIds - doseId
    }

    override suspend fun cancelAll() {
        armedIds.forEach { cancel(it) }
        armedIds = emptyList()
    }

    override suspend fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                "android.permission.POST_NOTIFICATIONS"
            ) == PackageManager.PERMISSION_GRANTED

    /**
     * The runtime prompt has to come from an Activity, so this only opens the
     * settings page as a fallback; [MainActivity] asks for the permission directly.
     */
    override suspend fun requestPermission() {
        val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private fun canScheduleExact(manager: AlarmManager): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()

    private fun ensureChannel(settings: AppSettings) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService<NotificationManager>() ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Medication reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when a dose is due"
            enableVibration(settings.vibrationEnabled)
            if (!settings.soundEnabled) setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    private companion object {
        const val MAX_ALARMS = 100
        const val INEXACT_WINDOW_MS = 5 * 60 * 1000L
    }
}

internal const val CHANNEL_ID = "medireminder.doses"
internal const val ACTION_REMIND = "me.sandbad.medireminder.REMIND"
internal const val ACTION_TAKE = "me.sandbad.medireminder.TAKE"
internal const val ACTION_SNOOZE = "me.sandbad.medireminder.SNOOZE"
internal const val EXTRA_DOSE_ID = "doseId"
internal const val EXTRA_TITLE = "title"
internal const val EXTRA_BODY = "body"
