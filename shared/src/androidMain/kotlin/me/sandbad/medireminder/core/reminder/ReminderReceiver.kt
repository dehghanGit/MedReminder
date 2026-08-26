package me.sandbad.medireminder.core.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.sandbad.medireminder.core.service.MedicationService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Posts the "dose due" notification and handles its Take / Snooze actions.
 * Registered in the app manifest.
 */
class ReminderReceiver : BroadcastReceiver(), KoinComponent {

    private val service: MedicationService by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        val doseId = intent.getLongExtra(EXTRA_DOSE_ID, -1L)
        when (intent.action) {
            ACTION_REMIND -> {
                if (doseId < 0) return
                notify(
                    context,
                    doseId,
                    intent.getStringExtra(EXTRA_TITLE) ?: "Medication reminder",
                    intent.getStringExtra(EXTRA_BODY) ?: "A dose is due"
                )
            }

            ACTION_TAKE -> {
                if (doseId < 0) return
                context.getSystemService<NotificationManager>()?.cancel(doseId.toInt())
                val result = goAsync()
                scope.launch {
                    try {
                        service.markTaken(doseId)
                    } finally {
                        result.finish()
                    }
                }
            }

            ACTION_SNOOZE -> {
                if (doseId < 0) return
                context.getSystemService<NotificationManager>()?.cancel(doseId.toInt())
                val result = goAsync()
                scope.launch {
                    try {
                        service.snooze(doseId)
                    } finally {
                        result.finish()
                    }
                }
            }

            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val result = goAsync()
                scope.launch {
                    try {
                        service.syncUpcoming()
                    } finally {
                        result.finish()
                    }
                }
            }
        }
    }

    private fun notify(context: Context, doseId: Long, title: String, body: String) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = launch?.let {
            PendingIntent.getActivity(
                context, doseId.toInt(), it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(0, "Take", actionIntent(context, doseId, ACTION_TAKE))
            .addAction(0, "Snooze", actionIntent(context, doseId, ACTION_SNOOZE))
            .build()

        runCatching { manager.notify(doseId.toInt(), notification) }
    }

    private fun actionIntent(context: Context, doseId: Long, action: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_DOSE_ID, doseId)
        }
        return PendingIntent.getBroadcast(
            context,
            (doseId.toInt() * 31) + action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
