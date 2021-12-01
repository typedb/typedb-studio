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
import mu.KLogger

class NotificationManager {

    val queue: SnapshotStateList<Notification> = mutableStateListOf();

    fun info(message: String, logger: KLogger) {
        logger.info { message }
        queue += Notification(Notification.Type.INFO, message)
    }

    fun userError(error: Error.User, logger: KLogger) {
        logger.error { error.message }
        queue += Notification(Notification.Type.ERROR, error.message)
    }

    fun systemError(error: Error.System, logger: KLogger) {
        logger.error { error.message }
        logger.error { error.cause }
        queue += Notification(Notification.Type.ERROR, error.message)
    }

    fun dismiss(message: Notification) {
        queue -= message
    }
}
