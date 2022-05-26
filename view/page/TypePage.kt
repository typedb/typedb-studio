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
import androidx.compose.foundation.border
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.vaticle.typedb.studio.view.common.component.Navigator
import com.vaticle.typedb.studio.view.common.component.Scrollbar
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.component.Table
import com.vaticle.typedb.studio.view.common.component.Tooltip
import com.vaticle.typedb.studio.view.common.theme.Color.FADED_OPACITY
import com.vaticle.typedb.studio.view.common.theme.Theme
import kotlinx.coroutines.CoroutineScope

sealed class TypePage(type: TypeState.Thing, coroutineScope: CoroutineScope) : Page(type) {

    override val icon: Form.IconArg = typeIcon(type)
    protected abstract val type: TypeState.Thing

    private val focusReq = FocusRequester()
    private val horScroller = ScrollState(0)
    private val verScroller = ScrollState(0)
    private var width: Dp by mutableStateOf(0.dp)
    protected val isEditable get() = false // TODO: type.schemaMgr.hasWriteTx && !type.isRoot && !GlobalState.client.hasRunningCommand
    private val subtypesNavState = Navigator.NavigatorState(
        container = type,
        title = Label.SUBTYPES_OF + " " + type.name,
        initExpandDepth = 4,
        coroutineScope = coroutineScope
    ) { GlobalState.resource.open(it.item) }

    companion object {
        private val MIN_WIDTH = 600.dp
        private val MAX_WIDTH = 900.dp
        private val PAGE_PADDING = 40.dp
        private val HORIZONTAL_SPACING = 8.dp
        private val VERTICAL_SPACING = 16.dp
        private val ICON_COL_WIDTH = 80.dp
        private val TABLE_BUTTON_HEIGHT = 24.dp
        private val EMPTY_BOX_HEIGHT = Table.ROW_HEIGHT

        @Composable
        fun create(type: TypeState.Thing): TypePage {
            val coroutineScope = rememberCoroutineScope()
            return when (type) {
                is TypeState.Entity -> Entity(type, coroutineScope)
                is TypeState.Relation -> Relation(type, coroutineScope)
                is TypeState.Attribute -> Attribute(type, coroutineScope)
            }
        }
    }

    @Composable
    abstract fun MainSections()

    @Composable
    override fun Content() {
        val density = LocalDensity.current.density
        val bgColor = Theme.studio.background0
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
    protected fun Separator() {
        Separator.Horizontal(
            color = Theme.studio.border.copy(alpha = FADED_OPACITY),
            modifier = Modifier.fillMaxWidth()
        )
    }

    @Composable
    private fun TypeSections() {
        TitleSection()
        Separator()
        SupertypeSection()
        Separator()
        AbstractSection()
        Separator()
        MainSections()
        Separator()
        ButtonsSection()
    }

    @Composable
    protected fun AdvanceSections(sections: @Composable () -> Unit) {

    }

    @Composable
    protected fun SectionLine(content: @Composable RowScope.() -> Unit) {
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(HORIZONTAL_SPACING), Alignment.CenterVertically) {
            content()
        }
    }

    @Composable
    private fun TitleSection() {
        SectionLine {
            Form.TextBox(text = displayName(type), leadingIcon = typeIcon(type))
            EditButton { } // TODO
            Spacer(modifier = Modifier.weight(1f))
        }
    }

