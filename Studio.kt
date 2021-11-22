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
import com.vaticle.typedb.studio.common.component.Separator
import com.vaticle.typedb.studio.common.notification.Message
import com.vaticle.typedb.studio.common.system.UserDataDirectory
import com.vaticle.typedb.studio.common.theme.Theme
import com.vaticle.typedb.studio.connection.ConnectionWindow
import com.vaticle.typedb.studio.navigator.NavigatorArea
import com.vaticle.typedb.studio.notification.NotificationArea
import com.vaticle.typedb.studio.page.PageArea
import com.vaticle.typedb.studio.service.Service
import com.vaticle.typedb.studio.statusbar.StatusBarArea
import com.vaticle.typedb.studio.toolbar.ToolbarArea
import mu.KotlinLogging.logger

object Studio {

    private fun application(window: @Composable (onExit: () -> Unit) -> Unit) {
        androidx.compose.ui.window.application {
            Theme.Material {
                window { exitApplication() }
            }
        }
    }

    @Composable
    private fun MainWindow(onExit: () -> Unit) {
        val log = logger {}
        // TODO: we want no title bar, by passing undecorated=true, but it seems to cause intermittent crashes on startup
        //       (see #40). Test if they occur when running the distribution, or only with bazel run :studio-bin-*
        // TODO: we don't want to exitApplication() onCloseRequest for MacOS
        Window(
            title = Label.TYPEDB_STUDIO,
            onCloseRequest = { log.debug { Label.CLOSING_TYPEDB_STUDIO }; onExit() },
            state = rememberWindowState(WindowPlacement.Maximized)
        ) {
            Column(modifier = Modifier.fillMaxSize().background(Theme.colors.background)) {
                ToolbarArea.Layout()
                Separator.Horizontal()
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    NavigatorArea.Layout()
                    Separator.Vertical()
                    PageArea.Layout()
                }
                Separator.Horizontal()
                StatusBarArea.Layout()
            }
            NotificationArea.Layout()
        }
        if (Service.connection.openDialog) ConnectionWindow.Layout()
    }

    @Composable
    private fun ErrorWindow(exception: Exception, onExit: () -> Unit) {
        Window(
            title = Label.TYPEDB_STUDIO_FAILED_TO_START,
            onCloseRequest = { onExit() },
            state = rememberWindowState(
                placement = WindowPlacement.Floating,
                position = WindowPosition.Aligned(Alignment.Center),
                size = WindowSize(width = 600.dp, 310.dp),
            )
        ) {
            Column(modifier = Modifier.fillMaxSize().background(Theme.colors.background).padding(5.dp)) {
                val rowModifier = Modifier.padding(5.dp)
                val labelModifier = Modifier.width(40.dp)
                val labelStyle = Theme.typography.body1.copy(fontWeight = FontWeight.Bold)
                val contentColor = Theme.colors.error2
                Row(verticalAlignment = Alignment.Top, modifier = rowModifier) {
                    Form.Text(value = Label.TITLE, modifier = labelModifier, style = labelStyle)
                    exception.message?.let { Form.TextSelectable(value = it, color = contentColor) }
                }
                Row(verticalAlignment = Alignment.Top, modifier = rowModifier) {
                    Form.Text(value = Label.TRACE, modifier = labelModifier, style = labelStyle)
                    Form.TextSelectable(value = exception.stackTraceToString(), color = contentColor)
                }
            }
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            // TODO: we should wrap this function body in a try-catch block and throw any errors into a native popup window
            Message.loadClasses()
            UserDataDirectory.initialise()
            application { MainWindow(it) }
        } catch (exception: Exception) {
            application { ErrorWindow(exception, it) }
        }
    }
}
