package com.vaticle.typedb.studio.diagnostics

import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import com.vaticle.typedb.studio.diagnostics.LogLevel.DEBUG
import com.vaticle.typedb.studio.diagnostics.LogLevel.ERROR
import com.vaticle.typedb.studio.diagnostics.LogLevel.INFO
import com.vaticle.typedb.studio.diagnostics.LogLevel.TRACE
import com.vaticle.typedb.studio.diagnostics.LogLevel.WARN
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mu.KLogger

fun withErrorHandling(
    message: () -> String, logger: KLogger, snackbarHostState: SnackbarHostState,
    snackbarCoroutineScope: CoroutineScope, logLevel: LogLevel = ERROR,
    snackbarDuration: SnackbarDuration = SnackbarDuration.Long, action: () -> Unit
) {
    try {
        action()
    } catch (e: Exception) {
        snackbarCoroutineScope.launch {
            when (logLevel) {
                ERROR -> logger.error(e) { message() }
                WARN -> logger.warn(e) { message() }
                INFO -> logger.info(e) { message() }
                DEBUG -> logger.debug(e) { message() }
                TRACE -> logger.trace(e) { message() }
            }
            snackbarHostState.showSnackbar(e.toString(), actionLabel = "HIDE", snackbarDuration)
        }
    }
}

enum class LogLevel {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR
}
