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

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement.Maximized
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.vaticle.typedb.studio.common.system.UserDataDirectory
import mu.KotlinLogging.logger

@Composable
fun Studio(onCloseRequest: () -> Unit) {

    // TODO: we want no title bar, by passing undecorated = true, but it seems to cause intermittent crashes on startup
    //       (see #40). Test if they occur when running the distribution, or only with bazel run :studio-bin-*
    Window(title = "TypeDB Studio", onCloseRequest = onCloseRequest, state = rememberWindowState(Maximized)) {
//        StudioTheme {
//            Column(modifier = Modifier.fillMaxWidth().background(StudioTheme.colors.background)) {
//                Toolbar.Area()
//                Separator.Horizontal()
//                Row(Modifier.fillMaxWidth().weight(1f)) {
//                    Navigator.Area()
//                    Separator.Vertical()
//                    Page.Area()
//                }
//                Separator.Horizontal()
//                StatusBar.Area()
//            }
//        }
    }
//    if (Service.connection.openDialog) {
//        ConnectionDialog.Window()
//    }
}

fun main() {
    UserDataDirectory.initialise()
    val log = logger {}

    application {
        fun onCloseRequest() {
            log.debug { "Closing TypeDB Studio" }
            exitApplication() // TODO: I think this is the wrong behaviour on MacOS
        }
        Studio(::onCloseRequest)
    }
}
