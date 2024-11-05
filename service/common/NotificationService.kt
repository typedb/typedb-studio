/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.service.common

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.typedb.studio.service.common.NotificationService.Notification.Type.ERROR
import com.typedb.studio.service.common.NotificationService.Notification.Type.INFO
import com.typedb.studio.service.common.NotificationService.Notification.Type.WARNING
import com.typedb.studio.service.common.util.Message
import com.typedb.studio.service.common.util.Message.Companion.UNKNOWN
import com.typedb.studio.service.common.util.Message.System.Companion.UNEXPECTED_ERROR_IN_COROUTINE
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KLogger
import mu.KotlinLogging

class NotificationService {

    // Not a data class, because each object has to be unique
    class Notification constructor(val type: Type, val code: String, val message: String) {
        enum class Type { INFO, WARNING, ERROR }
    }

    val queue: SnapshotStateList<Notification> = mutableStateListOf()
    val isOpen: Boolean get() = queue.isNotEmpty()
    private val coroutines = CoroutineScope(Default)

    companion object {
        private val HIDE_DELAY = 10.seconds
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
        val notification = Notification(INFO, message.code(), stringOf(message, *params))
        queue += notification
        coroutines.launchAndHandle(this, LOGGER) {
            delay(HIDE_DELAY)
            dismiss(notification)
        }
    }

    fun userError(logger: KLogger, message: Message, vararg params: Any) = userNotification(
        logger, ERROR, message.code(), stringOf(message, *params)
    )

    fun userWarning(logger: KLogger, message: Message, vararg params: Any) = userNotification(
        logger, WARNING, message.code(), stringOf(message, *params)
    )

    fun systemWarning(logger: KLogger, cause: Throwable, message: Message, vararg params: Any) = systemNotification(
        logger, cause, WARNING, message.code(), stringOf(message, *params)
    )

    fun systemError(logger: KLogger, cause: Throwable, message: Message, vararg params: Any) = systemNotification(
        logger, cause, ERROR, message.code(), stringOf(message, *params)
    )

    fun dismiss(notification: Notification) {
        queue -= notification
    }

    fun dismissAll() {
        queue.clear()
    }

    private fun userNotification(
        logger: KLogger, type: Notification.Type, code: String, message: String
    ) = userNotification(logger, Notification(type, code, message))

    fun userNotification(logger: KLogger? = null, notification: Notification) {
        logger?.let { l ->
            when (notification.type) {
                INFO -> l.info { notification.message }
                WARNING -> l.warn { notification.message }
                ERROR -> l.error { notification.message }
            }
        }
        queue += notification
    }

    private fun systemNotification(
        logger: KLogger, cause: Throwable, type: Notification.Type, code: String, message: String
    ) {
        when (type) {
            INFO -> logger.info { message }
            WARNING -> logger.warn { message }
            ERROR -> {
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
