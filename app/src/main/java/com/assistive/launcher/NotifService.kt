package com.assistive.launcher

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Packages that currently have a (non-ongoing) notification. Drives the blue "New" pill. */
object Badges {
    var packages by mutableStateOf(setOf<String>())
}

class NotifService : NotificationListenerService() {
    override fun onListenerConnected() = refresh()
    override fun onListenerDisconnected() { Badges.packages = emptySet() }
    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    private fun refresh() {
        try {
            Badges.packages = (activeNotifications ?: emptyArray())
                .filter { !it.isOngoing }
                .map { it.packageName }
                .toSet()
        } catch (_: Exception) {
        }
    }
}
