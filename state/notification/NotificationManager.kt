/*
 * Copyright (C) 2021 Vaticle
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.vaticle.typedb.studio.state.notification

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.vaticle.typedb.studio.state.common.Message
import com.vaticle.typedb.studio.state.notification.Notification.Type.ERROR
import com.vaticle.typedb.studio.state.notification.Notification.Type.INFO
import com.vaticle.typedb.studio.state.notification.Notification.Type.WARNING
import mu.KLogger

class NotificationManager {

    val queue: SnapshotStateList<Notification> = mutableStateListOf()

    fun info(logger: KLogger, message: Message, vararg params: Any) {
        logger.info { message }
        queue += Notification(INFO, message.code(), stringOf(message, *params))
    }

    fun userError(logger: KLogger, message: Message, vararg params: Any) {
        userNotification(logger, ERROR, message.code(), stringOf(message, *params))

    }

    fun userWarning(logger: KLogger, message: Message, vararg params: Any) {
        userNotification(logger, WARNING, message.code(), stringOf(message, *params))
    }

    fun systemWarning(logger: KLogger, cause: Throwable, message: Message, vararg params: Any) {
        systemNotification(logger, cause, WARNING, message.code(), stringOf(message, *params))
    }

    fun systemError(logger: KLogger, cause: Throwable, message: Message, vararg params: Any) {
        systemNotification(logger, cause, ERROR, message.code(), stringOf(message, *params))
    }

    fun dismiss(message: Notification) {
        queue -= message
    }

    fun dismissAll() {
        queue.clear()
    }

    private fun userNotification(logger: KLogger, type: Notification.Type, code: String, message: String) {
        when (type) {
            INFO -> logger.info { message }
            WARNING -> logger.warn { message }
            ERROR -> logger.error { message }
        }
        queue += Notification(type, code, message)
    }

    private fun systemNotification(
        logger: KLogger, cause: Throwable, type: Notification.Type,
        code: String, message: String
    ) {
        when (type) {
            INFO -> logger.info { message }
            WARNING -> logger.warn { message }
            ERROR -> {
                logger.error { message }
                logger.error { cause }
            }
        }
        queue += Notification(type, code, message)
    }

    private fun stringOf(message: Message, vararg params: Any): String {
        val messageStr = message.message(*params)
        assert(!messageStr.contains("%s"))
        return messageStr
    }
}
