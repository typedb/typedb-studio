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

package com.vaticle.typedb.studio.diagnostics

import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.vaticle.typedb.client.common.exception.TypeDBClientException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mu.KLogger

class ErrorReporter(
    val logger: KLogger, private val snackbarHostState: SnackbarHostState,
    private val snackbarCoroutineScope: CoroutineScope
) {
    fun reportIDEError(error: Throwable) {
        snackbarCoroutineScope.launch {
            logger.error("An IDE error occurred", error)
            val errorMessage = if (error.message?.isNotBlank() == true) error.message!! else error.toString()
            snackbarHostState.showSnackbar(errorMessage, actionLabel = "HIDE", SnackbarDuration.Long)
        }
    }

    fun reportOddBehaviour(error: Throwable) {
        logger.warn("An unexpected error occurred", error)
    }

    fun reportUserError(error: Throwable, message: () -> String) {
        snackbarCoroutineScope.launch {
            logger.info(error) { message() }
            val errorMessage = "${message()}\n\n" +
                    if (error.message?.isNotBlank() == true) error.message!! else error.toString()
            snackbarHostState.showSnackbar(errorMessage, actionLabel = "HIDE", SnackbarDuration.Long)
        }
    }

    fun reportTypeDBClientError(error: TypeDBClientException, message: () -> String = { "An error occurred" }) {
        snackbarCoroutineScope.launch {
            logger.info(error) { message() }
            val errorMessage = when (error.message?.isNotBlank()) {
                true -> {
                    val rawMessage = error.message!!
                    val matchResult: MatchResult? = Regex("^\\[[A-Z]{3}[0-9]+][^:]*:").find(rawMessage)
                    if (matchResult != null) {
                        // This converts, for example, "[QRY04] Query Error: Invalid thing write" into
                        // "[QRY04] Query Error\n\nInvalid thing write"
                        rawMessage.take(matchResult.range.last) + "\n\n" + rawMessage.drop(matchResult.range.last + 2)
                    } else {
                        rawMessage
                    }
                }
                else -> error.toString()
            }
            snackbarHostState.showSnackbar(errorMessage, actionLabel = "HIDE", SnackbarDuration.Long)
        }
    }
}

@Composable
fun rememberErrorReporter(
    logger: KLogger, snackbarHostState: SnackbarHostState, snackbarCoroutineScope: CoroutineScope
): ErrorReporter = remember { ErrorReporter(logger, snackbarHostState, snackbarCoroutineScope) }
