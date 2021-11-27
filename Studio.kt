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

package com.vaticle.typedb.studio

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowSize
import androidx.compose.ui.window.rememberWindowState
import com.vaticle.typedb.studio.common.Label
import com.vaticle.typedb.studio.common.component.Form
import com.vaticle.typedb.studio.common.component.Layout
import com.vaticle.typedb.studio.common.component.Separator
import com.vaticle.typedb.studio.common.notification.Message
import com.vaticle.typedb.studio.common.system.UserDataDirectory
import com.vaticle.typedb.studio.common.theme.Theme
import com.vaticle.typedb.studio.connection.ConnectionWindow
import com.vaticle.typedb.studio.navigator.NavigatorArea
import com.vaticle.typedb.studio.notification.NotificationArea
import com.vaticle.typedb.studio.page.PageArea
import com.vaticle.typedb.studio.project.ProjectWindow
import com.vaticle.typedb.studio.service.Service
import com.vaticle.typedb.studio.statusbar.StatusBarArea
import com.vaticle.typedb.studio.toolbar.ToolbarArea
import mu.KLogger
import mu.KotlinLogging.logger
import kotlin.system.exitProcess

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
                ToolbarArea.Layout()
                Separator.Horizontal()
                Layout.ResizableRow(
                    splitCount = 2,
                    separatorWidth = Separator.WEIGHT,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) { rowState: Layout.RowState ->
                    NavigatorArea.Layout(rowState.items[0])
                    Separator.Vertical()
                    PageArea.Layout(rowState.items[1])
                }
                Separator.Horizontal()
                StatusBarArea.Layout()
            }
            NotificationArea.Layout()
        }
        if (Service.connection.showWindow) ConnectionWindow.Layout()
        if (Service.project.showWindow) ProjectWindow.Layout()
    }

    @Composable
    private fun ErrorWindow(exception: Exception, onClose: () -> Unit) {
        Window(
            title = Label.TYPEDB_STUDIO_APPLICATION_ERROR,
            onCloseRequest = { onClose() },
            state = rememberWindowState(
                placement = WindowPlacement.Floating,
                position = WindowPosition.Aligned(Alignment.Center),
                size = WindowSize(ERROR_WINDOW_WIDTH, ERROR_WINDOW_HEIGHT),
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
