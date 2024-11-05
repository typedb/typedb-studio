/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.module.type

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.typedb.studio.framework.common.theme.Theme
import com.typedb.studio.framework.material.Browsers
import com.typedb.studio.framework.material.ConceptDisplay.iconOf
import com.typedb.studio.framework.material.ContextMenu
import com.typedb.studio.framework.material.Form
import com.typedb.studio.framework.material.Form.IconButtonArg
import com.typedb.studio.framework.material.Icon
import com.typedb.studio.framework.material.Navigator
import com.typedb.studio.framework.material.Navigator.rememberNavigatorState
import com.typedb.studio.framework.material.Tooltip
import com.typedb.studio.service.Service
import com.typedb.studio.service.common.util.Label
import com.typedb.studio.service.common.util.Sentence
import com.typedb.studio.service.schema.ThingTypeState

class TypeBrowser(isOpen: Boolean = false, order: Int) : Browsers.Browser(isOpen, order) {

    override val label: String = Label.TYPE_BROWSER
    override val icon: Icon = Icon.TYPES
    override val isActive: Boolean get() = Service.driver.isConnected && Service.driver.session.isOpen
    override var buttons: List<IconButtonArg> by mutableStateOf(emptyList())

    private val schemaIsWritable get() = Service.schema.isWritable

    @Composable
    override fun Content() {
        val driver = Service.driver
        val schema = Service.schema
        if (!driver.isConnected) ConnectToServerHelper()
        else if (!driver.isInteractiveMode) NonInteractiveModeMessage()
        else if (!driver.session.isOpen || driver.selectDBDialog.isOpen || !schema.isOpen) SelectDBHelper()
        else NavigatorLayout()
    }

    @Composable
    private fun NavigatorLayout() {
        val navState = rememberNavigatorState(
            container = Service.schema,
            title = Label.TYPE_BROWSER,
            behaviour = Navigator.Behaviour.Browser(),
            initExpandDepth = 1,
            openFn = { it.item.tryOpen() },
            contextMenuFn = { contextMenuItems(it) }
        ) { Service.schema.onTypesUpdated { it.reloadEntriesAsync() } }
        buttons = listOf(refreshButton(navState), exportButton(navState)) + navState.buttons
        Navigator.Layout(
            state = navState,
            modifier = Modifier.fillMaxSize(),
            iconArg = { iconOf(it.item.conceptType) }
        )
        LaunchedEffect(navState) { navState.launch() }
    }

    private fun refresh(navState: Navigator.NavigatorState<ThingTypeState<*, *>>) {
        Service.schema.closeReadTx()
        navState.reloadEntriesAsync()
    }

    private fun refreshButton(navState: Navigator.NavigatorState<ThingTypeState<*, *>>) = IconButtonArg(
        icon = Icon.REFRESH,
        enabled = !Service.schema.hasRunningCommand,
        tooltip = Tooltip.Arg(title = Label.REFRESH)
    ) { refresh(navState) }

    private fun exportButton(navState: Navigator.NavigatorState<ThingTypeState<*, *>>) = IconButtonArg(
        icon = Icon.EXPORT,
        enabled = Service.project.current != null && !Service.schema.hasRunningCommand,
        tooltip = Tooltip.Arg(title = Label.EXPORT_SCHEMA_TYPES)
    ) {
        Service.schema.exportSchemaTypesAsync { schemaTypes ->
            refresh(navState)
            Service.project.tryCreateUntitledFile()?.let { file ->
                file.content(schemaTypes)
                file.tryOpen()
            }
        }
    }

    private fun contextMenuItems(itemState: Navigator.ItemState<ThingTypeState<*, *>>): List<List<ContextMenu.Item>> {
        val typeState = itemState.item
        if (!typeState.isOpen) typeState.loadTypeDependenciesAsync()
        return listOf(
            listOf(
                ContextMenu.Item(
                    label = Label.OPEN,
                    icon = Icon.OPEN
                ) { typeState.tryOpen() },
            ),
            listOf(
                ContextMenu.Item(
                    label = Label.CREATE_SUBTYPE,
                    icon = Icon.SUBTYPE_CREATE,
                    enabled = schemaIsWritable
                ) { typeState.initiateCreateSubtype { itemState.expand() } },
                ContextMenu.Item(
                    label = Label.RENAME_TYPE,
                    icon = Icon.RENAME,
                    enabled = schemaIsWritable
                ) { typeState.initiateRename() }
            ),
            listOf(
                ContextMenu.Item(
                    label = Label.DELETE,
                    icon = Icon.DELETE,
                    enabled = schemaIsWritable && typeState.canBeDeleted
                ) { typeState.initiateDelete() }
            )
        )
    }

    @Composable
    private fun ConnectToServerHelper() {
        Box(Modifier.fillMaxSize().background(Theme.studio.backgroundLight), Alignment.Center) {
            Form.TextButton(
                text = Label.CONNECT_TO_TYPEDB,
                leadingIcon = Form.IconArg(Icon.CONNECT_TO_TYPEDB)
            ) { Service.driver.connectServerDialog.open() }
        }
    }

    @Composable
    private fun NonInteractiveModeMessage() {
        Box(Modifier.fillMaxSize().background(Theme.studio.backgroundLight), Alignment.Center) {
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
        Box(Modifier.fillMaxSize().background(Theme.studio.backgroundLight), Alignment.Center) {
            Form.TextButton(
                text = Label.SELECT_DATABASE,
                leadingIcon = Form.IconArg(Icon.DATABASE)
            ) { Service.driver.selectDBDialog.open() }
        }
    }
}
