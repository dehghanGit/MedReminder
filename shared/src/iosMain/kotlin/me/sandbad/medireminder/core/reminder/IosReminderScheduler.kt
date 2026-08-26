package me.sandbad.medireminder.core.reminder

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import me.sandbad.medireminder.core.model.AppSettings
import me.sandbad.medireminder.core.model.DoseWithMedication
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

/** Local notifications via UNUserNotificationCenter, one request per pending dose. */
@OptIn(ExperimentalForeignApi::class)
class IosReminderScheduler : ReminderScheduler {

    private val center = UNUserNotificationCenter.currentNotificationCenter()

    override suspend fun scheduleAll(doses: List<DoseWithMedication>, settings: AppSettings) {
        cancelAll()
        if (!settings.remindersEnabled) return

        // iOS allows at most 64 pending local notifications per app.
        doses.sortedBy { it.dose.scheduledAt }.take(MAX_PENDING).forEach { item ->
            val content = UNMutableNotificationContent().apply {
                setTitle(ReminderCopy.title(item))
                setBody(ReminderCopy.body(item))
                if (settings.soundEnabled) setSound(UNNotificationSound.defaultSound())
            }
            val at = item.dose.scheduledAt
            val components = NSDateComponents().apply {
                year = at.year.toLong()
                month = at.monthNumber.toLong()
                day = at.dayOfMonth.toLong()
                hour = at.hour.toLong()
                minute = at.minute.toLong()
            }
            val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, false)
            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = identifier(item.dose.id),
                content = content,
                trigger = trigger
            )
            center.addNotificationRequest(request) { }
        }
    }

    override suspend fun cancel(doseId: Long) {
        center.removePendingNotificationRequestsWithIdentifiers(listOf(identifier(doseId)))
        center.removeDeliveredNotificationsWithIdentifiers(listOf(identifier(doseId)))
    }

    override suspend fun cancelAll() {
        center.removeAllPendingNotificationRequests()
    }

    override suspend fun hasPermission(): Boolean = suspendCancellableCoroutine { cont ->
        center.getNotificationSettingsWithCompletionHandler { settings ->
            cont.resume(settings?.authorizationStatus == UNAuthorizationStatusAuthorized)
        }
    }

    override suspend fun requestPermission() {
        val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        center.requestAuthorizationWithOptions(options) { _, _ -> }
    }

    private fun identifier(doseId: Long) = "dose-$doseId"

    private companion object {
        const val MAX_PENDING = 60
    }
}