    @Composable
    private fun SupertypeSection() {
        SectionLine {
            Form.Text(value = Label.SUPERTYPE)
            Spacer(modifier = Modifier.weight(1f))
            Form.TextButton(
                text = type.supertype?.let { displayName(it) } ?: AnnotatedString("(${Label.THING.lowercase()})"),
                leadingIcon = type.supertype?.let { typeIcon(it) },
                enabled = !type.isRoot,
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
    protected fun OwnsAttributeTypesSection() {
        SectionLine { Form.Text(value = Label.OWNS_ATTRIBUTE_TYPES) }
        OwnsAttributeTypesTable()
        OwnsAttributeTypeAddition()
    }

    @Composable
    private fun OwnsAttributeTypesTable() {
        val tableHeight = Table.ROW_HEIGHT * (type.ownsAttributeTypes.size + 1).coerceAtLeast(2)
        Table.Layout(
            items = type.ownsAttributeTypeProperties.sortedBy { it.attributeType.name },
            modifier = Modifier.fillMaxWidth().height(tableHeight),
            columns = listOf(
                Table.Column(header = Label.OWNS, contentAlignment = Alignment.CenterStart) { props ->
                    ClickableText(displayName(props.attributeType)) { GlobalState.resource.open(props.attributeType) }
                },
                Table.Column(header = Label.OVERRIDDEN, contentAlignment = Alignment.CenterStart) { props ->
                    props.overriddenType?.let { ot -> ClickableText(displayName(ot)) { GlobalState.resource.open(ot) } }
                },
                Table.Column(header = Label.KEY, size = Either.second(ICON_COL_WIDTH)) { MayTickIcon(it.isKey) },
                Table.Column(header = Label.INHERITED, size = Either.second(ICON_COL_WIDTH)) {
                    MayTickIcon(it.isInherited)
                },
                Table.Column(header = null, size = Either.second(ICON_COL_WIDTH)) {
                    MayRemoveButton(Label.UNDEFINE_OWNS_ATTRIBUTE_TYPE, it.isInherited) {
                        type.undefineOwnsAttributeType(it.attributeType)
                    }
                },
            )
        )
    }

    @Composable
    private fun OwnsAttributeTypeAddition() {
        val baseFontColor = Theme.studio.onPrimary
        var attributeType: TypeState.Attribute? by remember { mutableStateOf(null) }
        val attributeTypeList = GlobalState.schema.rootAttributeType?.subtypes
            ?.filter { !type.ownsAttributeTypes.contains(it) }
            ?.sortedBy { it.name }
            ?: listOf()

        var overriddenType: TypeState.Attribute? by remember { mutableStateOf(null) }
        val overridableTypeList: List<TypeState.Attribute> = attributeType?.supertypes
            ?.intersect(type.supertype!!.ownsAttributeTypes.toSet())
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
                    onExpand = { GlobalState.schema.rootAttributeType?.loadSubtypesRecursively() },
                    onSelection = { attributeType = it; it.loadProperties() },
                    displayFn = { displayName(it, baseFontColor) },
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
                    displayFn = { displayName(it, baseFontColor) },
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
                text = Label.OWNS,
                leadingIcon = Form.IconArg(Icon.Code.PLUS) { Theme.studio.secondary },
                enabled = isOwnable,
                tooltip = Tooltip.Arg(Label.DEFINE_OWNS_ATTRIBUTE_TYPE, Sentence.EDITING_TYPES_REQUIREMENT_DESCRIPTION),
                onClick = { type.defineOwnsAttributeType(attributeType!!, overriddenType, isKey) }
            )
        }
    }

    @Composable
    protected fun PlaysRoleTypesSection() {
        SectionLine { Form.Text(value = Label.PLAYS_ROLE_TYPES) }
        RoleTypesTable(type.playsRoleTypeProperties) { type.undefinePlaysRoleType(it) }
        PlaysRoleTypeAddition()
    }

    @Composable
    protected fun RoleTypesTable(
        roleTypeProperties: List<TypeState.RoleTypeProperties>,
        undefineFn: (TypeState.Role) -> Unit
    ) {
        val tableHeight = Table.ROW_HEIGHT * (roleTypeProperties.size + 1).coerceAtLeast(2)
        Table.Layout(
            items = roleTypeProperties.sortedBy { it.roleType.scopedName },
            modifier = Modifier.fillMaxWidth().height(tableHeight),
            columns = listOf(
                Table.Column(header = Label.PLAYS, contentAlignment = Alignment.CenterStart) { props ->
                    ClickableText(props.roleType.scopedName) { GlobalState.resource.open(props.roleType.relationType) }
                },
                Table.Column(header = Label.OVERRIDDEN, contentAlignment = Alignment.CenterStart) { props ->
                    props.overriddenType?.let { ot ->
                        ClickableText(ot.scopedName) { GlobalState.resource.open(ot.relationType) }
                    }
                },
                Table.Column(header = Label.INHERITED, size = Either.second(ICON_COL_WIDTH)) {
                    MayTickIcon(it.isInherited)
                },
                Table.Column(header = null, size = Either.second(ICON_COL_WIDTH)) {
                    MayRemoveButton(Label.UNDEFINE_PLAYS_ROLE_TYPE, it.isInherited) { undefineFn(it.roleType) }
                },
            )
        )
    }

    @Composable
    private fun PlaysRoleTypeAddition() {
        val baseFontColor = Theme.studio.onPrimary
        var roleType: TypeState.Role? by remember { mutableStateOf(null) }
        val roleTypeList = GlobalState.schema.rootRelationType?.subtypes
            ?.flatMap { it.relatesRoleTypes }
            ?.filter { !type.playsRoleTypes.contains(it) }
            ?.sortedBy { it.scopedName }
            ?: listOf()

        var overriddenType: TypeState.Role? by remember { mutableStateOf(null) }
        val overridableTypeList: List<TypeState.Role> = roleType?.supertypes
            ?.intersect(type.supertype!!.playsRoleTypes.toSet())
            ?.sortedBy { it.scopedName } ?: listOf()

        val isPlayable = isEditable && roleType != null
        val isOverridable = isEditable && overridableTypeList.isNotEmpty()

        SectionLine {
            Box(Modifier.weight(1f)) {
                Form.Dropdown(
                    selected = roleType,
                    placeholder = Label.SELECT_ROLE_TYPE,
                    onExpand = { GlobalState.schema.rootRelationType?.loadRelatesRoleTypeRecursively() },
                    onSelection = { roleType = it; it.loadProperties() },
                    displayFn = { displayName(it, baseFontColor) },
                    modifier = Modifier.fillMaxSize(),
                    enabled = isEditable,
                    values = roleTypeList
                )
            }
            Form.Text(value = Label.AS.lowercase(), enabled = isOverridable)
            Box(Modifier.weight(1f)) {
                Form.Dropdown(
                    selected = overriddenType,
                    placeholder = Label.SELECT_OVERRIDDEN_TYPE_OPTIONAL,
                    onSelection = { overriddenType = it },
                    displayFn = { displayName(it, baseFontColor) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isOverridable,
                    values = overridableTypeList
                )
            }
            Form.TextButton(
                text = Label.PLAYS,
                leadingIcon = Form.IconArg(Icon.Code.PLUS) { Theme.studio.secondary },
                enabled = isPlayable,
                tooltip = Tooltip.Arg(Label.DEFINE_PLAYS_ROLE_TYPE, Sentence.EDITING_TYPES_REQUIREMENT_DESCRIPTION),
                onClick = { type.definePlaysRoleType(roleType!!, overriddenType) }
            )
        }
    }

    @Composable
    protected fun SubtypesSection() {
        SectionLine { Form.Text(value = Label.SUBTYPES) }
        Navigator.Layout(
            state = subtypesNavState,
            modifier = Modifier.fillMaxWidth()
                .height((Navigator.ITEM_HEIGHT * type.subtypes.size).coerceAtLeast(EMPTY_BOX_HEIGHT))
                .border(1.dp, Theme.studio.border)
                .background(Theme.studio.background1),
            itemHeight = if (type.subtypes.size > 1) Navigator.ITEM_HEIGHT else EMPTY_BOX_HEIGHT,
            bottomSpace = 0.dp,
            iconArg = { typeIcon(it.item) }
        )
    }

    @Composable
    private fun ButtonsSection() {
        SectionLine {
            Spacer(Modifier.weight(1f))
            DeleteButton()
            ExportButton()
            RefreshButton()
        }
    }

    @Composable
    private fun DeleteButton() {
        Form.TextButton(
            text = Label.DELETE,
            textColor = Theme.studio.error,
            leadingIcon = Form.IconArg(Icon.Code.TRASH_CAN) { Theme.studio.error },
            enabled = isEditable,
            tooltip = Tooltip.Arg(Label.DELETE, Sentence.EDITING_TYPES_REQUIREMENT_DESCRIPTION)
        ) { } // TODO
    }

    @Composable
    private fun ExportButton() {
        Form.TextButton(
            text = Label.EXPORT,
            leadingIcon = Form.IconArg(Icon.Code.ARROW_UP_RIGHT_FROM_SQUARE),
            enabled = GlobalState.project.current != null,
            tooltip = Tooltip.Arg(Label.EXPORT_SYNTAX)
        ) {
            type.exportSyntax { syntax ->
                GlobalState.project.tryCreateUntitledFile()?.let { file ->
                    file.content(syntax)
                    GlobalState.resource.open(file)
                }
            }
        }
    }

    @Composable
    private fun RefreshButton() {
        Form.TextButton(
            text = Label.REFRESH,
            leadingIcon = Form.IconArg(Icon.Code.ROTATE),
            tooltip = Tooltip.Arg(Label.REFRESH)
        ) { type.loadProperties() }
    }

    @Composable
    protected fun displayName(type: TypeState): AnnotatedString = displayName(type, Theme.studio.onPrimary)

    protected fun displayName(type: TypeState, baseFontColor: Color): AnnotatedString {
        return buildAnnotatedString {
            when (type) {
                is TypeState.Role -> append(type.scopedName)
                is TypeState.Thing -> append(type.name)
            }
            if (type is TypeState.Attribute) type.valueType?.let { valueType ->
                append(" ")
                withStyle(SpanStyle(baseFontColor.copy(FADED_OPACITY))) { append("(${valueType})") }
            }
        }
    }

    @Composable
    protected fun MayRemoveButton(tooltip: String, isVisible: Boolean, onClick: () -> Unit) {
        if (!isVisible) Form.IconButton(
            icon = Icon.Code.MINUS,
            modifier = Modifier.size(TABLE_BUTTON_HEIGHT),
            iconColor = Theme.studio.error,
            enabled = isEditable,
            tooltip = Tooltip.Arg(tooltip, Sentence.EDITING_TYPES_REQUIREMENT_DESCRIPTION),
            onClick = onClick
        )
    }

    @Composable
    protected fun MayTickIcon(boolean: Boolean) {
        if (boolean) Icon.Render(icon = Icon.Code.CHECK, color = Theme.studio.secondary)
    }

    @Composable
    protected fun EditButton(onClick: () -> Unit) {
        Form.IconButton(
            icon = Icon.Code.PEN,
            enabled = isEditable,
            tooltip = Tooltip.Arg(Label.RENAME, Sentence.EDITING_TYPES_REQUIREMENT_DESCRIPTION),
            onClick = onClick
        )
    }

    class Entity(
        override var type: TypeState.Entity, coroutineScope: CoroutineScope
    ) : TypePage(type, coroutineScope) {

        override fun updateResourceInner(resource: Resource) {
            type = resource as TypeState.Entity
        }

        @Composable
        override fun MainSections() {
            OwnsAttributeTypesSection()
            Separator()
            PlaysRoleTypesSection()
            Separator()
            SubtypesSection()
        }
    }

    class Relation(
        override var type: TypeState.Relation, coroutineScope: CoroutineScope
    ) : TypePage(type, coroutineScope) {

        override fun updateResourceInner(resource: Resource) {
            type = resource as TypeState.Relation
        }

        @Composable
        override fun MainSections() {
            RelatesRoleTypesSection()
            Separator()
            OwnsAttributeTypesSection()
            Separator()
            SubtypesSection()
            AdvanceSections {
                PlaysRoleTypesSection()
            }
        }

        @Composable
        private fun RelatesRoleTypesSection() {
            SectionLine { Form.Text(value = Label.RELATES_ROLE_TYPES) }
            RoleTypesTable(type.relatesRoleTypeProperties) { type.undefineRelatesRoleType(it) }
            RelatesRoleTypeAddition()
        }

        @Composable
        private fun RelatesRoleTypeAddition() {
            val baseFontColor = Theme.studio.onPrimary
            var roleType: String by remember { mutableStateOf("") }
            var overriddenType: TypeState.Role? by remember { mutableStateOf(null) }
            val overridableTypeList = type.supertype?.relatesRoleTypes
                ?.filter { GlobalState.schema.rootRelationType?.relatesRoleTypes?.contains(it) != true }
                ?.sortedBy { it.scopedName } ?: listOf()

            val isRelatable = isEditable && roleType.isNotEmpty()
            val isOverridable = isEditable && overridableTypeList.isNotEmpty()

            SectionLine {
                Form.TextInput(
                    value = roleType,
                    placeholder = Label.ROLE.lowercase(),
                    modifier = Modifier.weight(1f),
                    onValueChange = { roleType = it },
                    enabled = isEditable,
                )
                Form.Text(value = Label.AS.lowercase(), enabled = isOverridable)
                Box(Modifier.weight(1f)) {
                    Form.Dropdown(
                        selected = overriddenType,
                        placeholder = Label.SELECT_OVERRIDDEN_TYPE_OPTIONAL,
                        onSelection = { overriddenType = it },
                        displayFn = { displayName(it, baseFontColor) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isOverridable,
                        values = overridableTypeList
                    )
                }
                Form.TextButton(
                    text = Label.RELATES,
                    leadingIcon = Form.IconArg(Icon.Code.PLUS) { Theme.studio.secondary },
                    enabled = isRelatable,
                    tooltip = Tooltip.Arg(
                        Label.DEFINE_RELATES_ROLE_TYPE,
                        Sentence.EDITING_TYPES_REQUIREMENT_DESCRIPTION
                    ),
                    onClick = { type.defineRelatesRoleType(roleType, overriddenType) }
                )
            }
        }
    }

    class Attribute(
        override var type: TypeState.Attribute, coroutineScope: CoroutineScope
    ) : TypePage(type, coroutineScope) {

        override fun updateResourceInner(resource: Resource) {
            type = resource as TypeState.Attribute
        }

        @Composable
        override fun MainSections() {
            OwnersSection()
            Separator()
            SubtypesSection()
            AdvanceSections {
                OwnsAttributeTypesSection()
                Separator()
                PlaysRoleTypesSection()
            }
        }

        @Composable
        private fun OwnersSection() {
            SectionLine { Form.Text(value = Label.OWNER_TYPES) }
            val tableHeight = Table.ROW_HEIGHT * (type.ownerTypes.size + 1).coerceAtLeast(2)
            Table.Layout(
                items = type.ownerTypeProperties.values.sortedBy { it.ownerType.name },
                modifier = Modifier.fillMaxWidth().height(tableHeight),
                columns = listOf(
                    Table.Column(header = Label.OWNER, contentAlignment = Alignment.CenterStart) { props ->
                        ClickableText(displayName(props.ownerType)) { GlobalState.resource.open(props.ownerType) }
                    },
                    Table.Column(header = Label.KEY, size = Either.second(ICON_COL_WIDTH)) {
                        MayTickIcon(it.isKey)
                    },
                    Table.Column(header = Label.INHERITED, size = Either.second(ICON_COL_WIDTH)) {
                        MayTickIcon(it.isInherited)
                    }
                )
            )
        }
    }
}
