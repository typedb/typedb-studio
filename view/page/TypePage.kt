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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.state.resource.Resource
import com.vaticle.typedb.studio.state.schema.TypeState
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.Sentence
import com.vaticle.typedb.studio.view.common.Util.toDP
import com.vaticle.typedb.studio.view.common.Util.typeIcon
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Form.ClickableText
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.Scrollbar
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.component.Table
import com.vaticle.typedb.studio.view.common.component.Tooltip
import com.vaticle.typedb.studio.view.common.theme.Color.FADED_OPACITY
import com.vaticle.typedb.studio.view.common.theme.Theme

class TypePage constructor(private var type: TypeState) : Page(type) {

    override val icon: Form.IconArg = typeIcon(type)

    private val focusReq = FocusRequester()
    private val horScroller = ScrollState(0)
    private val verScroller = ScrollState(0)
    private var width: Dp by mutableStateOf(0.dp)
    private val isEditable get() = type.schemaMgr.hasWriteTx && !type.isRoot && !GlobalState.client.hasRunningCommand

    companion object {
        private val MIN_WIDTH = 600.dp
        private val MAX_WIDTH = 900.dp
        private val PAGE_PADDING = 40.dp
        private val HORIZONTAL_SPACING = 8.dp
        private val VERTICAL_SPACING = 16.dp
        private val ICON_COL_WIDTH = 80.dp
        private val TABLE_BUTTON_HEIGHT = 24.dp
    }

    override fun updateResourceInner(resource: Resource) {
        type = resource as TypeState
    }

