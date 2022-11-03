/*
 * Copyright (C) 2022 Vaticle
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

package com.vaticle.typedb.studio.service.common

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.vaticle.typedb.studio.service.common.util.Message
import com.vaticle.typedb.studio.service.common.util.Message.Companion.UNKNOWN
import com.vaticle.typedb.studio.service.common.util.Message.System.Companion.UNEXPECTED_ERROR_IN_COROUTINE
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KLogger
import mu.KotlinLogging

@OptIn(ExperimentalTime::class)
class NotificationService {

    // not a data class, because each object has to be unique
    class Notification internal constructor(val type: Type, val code: String, val message: String) {
        enum class Type { INFO, WARNING, ERROR }
    }

    val queue: SnapshotStateList<Notification> = mutableStateListOf()
    val isOpen: Boolean get() = queue.isNotEmpty()
    private val coroutines = CoroutineScope(Default)

    companion object {
        private val HIDE_DELAY = Duration.seconds(10)
        private val LOGGER = KotlinLogging.logger {}

        fun <T> launchCompletableFuture(
            notificationSrv: NotificationService,
            logger: KLogger,
            function: () -> T
        ): CompletableFuture<T?> = CompletableFuture.supplyAsync {
            return@supplyAsync try {
                function()
            } catch (e: Throwable) {
                notificationSrv.systemError(logger, e, UNEXPECTED_ERROR_IN_COROUTINE, e.message ?: UNKNOWN)
                null
            }
        }

        fun CoroutineScope.launchAndHandle(
            notificationSrv: NotificationService,
            logger: KLogger,
            function: suspend () -> Unit
        ) = this.launch(Default) {
            try {
                function()
            } catch (_: CancellationException) {
            } catch (e: Throwable) {
                notificationSrv.systemError(logger, e, UNEXPECTED_ERROR_IN_COROUTINE, e.message ?: UNKNOWN)
            }
        }
    }

    fun info(logger: KLogger, message: Message, vararg params: Any) {
        logger.info { message }
        val notification = Notification(Notification.Type.INFO, message.code(), stringOf(message, *params))
        queue += notification
        coroutines.launchAndHandle(this, LOGGER) {
            delay(HIDE_DELAY)
            dismiss(notification)
        }
    }

    fun userError(logger: KLogger, message: Message, vararg params: Any) = userNotification(
        logger, Notification.Type.ERROR, message.code(), stringOf(message, *params)
    )

    fun userWarning(logger: KLogger, message: Message, vararg params: Any) = userNotification(
        logger, Notification.Type.WARNING, message.code(), stringOf(message, *params)
    )

    fun systemWarning(logger: KLogger, cause: Throwable, message: Message, vararg params: Any) = systemNotification(
        logger, cause, Notification.Type.WARNING, message.code(), stringOf(message, *params)
    )

    fun systemError(logger: KLogger, cause: Throwable, message: Message, vararg params: Any) = systemNotification(
        logger, cause, Notification.Type.ERROR, message.code(), stringOf(message, *params)
    )

    fun dismiss(notification: Notification) {
        queue -= notification
    }

    fun dismissAll() {
        queue.clear()
    }

    private fun userNotification(logger: KLogger, type: Notification.Type, code: String, message: String) {
        when (type) {
            Notification.Type.INFO -> logger.info { message }
            Notification.Type.WARNING -> logger.warn { message }
            Notification.Type.ERROR -> logger.error { message }
        }
        queue += Notification(type, code, message)
    }

    private fun systemNotification(
        logger: KLogger, cause: Throwable, type: Notification.Type, code: String, message: String
    ) {
        when (type) {
            Notification.Type.INFO -> logger.info { message }
            Notification.Type.WARNING -> logger.warn { message }
            Notification.Type.ERROR -> {
                logger.error { message }
                logger.error { cause.stackTraceToString() }
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
