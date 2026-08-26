package me.sandbad.medireminder.core.reminder

import me.sandbad.medireminder.core.model.AppSettings
import me.sandbad.medireminder.core.model.DoseWithMedication

/**
 * Hands pending doses to whatever the platform uses to wake the user
 * (AlarmManager on Android, UNUserNotificationCenter on iOS, a timer on desktop).
 *
 * Implementations must be idempotent: [scheduleAll] is called with the *complete*
 * set of upcoming doses every time the schedule changes, and should replace, not
 * append to, anything already pending.
 */
interface ReminderScheduler {
    suspend fun scheduleAll(doses: List<DoseWithMedication>, settings: AppSettings)
    suspend fun cancel(doseId: Long)
    suspend fun cancelAll()
    /** Whether the OS has granted permission to post reminders. */
    suspend fun hasPermission(): Boolean
    suspend fun requestPermission()
}

/** Copy shown in the notification for a due dose. */
object ReminderCopy {
    fun title(dose: DoseWithMedication): String = "Time for ${dose.medication.name}"

    fun body(dose: DoseWithMedication): String {
        val amount = dose.medication.form.unitLabel
        val qty = dose.dose.quantity
        val count = if (qty % 1.0 == 0.0) qty.toLong().toString() else qty.toString()
        val plural = if (qty == 1.0) amount else "${amount}s"
        val instructions = dose.medication.instructions?.takeIf { it.isNotBlank() }
        return buildString {
            append("Take $count $plural")
            dose.medication.strength?.let { append(" · ${dose.medication.displayName}") }
            instructions?.let { append(" · $it") }
        }
    }
}

/** Fallback used on platforms where reminders are not wired up yet. */
class NoopReminderScheduler : ReminderScheduler {
    override suspend fun scheduleAll(doses: List<DoseWithMedication>, settings: AppSettings) = Unit
    override suspend fun cancel(doseId: Long) = Unit
    override suspend fun cancelAll() = Unit
    override suspend fun hasPermission(): Boolean = true
    override suspend fun requestPermission() = Unit
}