    @Composable
    override fun Content() {
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
    private fun Separator() {
        Separator.Horizontal(color = Theme.colors.border.copy(alpha = FADED_OPACITY), modifier = Modifier.fillMaxWidth())
    }

    @Composable
    private fun TypeSections() {
        TitleSection()
        Separator()
        SupertypeSection()
        Separator()
        AbstractSection()
        Separator()
        when {
            type.isEntityType -> EntityTypeSections()
            type.isRelationType -> RelationTypeSections()
            type.isAttributeType -> AttributeTypeSections()
        }
        Separator()
        DeleteButton()
    }

    @Composable
    private fun EntityTypeSections() {
        OwnedAttributeTypesSection()
        PlayedRolesSection()
        SubtypesSection()
    }

    @Composable
    private fun RelationTypeSections() {
        RelatedRolesSection()
        OwnedAttributeTypesSection()
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
    private fun SectionLine(content: @Composable RowScope.() -> Unit) {
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(HORIZONTAL_SPACING), Alignment.CenterVertically) {
            content()
        }
    }

    @Composable
    private fun TitleSection() {
        SectionLine {
            Form.TextBox(text = fullName(type), leadingIcon = typeIcon(type))
            Spacer(modifier = Modifier.weight(1f))
            EditButton { } // TODO
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
        SectionLine {
            Form.Text(value = Label.SUPERTYPE)
            Spacer(modifier = Modifier.weight(1f))
            Form.TextButton(
                text = type.supertype?.let { fullName(it) } ?: AnnotatedString("(${Label.NONE.lowercase()})"),
                leadingIcon = type.supertype?.let { typeIcon(it) },
                enabled = type.supertype != null,
            ) { type.supertype?.let { GlobalState.resource.open(it) } }
            EditButton { } // TODO
        }
    }

    @Composable
    private fun AbstractSection() {
        SectionLine {
            Form.Text(value = Label.ABSTRACT)
            Spacer(modifier = Modifier.weight(1f))
            Form.TextBox(((if (type.isAbstract) "" else Label.NOT + " ") + Label.ABSTRACT).lowercase())
            EditButton { } // TODO
        }
    }

    @Composable
    private fun OwnedAttributeTypesSection() {
        SectionLine { Form.Text(value = Label.OWNED_ATTRIBUTE_TYPES) }
        OwnedAttributeTypesTable()
        OwnedAttributeTypeAddition()
    }

    @Composable
    private fun OwnedAttributeTypesTable() {
        val tableHeight = Table.ROW_HEIGHT * (type.ownedAttributeTypeProperties.size + 1).coerceAtLeast(2)

        @Composable
        fun MayTick(boolean: Boolean) {
            if (boolean) Icon.Render(icon = Icon.Code.CHECK, color = Theme.colors.secondary)
        }

        @Composable
        fun MayRemoveOwnedAttributeTypeButton(attTypeProp: TypeState.AttributeTypeProperties) {
            if (!attTypeProp.isInherited) Form.IconButton(
                icon = Icon.Code.MINUS,
                modifier = Modifier.size(TABLE_BUTTON_HEIGHT),
                iconColor = Theme.colors.error,
                enabled = isEditable,
                tooltip = Tooltip.Arg(Label.REMOVE_OWNED_ATTRIBUTE, Sentence.EDITING_TYPES_REQUIREMENT_DESCRIPTION),
                onClick = { type.removeOwnedAttribute(attTypeProp.attributeType) }
            )
        }

        Table.Layout(
            items = type.ownedAttributeTypeProperties.values.sortedBy { it.attributeType.name },
            modifier = Modifier.fillMaxWidth().height(tableHeight),
            columns = listOf(
                Table.Column(header = Label.ATTRIBUTES, contentAlignment = Alignment.CenterStart) { props ->
                    ClickableText(fullName(props.attributeType)) { GlobalState.resource.open(props.attributeType) }
                },
                Table.Column(header = Label.OVERRIDES, contentAlignment = Alignment.CenterStart) { props ->
                    props.overriddenType?.let { ot -> ClickableText(fullName(ot)) { GlobalState.resource.open(ot) } }
                },
                Table.Column(header = Label.KEY, size = Either.first(ICON_COL_WIDTH)) { MayTick(it.isKey) },
                Table.Column(header = Label.INHERITED, size = Either.first(ICON_COL_WIDTH)) { MayTick(it.isInherited) },
                Table.Column(
                    header = null,
                    size = Either.first(ICON_COL_WIDTH)
                ) { MayRemoveOwnedAttributeTypeButton(it) },
            )
        )
    }

    @Composable
    private fun OwnedAttributeTypeAddition() {
        val baseFontColor = Theme.colors.onPrimary
        var attributeType: TypeState? by remember { mutableStateOf(null) }
        val attributeTypeList = GlobalState.schema.rootAttributeType?.subtypes?.filter {
            !type.ownedAttributeTypeProperties.values.map { p -> p.attributeType }.contains(it)
        }?.sortedBy { it.name } ?: listOf()

        var overriddenType: TypeState? by remember { mutableStateOf(null) }
        val overridableTypeList: List<TypeState> = attributeType?.supertypes
            ?.intersect(type.supertype!!.ownedAttributeTypes)
            ?.sortedBy { it.name } ?: listOf()

        val isOwnable = isEditable && attributeType != null
        val isOverridable = isEditable && overridableTypeList.isNotEmpty()
        val isKeyable = isEditable && attributeType?.isKeyable == true
        var isKey: Boolean by remember { mutableStateOf(false) }

        SectionLine {
            Box(Modifier.weight(1f)) {
                Form.Dropdown(
                    selected = attributeType,
                    placeholder = Label.SELECT_ATTRIBUTE_TYPE,
                    onExpand = { GlobalState.schema.rootAttributeType?.reloadEntriesRecursively() },
                    onSelection = { attributeType = it; it.reloadProperties() },
                    displayFn = { fullName(it, baseFontColor) },
                    modifier = Modifier.fillMaxSize(),
                    enabled = isEditable,
                    values = attributeTypeList
                )
            }
            Form.Text(value = Label.AS.lowercase(), enabled = isOverridable)
            Box(Modifier.weight(1f)) {
                Form.Dropdown(
                    selected = overriddenType,
                    placeholder = Label.SELECT_OVERRIDDEN_TYPE_OPTIONAL,
                    onSelection = { overriddenType = it },
                    displayFn = { fullName(it, baseFontColor) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isOverridable,
                    values = overridableTypeList
                )
            }
            Form.Text(value = Label.KEY.lowercase(), enabled = isKeyable)
            Form.Checkbox(
                value = isKey,
                enabled = isKeyable
            ) { isKey = it }
            Form.TextButton(
                text = Label.OWN,
                leadingIcon = Form.IconArg(Icon.Code.PLUS) { Theme.colors.secondary },
                enabled = isOwnable,
                tooltip = Tooltip.Arg(Label.ADD_OWNED_ATTRIBUTE_TYPE, Sentence.EDITING_TYPES_REQUIREMENT_DESCRIPTION),
                onClick = { type.addOwnedAttributes(attributeType!!, overriddenType, isKey) }
            )
        }
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

    @Composable
    private fun fullName(type: TypeState): AnnotatedString = fullName(type, Theme.colors.onPrimary)

    private fun fullName(type: TypeState, baseFontColor: Color): AnnotatedString {
        return buildAnnotatedString {
            append(type.name)
            if (type.isAttributeType && !type.isRoot) {
                append(" ")
                withStyle(SpanStyle(baseFontColor.copy(FADED_OPACITY))) { append("(${type.valueType})") }
            }
        }
    }

    @Composable
    private fun EditButton(onClick: () -> Unit) {
        Form.IconButton(
            icon = Icon.Code.PEN,
            enabled = isEditable,
            tooltip = Tooltip.Arg(Label.RENAME, Sentence.EDITING_TYPES_REQUIREMENT_DESCRIPTION),
            onClick = onClick
        )
    }
}
