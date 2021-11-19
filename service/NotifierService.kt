package com.vaticle.typedb.studio.service

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

class NotifierService {

    val notifications: SnapshotStateList<Notification> = mutableStateListOf()

    fun push(message: String) {
        notifications += Notification(message)
    }

    fun dismiss(notification: Notification) {
        notifications -= notification
    }

    fun dismissAll() {
        notifications.clear()
    }

    class Notification(val message: String)
}
