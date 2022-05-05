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

package com.vaticle.typedb.studio.view.page

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.state.resource.Resource
import com.vaticle.typedb.studio.state.schema.TypeState
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.Util.toDP
import com.vaticle.typedb.studio.view.common.Util.typeIcon
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.Scrollbar
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.component.Tooltip
import com.vaticle.typedb.studio.view.common.theme.Theme

class TypePage constructor(private var type: TypeState) : Page(type) {

    override val name: String = type.name
    override val icon: Form.IconArg = typeIcon(type)

    private val horScroller = ScrollState(0)
    private val verScroller = ScrollState(0)
    private var width: Dp by mutableStateOf(0.dp)
    private val isEditable get() = type.schemaMgr.hasWriteTx && !type.isRoot && !GlobalState.client.hasRunningCommand

    companion object {
        private val MIN_WIDTH = 600.dp
        private val MAX_WIDTH = 900.dp
        private val PAGE_PADDING = 40.dp
        private val HORIZONTAL_SPACING = 6.dp
        private val VERTICAL_SPACING = 18.dp
    }

    override fun updateResourceInner(resource: Resource) {
        type = resource as TypeState
    }

    @Composable
    override fun Content() {
        val focusReq = remember { FocusRequester() }
        val density = LocalDensity.current.density
        val bgColor = Theme.colors.background0
        Box(Modifier.background(bgColor).focusRequester(focusReq).focusable()
            .onGloballyPositioned { width = toDP(it.size.width, density) }) {
            Box(Modifier.fillMaxSize().horizontalScroll(horScroller).verticalScroll(verScroller), Alignment.TopCenter) {
                Column(
                    modifier = Modifier.width(width.coerceIn(MIN_WIDTH, MAX_WIDTH)).padding(PAGE_PADDING),
                    verticalArrangement = Arrangement.spacedBy(VERTICAL_SPACING),
                ) { TypeSections() }
            }
            Scrollbar.Vertical(rememberScrollbarAdapter(verScroller), Modifier.align(Alignment.CenterEnd))
            Scrollbar.Horizontal(rememberScrollbarAdapter(horScroller), Modifier.align(Alignment.BottomCenter))
        }
        LaunchedEffect(focusReq) { focusReq.requestFocus() }
    }

    @Composable
    private fun TypeSections() {
        TitleSection()
        SupertypeSection()
        AbstractSection()
        when {
            type.isEntityType -> EntityTypeSections()
            type.isRelationType -> RelationTypeSections()
            type.isAttributeType -> AttributeTypeSections()
        }
        DeleteButton()
    }

    @Composable
    private fun EntityTypeSections() {
        OwnedAttributesSection()
        PlayedRolesSection()
        SubtypesSection()
    }

    @Composable
    private fun RelationTypeSections() {
        RelatedRolesSection()
        OwnedAttributesSection()
        SubtypesSection()
        AdvanceSections {
            PlayedRolesSection()
        }
    }

    @Composable
    private fun AttributeTypeSections() {
        AttributeOwnersSection()
        SubtypesSection()
        AdvanceSections {
            PlayedRolesSection()
        }
    }

    @Composable
    private fun AdvanceSections(sections: @Composable () -> Unit) {

    }

    @Composable
    private fun TitleSection() {
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(HORIZONTAL_SPACING)) {
            Form.TextBox(value = type.name, leadingIcon = typeIcon(type))
            Spacer(modifier = Modifier.weight(1f))
            Form.IconButton(
                icon = Icon.Code.PEN,
                enabled = isEditable,
                tooltip = Tooltip.Arg(Label.RENAME)
            ) { } // TODO
            Form.IconButton(
                icon = Icon.Code.ROTATE,
                tooltip = Tooltip.Arg(Label.REFRESH)
            ) { type.reloadProperties() }
            Form.IconButton(
                icon = Icon.Code.ARROW_UP_RIGHT_FROM_SQUARE,
                tooltip = Tooltip.Arg(Label.EXPORT)
            ) { } // TODO
        }
    }

    @Composable
    private fun SupertypeSection() {
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(HORIZONTAL_SPACING), Alignment.CenterVertically) {
            Form.Text(value = Label.SUPERTYPE)
            Separator.Horizontal(modifier = Modifier.weight(1f))
            Form.TextButton(
                text = type.supertype?.name ?: "(${Label.NONE.lowercase()})",
                leadingIcon = type.supertype?.let { typeIcon(it) },
                enabled = type.supertype != null,
            ) { type.supertype?.let { GlobalState.resource.open(it) } }
            Form.IconButton(
                icon = Icon.Code.PEN,
                enabled = isEditable,
                tooltip = Tooltip.Arg(Label.EDIT)
            ) { } // TODO
        }
    }

    @Composable
    private fun AbstractSection() {
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(HORIZONTAL_SPACING), Alignment.CenterVertically) {
            Form.Text(value = Label.ABSTRACT)
            Separator.Horizontal(modifier = Modifier.weight(1f))
            Form.TextBox(((if (type.isAbstract) "" else Label.NOT + " ") + Label.ABSTRACT).lowercase())
            Form.IconButton(
                icon = Icon.Code.PEN,
                enabled = isEditable,
                tooltip = Tooltip.Arg(Label.EDIT)
            ) { } // TODO
        }
    }

    @Composable
    private fun OwnedAttributesSection() {

    }

    @Composable
    private fun AttributeOwnersSection() {

    }

    @Composable
    private fun PlayedRolesSection() {

    }

    @Composable
    private fun RelatedRolesSection() {

    }

    @Composable
    private fun SubtypesSection() {

    }

    @Composable
    private fun DeleteButton() {

    }
}
