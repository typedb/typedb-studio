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

package com.vaticle.typedb.studio.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.controller.Controller
import com.vaticle.typedb.studio.controller.common.UserDataDirectory
import com.vaticle.typedb.studio.controller.notification.Message
import com.vaticle.typedb.studio.viewer.common.Label
import com.vaticle.typedb.studio.viewer.common.component.Form
import com.vaticle.typedb.studio.viewer.common.component.Layout
import com.vaticle.typedb.studio.viewer.common.theme.Theme
import com.vaticle.typedb.studio.viewer.navigator.Navigator
import kotlin.system.exitProcess
import mu.KLogger
import mu.KotlinLogging.logger

object Studio {

    private val ERROR_WINDOW_WIDTH = 1000.dp
    private val ERROR_WINDOW_HEIGHT = 610.dp

    @JvmStatic
    fun main(args: Array<String>) {
        var logger: KLogger? = null
        try {
            Message.loadClasses()
            UserDataDirectory.initialise()
            logger = logger {}
            application { MainWindow(it) }
        } catch (exception: Exception) {
            application { ErrorWindow(exception, it) }
        } finally {
            logger?.debug { Label.CLOSING_TYPEDB_STUDIO }
            exitProcess(0)
        }
    }

    private fun application(window: @Composable (onExit: () -> Unit) -> Unit) {
        androidx.compose.ui.window.application {
            Theme.Material {
                // TODO: we don't want to call exitApplication() onCloseRequest for MacOS
                window { exitApplication() }
            }
        }
    }

    @Composable
    private fun MainWindow(onClose: () -> Unit) {
        // TODO: we want no title bar, by passing undecorated=true, but it seems to cause intermittent crashes on startup
        //       (see #40). Test if they occur when running the distribution, or only with bazel run :studio-bin-*
        Window(
            title = Label.TYPEDB_STUDIO,
            onCloseRequest = { onClose() },
            state = rememberWindowState(WindowPlacement.Maximized)
        ) {
            Column(modifier = Modifier.fillMaxSize().background(Theme.colors.background)) {
                Toolbar.Area()
                Layout.HorizontalSeparator()
                Layout.ResizableRow(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    separator = Layout.Separator(Layout.SEPARATOR_WEIGHT),
                    Layout.Item(
                        id = Navigator.ID,
                        initSize = Either.first(Navigator.WIDTH),
                        minSize = Navigator.MIN_WIDTH
                    ) { Navigator.Area(it) },
                    Layout.Item(
                        id = Page.ID,
                        initSize = Either.second(1f),
                        minSize = Page.MIN_WIDTH
                    ) { Page.Area() }
                )
                Layout.HorizontalSeparator()
                StatusBar.Area()
            }
            Notification.Area()
        }
        if (Controller.connection.showWindow) Connection.Window()
        if (Controller.project.showWindow) Project.Window()
    }

    @Composable
    private fun ErrorWindow(exception: Exception, onClose: () -> Unit) {
        Window(
            title = Label.TYPEDB_STUDIO_APPLICATION_ERROR,
            onCloseRequest = { onClose() },
            state = rememberWindowState(
                placement = WindowPlacement.Floating,
                position = WindowPosition.Aligned(Alignment.Center),
                size = DpSize(ERROR_WINDOW_WIDTH, ERROR_WINDOW_HEIGHT),
            )
        ) {
            Column(modifier = Modifier.fillMaxSize().background(Theme.colors.background).padding(5.dp)) {
                val rowVerticalAlignment = Alignment.Top
                val rowModifier = Modifier.padding(5.dp)
                val labelModifier = Modifier.width(40.dp)
                val labelStyle = Theme.typography.body1.copy(fontWeight = FontWeight.Bold)
                val contentColor = Theme.colors.error2
                Row(verticalAlignment = rowVerticalAlignment, modifier = rowModifier) {
                    Form.Text(value = "${Label.TITLE}:", modifier = labelModifier, style = labelStyle)
                    exception.message?.let { Form.TextSelectable(value = it, color = contentColor) }
                }
                Row(verticalAlignment = rowVerticalAlignment, modifier = rowModifier) {
                    Form.Text(value = "${Label.TRACE}:", modifier = labelModifier, style = labelStyle)
                    Form.TextSelectable(value = exception.stackTraceToString(), color = contentColor)
                }
            }
        }
    }
}
