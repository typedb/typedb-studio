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

package com.vaticle.typedb.studio.view.type

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import com.vaticle.typedb.studio.state.common.util.Label
import com.vaticle.typedb.studio.state.common.util.Sentence
import com.vaticle.typedb.studio.state.resource.Resource
import com.vaticle.typedb.studio.state.schema.TypeState
import com.vaticle.typedb.studio.view.common.Util.toDP
import com.vaticle.typedb.studio.view.common.theme.Color.FADED_OPACITY
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.material.Form
import com.vaticle.typedb.studio.view.material.Form.ClickableText
import com.vaticle.typedb.studio.view.material.Icon
import com.vaticle.typedb.studio.view.material.Navigator
import com.vaticle.typedb.studio.view.material.Page
import com.vaticle.typedb.studio.view.material.Scrollbar
import com.vaticle.typedb.studio.view.material.Separator
import com.vaticle.typedb.studio.view.material.Table
import com.vaticle.typedb.studio.view.material.Tooltip
import kotlinx.coroutines.CoroutineScope

sealed class TypePage(
    type: TypeState.Thing,
    showAdvanced: Boolean = false,
    coroutineScope: CoroutineScope
) : Page() {

    override val hasSecondary: Boolean = false
    override val icon: Form.IconArg = typeIcon(type)
    protected abstract val type: TypeState.Thing

    private val focusReq = FocusRequester()
    private val horScroller = ScrollState(0)
    private val verScroller = ScrollState(0)
    private var width: Dp by mutableStateOf(0.dp)
    protected val isEditable get() = false // TODO: type.schemaMgr.hasWriteTx && !type.isRoot && !GlobalState.client.hasRunningCommand
    private var showAdvanced by mutableStateOf(showAdvanced)
    private var showEditLabelDialog by mutableStateOf(false)
    private var showEditSupertypeDialog by mutableStateOf(false)
    private var showEditAbstractDialog by mutableStateOf(false)
    private val subtypesNavState = Navigator.NavigatorState(
        container = type,
        title = Label.SUBTYPES_OF + " " + type.name,
        mode = Navigator.Mode.LIST,
        initExpandDepth = 4,
        coroutineScope = coroutineScope
    ) { GlobalState.resource.open(it.item) }

    companion object {
        private val MIN_WIDTH = 600.dp
        private val MAX_WIDTH = 900.dp
        private val PAGE_PADDING = 40.dp
        private val LINE_SPACING = 8.dp
        private val LINE_END_PADDING = 4.dp
        private val VERTICAL_SPACING = 20.dp
        private val ICON_COL_WIDTH = 80.dp
        private val TABLE_BUTTON_HEIGHT = 24.dp
        private val EMPTY_BOX_HEIGHT = Table.ROW_HEIGHT
        private const val MAX_VISIBLE_SUBTYPES = 10

        @Composable
        fun create(type: TypeState.Thing): TypePage {
            val coroutineScope = rememberCoroutineScope()
            return when (type) {
                is TypeState.Entity -> Entity(type, coroutineScope)
                is TypeState.Relation -> Relation(type, coroutineScope)
                is TypeState.Attribute -> Attribute(type, coroutineScope)
            }
        }

        // TODO: move these methods to a package about type visualisations
        internal fun typeIcon(type: TypeState.Thing) = when (type) {
            is TypeState.Entity -> Form.IconArg(Icon.Code.RECTANGLE) { Theme.graph.vertex.entityType }
            is TypeState.Relation -> Form.IconArg(Icon.Code.RHOMBUS) { Theme.graph.vertex.relationType }
            is TypeState.Attribute -> Form.IconArg(Icon.Code.OVAL) { Theme.graph.vertex.attributeType }
        }
    }

    @Composable
    abstract fun MainSections()

    @Composable
    override fun PrimaryContent() {
        val density = LocalDensity.current.density
        val bgColor = Theme.studio.backgroundDark
        Box(Modifier.background(bgColor).focusRequester(focusReq).focusable()
            .onGloballyPositioned { width = toDP(it.size.width, density) }) {
            Box(Modifier.fillMaxSize().horizontalScroll(horScroller).verticalScroll(verScroller), Alignment.TopCenter) {
                SectionColumn(Modifier.padding(PAGE_PADDING)) { TypeSections() }
            }
            Scrollbar.Vertical(rememberScrollbarAdapter(verScroller), Modifier.align(Alignment.CenterEnd))
            Scrollbar.Horizontal(rememberScrollbarAdapter(horScroller), Modifier.align(Alignment.BottomCenter))
        }
        if (showEditLabelDialog) EditLabelDialog()
        if (showEditSupertypeDialog) EditSupertypeDialog()
        if (showEditAbstractDialog) EditAbstractDialog()
        LaunchedEffect(focusReq) { focusReq.requestFocus() }
    }

    @Composable
    private fun EditLabelDialog() {

    }

    @Composable
    private fun EditSupertypeDialog() {

    }

    @Composable
    private fun EditAbstractDialog() {

    }

    @Composable
    private fun SectionColumn(modifier: Modifier = Modifier, column: @Composable ColumnScope.() -> Unit) {
        Column(
            modifier = modifier.width(width.coerceIn(MIN_WIDTH, MAX_WIDTH)),
            verticalArrangement = Arrangement.spacedBy(VERTICAL_SPACING)
        ) { column() }
    }

    @Composable
    protected fun SectionRow(modifier: Modifier = Modifier, row: @Composable RowScope.() -> Unit) {
        Row(modifier.fillMaxWidth(), Arrangement.spacedBy(LINE_SPACING), Alignment.CenterVertically) {
            Spacer(Modifier.width(LINE_END_PADDING))
            row()
            Spacer(Modifier.width(LINE_END_PADDING))
        }
    }

    @Composable
    protected fun Separator(color: Color = Theme.studio.border.copy(alpha = FADED_OPACITY)) {
        Separator.Horizontal(
            color = color,
            modifier = Modifier.fillMaxWidth()
        )
    }

    @Composable
    protected fun AdvancedSections(sections: @Composable (separator: @Composable () -> Unit) -> Unit) {
        val borderColor = if (!showAdvanced) Theme.studio.border.copy(alpha = FADED_OPACITY)
        else Theme.studio.warningStroke.copy(alpha = FADED_OPACITY / 3)
        val modifier = if (!showAdvanced) Modifier
        else Modifier.background(Theme.studio.warningBackground.copy(alpha = FADED_OPACITY / 5))
        SectionColumn(modifier) {
            Separator(borderColor)
            SectionRow {
                Form.Text(value = Label.ADVANCED)
                Spacer(Modifier.weight(1f))
                Form.IconButton(
                    icon = if (showAdvanced) Icon.Code.CHEVRON_UP else Icon.Code.CHEVRON_DOWN
                ) { showAdvanced = !showAdvanced }
            }
            if (showAdvanced) {
                Separator(borderColor)
                sections { Separator(borderColor) }
            }
            Separator(borderColor)
        }
    }

    @Composable
    private fun TypeSections() {
        LabelSection()
        Separator()
        SupertypeSection()
        Separator()
        AbstractSection()
        Separator()
        MainSections()
        ButtonsSection()
    }

    @Composable
    private fun LabelSection() {
        SectionRow {
            Form.TextBox(text = TypeDisplayName(type), leadingIcon = typeIcon(type))
            EditButton { } // TODO
            Spacer(Modifier.weight(1f))
        }
    }

    @Composable
    private fun SupertypeSection() {
        SectionRow {
            Form.Text(value = Label.SUPERTYPE)
            Spacer(Modifier.weight(1f))
            Form.TextButton(
                text = type.supertype?.let { TypeDisplayName(it) } ?: AnnotatedString("(${Label.THING.lowercase()})"),
                leadingIcon = type.supertype?.let { typeIcon(it) },
                enabled = !type.isRoot,
            ) { type.supertype?.let { GlobalState.resource.open(it) } }
            EditButton { } // TODO
        }
    }

    @Composable
    private fun AbstractSection() {
        SectionRow {
            Form.Text(value = Label.ABSTRACT)
            Spacer(Modifier.weight(1f))
            Form.TextBox(((if (type.isAbstract) "" else Label.NOT + " ") + Label.ABSTRACT).lowercase())
            EditButton { } // TODO
        }
    }

    @Composable
    protected fun OwnsAttributeTypesSection() {
        SectionRow { Form.Text(value = Label.OWNS) }
        OwnsAttributeTypesTable()
        OwnsAttributeTypeAddition()
    }

    @Composable
    private fun OwnsAttributeTypesTable() {
        val tableHeight = Table.ROW_HEIGHT * (type.ownsAttributeTypes.size + 1).coerceAtLeast(2)
        SectionRow {
            Table.Layout(
                items = type.ownsAttributeTypeProperties.sortedBy { it.attributeType.name },
                modifier = Modifier.weight(1f).height(tableHeight),
                columns = listOf(
                    Table.Column(header = Label.ATTRIBUTE_TYPES, contentAlignment = Alignment.CenterStart) { props ->
                        ClickableText(TypeDisplayName(props.attributeType)) { GlobalState.resource.open(props.attributeType) }
                    },
                    Table.Column(header = Label.OVERRIDDEN, contentAlignment = Alignment.CenterStart) { props ->
                        props.overriddenType?.let { ot ->
                            ClickableText(TypeDisplayName(ot)) {
                                GlobalState.resource.open(
                                    ot
                                )
                            }
                        }
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

        SectionRow {
            Box(Modifier.weight(1f)) {
                Form.Dropdown(
                    selected = attributeType,
                    placeholder = Label.SELECT_ATTRIBUTE_TYPE,
                    onExpand = { GlobalState.schema.rootAttributeType?.loadSubtypesRecursively() },
                    onSelection = { attributeType = it; it.loadProperties() },
                    displayFn = { TypeDisplayName(it, baseFontColor) },
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
                    displayFn = { TypeDisplayName(it, baseFontColor) },
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
        SectionRow { Form.Text(value = Label.PLAYS) }
        RoleTypesTable(type.playsRoleTypeProperties) { type.undefinePlaysRoleType(it) }
        PlaysRoleTypeAddition()
    }

    @Composable
    protected fun RoleTypesTable(
        roleTypeProperties: List<TypeState.RoleTypeProperties>,
        undefineFn: (TypeState.Role) -> Unit
    ) {
        val tableHeight = Table.ROW_HEIGHT * (roleTypeProperties.size + 1).coerceAtLeast(2)
        SectionRow {
            Table.Layout(
                items = roleTypeProperties.sortedBy { it.roleType.scopedName },
                modifier = Modifier.weight(1f).height(tableHeight),
                columns = listOf(
                    Table.Column(header = Label.ROLE_TYPES, contentAlignment = Alignment.CenterStart) { props ->
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

        SectionRow {
            Box(Modifier.weight(1f)) {
                Form.Dropdown(
                    selected = roleType,
                    placeholder = Label.SELECT_ROLE_TYPE,
                    onExpand = { GlobalState.schema.rootRelationType?.loadRelatesRoleTypeRecursively() },
                    onSelection = { roleType = it; it.loadProperties() },
                    displayFn = { TypeDisplayName(it, baseFontColor) },
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
                    displayFn = { TypeDisplayName(it, baseFontColor) },
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
        val visibleSize = type.subtypes.size.coerceAtMost(MAX_VISIBLE_SUBTYPES)
        SectionRow { Form.Text(value = Label.SUBTYPES) }
        SectionRow {
            Navigator.Layout(
                state = subtypesNavState,
                modifier = Modifier.weight(1f)
                    .height((Navigator.ITEM_HEIGHT * visibleSize).coerceAtLeast(EMPTY_BOX_HEIGHT))
                    .border(1.dp, Theme.studio.border)
                    .background(Theme.studio.backgroundMedium),
                itemHeight = if (type.subtypes.size > 1) Navigator.ITEM_HEIGHT else EMPTY_BOX_HEIGHT,
                bottomSpace = 0.dp,
                iconArg = { typeIcon(it.item) }
            )
        }
    }

    @Composable
    private fun ButtonsSection() {
        SectionRow {
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
            textColor = Theme.studio.errorStroke,
            leadingIcon = Form.IconArg(Icon.Code.TRASH_CAN) { Theme.studio.errorStroke },
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
    protected fun TypeDisplayName(type: TypeState): AnnotatedString = TypeDisplayName(type, Theme.studio.onPrimary)

    protected fun TypeDisplayName(type: TypeState, baseFontColor: Color): AnnotatedString {
        val primary = when (type) {
            is TypeState.Role -> type.scopedName
            is TypeState.Thing -> type.name
        }
        val secondary = if (type is TypeState.Attribute) type.valueType else null
        return AnnotatedString(primary, secondary, baseFontColor)
    }

    @Composable
    protected fun AnnotatedString(primary: String, secondary: String? = null): AnnotatedString {
        return AnnotatedString(primary, secondary, Theme.studio.onPrimary)
    }

    protected fun AnnotatedString(primary: String, secondary: String? = null, baseFontColor: Color): AnnotatedString {
        return buildAnnotatedString {
            append(primary)
            secondary?.let {
                append(" ")
                withStyle(SpanStyle(baseFontColor.copy(FADED_OPACITY))) { append("(${it})") }
            }
        }
    }

    @Composable
    protected fun MayRemoveButton(tooltip: String, isVisible: Boolean, onClick: () -> Unit) {
        if (!isVisible) Form.IconButton(
            icon = Icon.Code.MINUS,
            modifier = Modifier.size(TABLE_BUTTON_HEIGHT),
            iconColor = Theme.studio.errorStroke,
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
    ) : TypePage(type, false, coroutineScope) {

        override fun updateResource(resource: Resource) {
            type = resource as TypeState.Entity
        }

        @Composable
        override fun MainSections() {
            OwnsAttributeTypesSection()
            Separator()
            PlaysRoleTypesSection()
            Separator()
            SubtypesSection()
            Separator()
        }
    }

    class Relation(
        override var type: TypeState.Relation, coroutineScope: CoroutineScope
    ) : TypePage(type, type.playsRoleTypes.isNotEmpty(), coroutineScope) {

        override fun updateResource(resource: Resource) {
            type = resource as TypeState.Relation
        }

        @Composable
        override fun MainSections() {
            RelatesRoleTypesSection()
            Separator()
            OwnsAttributeTypesSection()
            Separator()
            SubtypesSection()
            AdvancedSections {
                PlaysRoleTypesSection()
            }
        }

        @Composable
        private fun RelatesRoleTypesSection() {
            SectionRow { Form.Text(value = Label.RELATES) }
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

            SectionRow {
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
                        displayFn = { TypeDisplayName(it, baseFontColor) },
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
    ) : TypePage(type, type.ownsAttributeTypes.isNotEmpty() || type.playsRoleTypes.isNotEmpty(), coroutineScope) {

        override fun updateResource(resource: Resource) {
            type = resource as TypeState.Attribute
        }

        @Composable
        override fun MainSections() {
            OwnersSection()
            Separator()
            SubtypesSection()
            Separator()
        }

        @Composable
        private fun OwnersSection() {
            val tableHeight = Table.ROW_HEIGHT * (type.ownerTypes.size + 1).coerceAtLeast(2)
            SectionRow { Form.Text(value = Label.OWNERS) }
            SectionRow {
                Table.Layout(
                    items = type.ownerTypeProperties.values.sortedBy { it.ownerType.name },
                    modifier = Modifier.weight(1f).height(tableHeight),
                    columns = listOf(
                        Table.Column(header = Label.THING_TYPES, contentAlignment = Alignment.CenterStart) { props ->
                            ClickableText(TypeDisplayName(props.ownerType)) { GlobalState.resource.open(props.ownerType) }
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
}
