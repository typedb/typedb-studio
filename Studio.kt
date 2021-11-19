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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement.Maximized
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.vaticle.typedb.studio.common.Label
import com.vaticle.typedb.studio.common.component.Separator
import com.vaticle.typedb.studio.common.system.UserDataDirectory
import com.vaticle.typedb.studio.common.theme.Theme
import com.vaticle.typedb.studio.connection.ConnectionWindow
import com.vaticle.typedb.studio.navigator.NavigatorArea
import com.vaticle.typedb.studio.page.PageArea
import com.vaticle.typedb.studio.service.Service
import com.vaticle.typedb.studio.statusbar.StatusBar
import com.vaticle.typedb.studio.toolbar.ToolbarArea
import mu.KotlinLogging.logger

object Studio {

    @Composable
    fun Application(onCloseRequest: () -> Unit) {
        Theme.Material {
            // TODO: we want no title bar, by passing undecorated=true, but it seems to cause intermittent crashes on startup
            //       (see #40). Test if they occur when running the distribution, or only with bazel run :studio-bin-*
            Window(
                title = Label.TYPEDB_STUDIO,
                onCloseRequest = onCloseRequest,
                state = rememberWindowState(Maximized)
            ) {
                Column(modifier = Modifier.fillMaxWidth().background(Theme.colors.background)) {
                    ToolbarArea.Layout()
                    Separator.Horizontal()
                    Row(Modifier.fillMaxWidth().weight(1f)) {
                        NavigatorArea.Layout()
                        Separator.Vertical()
                        PageArea.Layout()
                    }
                    Separator.Horizontal()
                    StatusBar.Area()
                }

            }
            if (Service.connection.openDialog) ConnectionWindow.Layout()
        }
    }

    @JvmStatic
    fun main(args: Array<String>) {
        UserDataDirectory.initialise()
        val log = logger {}

        application {
            fun onCloseRequest() {
                log.debug { Label.CLOSING_TYPEDB_STUDIO }
                exitApplication() // TODO: I think this is the wrong behaviour on MacOS
            }
            Application(::onCloseRequest)
        }
    }
}
