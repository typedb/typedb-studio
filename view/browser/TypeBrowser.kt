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

package com.vaticle.typedb.studio.view.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.Sentence
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Form.IconButtonArg
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.theme.Theme

internal class TypeBrowser(state: BrowserArea.State, order: Int, initOpen: Boolean = false) :
    Browser(state, order, initOpen) {

    override val label: String = Label.TYPES
    override val icon: Icon.Code = Icon.Code.SITEMAP
    override val isActive: Boolean get() = GlobalState.connection.isConnected && GlobalState.connection.current!!.hasOpenSession
    override var buttons: List<IconButtonArg> by mutableStateOf(emptyList())

    @Composable
    override fun BrowserLayout() {
        val connectionMgr = GlobalState.connection
        if (!connectionMgr.isConnected) ConnectToServerHelper()
        else if (!connectionMgr.current!!.isInteractiveMode) NonInteractiveModeMessage()
        else if (!connectionMgr.current!!.hasOpenSession || connectionMgr.selectDatabaseDialog.isOpen) SelectDBHelper()
        else {

        }
    }

    @Composable
    private fun ConnectToServerHelper() {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().background(color = Theme.colors.background2)
        ) {
            Form.TextButton(
                text = Label.CONNECT_TO_TYPEDB,
                onClick = { GlobalState.connection.connectServerDialog.open() },
                leadingIcon = Icon.Code.SERVER
            )
        }
    }

    @Composable
    private fun NonInteractiveModeMessage() {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().background(color = Theme.colors.background2)
        ) {
            Form.Text(
                value = Sentence.TYPE_BROWSER_ONLY_INTERACTIVE,
                modifier = Modifier.padding(30.dp),
                align = TextAlign.Center,
                softWrap = true
            )
        }
    }

    @Composable
    private fun SelectDBHelper() {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().background(color = Theme.colors.background2)
        ) {
            Form.TextButton(
                text = Label.SELECT_DATABASE,
                onClick = { GlobalState.connection.selectDatabaseDialog.open() },
                leadingIcon = Icon.Code.DATABASE
            )
        }
    }
}
