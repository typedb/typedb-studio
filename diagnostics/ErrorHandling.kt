package com.vaticle.typedb.studio.diagnostics

import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.vaticle.typedb.studio.diagnostics.LogLevel.DEBUG
import com.vaticle.typedb.studio.diagnostics.LogLevel.ERROR
import com.vaticle.typedb.studio.diagnostics.LogLevel.INFO
import com.vaticle.typedb.studio.diagnostics.LogLevel.TRACE
import com.vaticle.typedb.studio.diagnostics.LogLevel.WARN
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mu.KLogger

class ErrorHandler(
    val logger: KLogger, private val snackbarHostState: SnackbarHostState,
    private val snackbarCoroutineScope: CoroutineScope
) {
    fun handleError(
        error: Throwable, message: () -> String = { "An error occurred" }, logLevel: LogLevel = ERROR,
        showSnackbar: Boolean = true, snackbarDuration: SnackbarDuration = SnackbarDuration.Long
    ) {
        snackbarCoroutineScope.launch {
            when (logLevel) {
                ERROR -> logger.error(error) { message() }
                WARN -> logger.warn(error) { message() }
                INFO -> logger.info(error) { message() }
                DEBUG -> logger.debug(error) { message() }
                TRACE -> logger.trace(error) { message() }
            }
            if (showSnackbar) {
                val errorMessage = if (error.message?.isNotBlank() == true) error.message!! else error.toString()
                snackbarHostState.showSnackbar(errorMessage, actionLabel = "HIDE", snackbarDuration)
            }
        }
    }
}

@Composable
fun rememberErrorHandler(
    logger: KLogger, snackbarHostState: SnackbarHostState, snackbarCoroutineScope: CoroutineScope
): ErrorHandler = remember { ErrorHandler(logger, snackbarHostState, snackbarCoroutineScope) }

fun withErrorHandling(
    handler: ErrorHandler, message: () -> String = { "An error occurred" }, logLevel: LogLevel = ERROR,
    showSnackbar: Boolean = true, snackbarDuration: SnackbarDuration = SnackbarDuration.Long, action: () -> Unit
) {
    try {
        action()
    } catch (e: Exception) {
        handler.handleError(e, message, logLevel, showSnackbar, snackbarDuration)
    }
}

enum class LogLevel {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR
}
