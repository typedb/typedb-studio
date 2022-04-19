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
import com.vaticle.typedb.studio.state.connection.SchemaType
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.Sentence
import com.vaticle.typedb.studio.view.common.component.ContextMenu
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Form.IconArg
import com.vaticle.typedb.studio.view.common.component.Form.IconButtonArg
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.Navigator
import com.vaticle.typedb.studio.view.common.theme.Theme

internal class TypeBrowser(state: BrowserArea.State, order: Int, initOpen: Boolean = false) :
    Browser(state, order, initOpen) {

    override val label: String = Label.TYPES
    override val icon: Icon.Code = Icon.Code.SITEMAP
    override val isActive: Boolean get() = GlobalState.connection.isConnected && GlobalState.connection.current!!.session.isOpen
    override var buttons: List<IconButtonArg> by mutableStateOf(emptyList())

    @Composable
    override fun BrowserLayout() {
        val conMgr = GlobalState.connection
        if (!conMgr.isConnected) ConnectToServerHelper()
        else if (!conMgr.current!!.isInteractiveMode) NonInteractiveModeMessage()
        else if (!conMgr.current!!.session.isOpen || conMgr.selectDatabaseDialog.isOpen) SelectDBHelper()
        else Content()
    }

    @Composable
    private fun Content() {
        val conMgr = GlobalState.connection
        val session = conMgr.current!!.session
        val navState = Navigator.rememberNavigatorState(
            container = session.rootSchemaType!!,
            title = Label.TYPE_BROWSER,
            initExpandDepth = 1,
        ) { } // TODO
        session.onSessionChange = { navState.replaceContainer(it) }
        session.onSchemaWrite = { navState.reloadEntriesAndExpand(1) }
        buttons = listOf(reloadButton(navState), exportButton(navState)) + navState.buttons
        Navigator.Layout(
            state = navState,
            iconArg = { typeIcon(it) },
            styleArgs = { listOf() },
            contextMenuFn = { item, onChangeEntries -> contextMenuItems(item, onChangeEntries) }
        )
    }

    private fun reload(navState: Navigator.NavigatorState<SchemaType>) {
        GlobalState.connection.current?.session?.resetSchemaReadTx()
        navState.reloadEntries()
    }

    private fun reloadButton(navState: Navigator.NavigatorState<SchemaType>): IconButtonArg {
        return IconButtonArg(Icon.Code.ROTATE) { reload(navState) }
    }

    private fun exportButton(navState: Navigator.NavigatorState<SchemaType>): IconButtonArg {
        return IconButtonArg(Icon.Code.SQUARE_ARROW_UP_RIGHT, enabled = GlobalState.project.current != null) {
            reload(navState)
            GlobalState.connection.current?.session?.exportTypeSchema { schema ->
                GlobalState.project.tryCreateUntitledFile()?.let { file ->
                    file.content(schema)
                    GlobalState.resource.open(file)
                }
            }
        }
    }

    private fun typeIcon(itemState: Navigator.ItemState<SchemaType>): IconArg {
        return when {
            itemState.item.isEntityType -> IconArg(Icon.Code.RECTANGLE, color = { Theme.colors.tertiary })
            itemState.item.isRelationType -> IconArg(Icon.Code.RHOMBUS, color = { Theme.colors.quaternary })
            itemState.item.isAttributeType -> IconArg(Icon.Code.OVAL, color = { Theme.colors.quinary })
            else -> throw IllegalStateException("Unrecognised Schema Type: " + itemState.item.toString())
        }
    }

    private fun contextMenuItems(
        itemState: Navigator.ItemState<SchemaType>, onChangeEntries: () -> Unit
    ): List<List<ContextMenu.Item>> {
        return listOf() // TODO
    }

    @Composable
    private fun ConnectToServerHelper() {
        Box(Modifier.fillMaxSize().background(Theme.colors.background2), Alignment.Center) {
            Form.TextButton(
                text = Label.CONNECT_TO_TYPEDB,
                onClick = { GlobalState.connection.connectServerDialog.open() },
                leadingIcon = Icon.Code.SERVER
            )
        }
    }

    @Composable
    private fun NonInteractiveModeMessage() {
        Box(Modifier.fillMaxSize().background(Theme.colors.background2), Alignment.Center) {
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
        Box(Modifier.fillMaxSize().background(Theme.colors.background2), Alignment.Center) {
            Form.TextButton(
                text = Label.SELECT_DATABASE,
                onClick = { GlobalState.connection.selectDatabaseDialog.open() },
                leadingIcon = Icon.Code.DATABASE
            )
        }
    }
}
