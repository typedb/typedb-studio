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

package com.vaticle.typedb.studio.service

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.vaticle.typedb.studio.common.notification.Error
import mu.KLogger

class NotifierService {

    enum class MessageType { INFO, ERROR }
    class Message(val type: MessageType, val text: String) // not data class, because each object has to be unique

    val messages: SnapshotStateList<Message> = mutableStateListOf();

    fun info(message: String, logger: KLogger) {
        logger.info { message }
        messages += Message(MessageType.INFO, message)
    }

    fun userError(error: Error.User, logger: KLogger) {
        logger.error { error.message }
        messages += Message(MessageType.ERROR, error.message)
    }

    fun systemError(error: Error.System, logger: KLogger) {
        logger.error { error.message }
        logger.error { error.cause }
        messages += Message(MessageType.ERROR, error.message)
    }

    fun dismiss(message: Message) {
        messages -= message
    }
}
