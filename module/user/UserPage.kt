/*
 * Copyright (C) 2022 Vaticle
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

package com.vaticle.typedb.studio.module.user

//import androidx.compose.foundation.ScrollState
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.focusable
//import androidx.compose.foundation.horizontalScroll
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.ColumnScope
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.RowScope
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.rememberScrollbarAdapter
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.focus.FocusRequester
//import androidx.compose.ui.focus.focusRequester
//import androidx.compose.ui.layout.onGloballyPositioned
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.unit.Dp
//import androidx.compose.ui.unit.dp
//import com.vaticle.typedb.client.api.concept.type.AttributeType
//import com.vaticle.typedb.client.api.concept.type.EntityType
//import com.vaticle.typedb.client.api.concept.type.RelationType
//import com.vaticle.typedb.client.api.concept.type.ThingType
//import com.vaticle.typedb.common.collection.Either
//import com.vaticle.typedb.studio.framework.common.Util.hyphenate
//import com.vaticle.typedb.studio.framework.common.Util.toDP
//import com.vaticle.typedb.studio.framework.common.theme.Color.FADED_OPACITY
//import com.vaticle.typedb.studio.framework.common.theme.Theme
//import com.vaticle.typedb.studio.framework.material.Concept.ConceptDetailedLabel
//import com.vaticle.typedb.studio.framework.material.Concept.conceptIcon
//import com.vaticle.typedb.studio.framework.material.Form
//import com.vaticle.typedb.studio.framework.material.Form.ClickableText
//import com.vaticle.typedb.studio.framework.material.Icon
//import com.vaticle.typedb.studio.framework.material.Navigator
//import com.vaticle.typedb.studio.framework.material.Pages
//import com.vaticle.typedb.studio.framework.material.Scrollbar
//import com.vaticle.typedb.studio.framework.material.Separator
//import com.vaticle.typedb.studio.framework.material.Table
//import com.vaticle.typedb.studio.framework.material.Tooltip
//import com.vaticle.typedb.studio.state.StudioState
//import com.vaticle.typedb.studio.state.common.util.Label
//import com.vaticle.typedb.studio.state.common.util.Sentence
//import com.vaticle.typedb.studio.state.page.Pageable
//import com.vaticle.typedb.studio.state.schema.TypeState
//import kotlinx.coroutines.CoroutineScope
//
//sealed class UserPage constructor(
//    val userState: UserState, showAdvanced: Boolean = false, coroutines: CoroutineScope
//) : Pages.Page() {
//
//    override val hasSecondary: Boolean = false
//    override val icon: Form.IconArg = conceptIcon(typeState.conceptType)
//
//    protected val isEditable
//        get() = !typeState.isRoot && StudioState.schema.isWritable &&
//                !StudioState.schema.hasRunningWrite && !StudioState.client.hasRunningCommand
//
//    private val focusReq = FocusRequester()
//    private val horScroller = ScrollState(0)
//    private val verScroller = ScrollState(0)
//    private var width: Dp by mutableStateOf(0.dp)
//    private var showAdvanced by mutableStateOf(showAdvanced)
//    private val subtypesNavState = Navigator.NavigatorState(
//        container = typeState,
//        title = Label.SUBTYPES_OF + " " + typeState.name,
//        mode = Navigator.Mode.LIST,
//        initExpandDepth = 4,
//        coroutines = coroutines
//    ) { it.item.tryOpen() }.also { typeState.onSubtypesUpdated { it.reloadEntries() } }
//
//    companion object {
//        private val MIN_WIDTH = 600.dp
//        private val MAX_WIDTH = 900.dp
//        private val PAGE_PADDING = 40.dp
//        private val LINE_SPACING = 8.dp
//        private val LINE_END_PADDING = 4.dp
//        private val VERTICAL_SPACING = 20.dp
//        private val ICON_COL_WIDTH = 80.dp
//        private val TABLE_BUTTON_HEIGHT = 24.dp
//        private val TABLE_ROW_HEIGHT = 40.dp
//        private val EMPTY_BOX_HEIGHT = Table.ROW_HEIGHT
//        private const val MAX_VISIBLE_SUBTYPES = 10
//
//        @Composable
//        fun create(type: TypeState.Thing<*, *>): TypePage<*, *> {
//            val coroutines = rememberCoroutineScope()
//            return when (type) {
//                is TypeState.Entity -> Entity(type, coroutines)
//                is TypeState.Relation -> Relation(type, coroutines)
//                is TypeState.Attribute -> Attribute(type, coroutines)
//            }
//        }
//    }
//
//    override fun updatePageable(pageable: Pageable) {
//        typeState = pageable as TS
//    }
//
//    @Composable
//    abstract fun MainSections()
//
//    @Composable
//    override fun PrimaryContent() {
//        val density = LocalDensity.current.density
//        val bgColor = Theme.studio.backgroundDark
//        Box(Modifier.background(bgColor).focusRequester(focusReq).focusable()
//            .onGloballyPositioned { width = toDP(it.size.width, density) }) {
//            Box(Modifier.fillMaxSize().horizontalScroll(horScroller).verticalScroll(verScroller), Alignment.TopCenter) {
//                SectionColumn(Modifier.padding(PAGE_PADDING)) { TypeSections() }
//            }
//            Scrollbar.Vertical(rememberScrollbarAdapter(verScroller), Modifier.align(Alignment.CenterEnd))
//            Scrollbar.Horizontal(rememberScrollbarAdapter(horScroller), Modifier.align(Alignment.BottomCenter))
//        }
//        LaunchedEffect(focusReq) { focusReq.requestFocus() }
//    }
//
//    @Composable
//    private fun SectionColumn(modifier: Modifier = Modifier, column: @Composable ColumnScope.() -> Unit) {
//        Column(
//            modifier = modifier.width(width.coerceIn(MIN_WIDTH, MAX_WIDTH)),
//            verticalArrangement = Arrangement.spacedBy(VERTICAL_SPACING)
//        ) { column() }
//    }
//
//    @Composable
//    protected fun SectionRow(modifier: Modifier = Modifier, row: @Composable RowScope.() -> Unit) {
//        Row(modifier.fillMaxWidth(), Arrangement.spacedBy(LINE_SPACING), Alignment.CenterVertically) {
//            Spacer(Modifier.width(LINE_END_PADDING))
//            row()
//            Spacer(Modifier.width(LINE_END_PADDING))
//        }
//    }
//
//    @Composable
//    protected fun Separator(highlight: Boolean = false) {
//        Separator.Horizontal(
//            modifier = Modifier.fillMaxWidth(),
//            color = when {
//                !highlight -> Theme.studio.border.copy(alpha = FADED_OPACITY)
//                else -> Theme.studio.warningStroke.copy(alpha = FADED_OPACITY / 3)
//            }
//        )
//    }
//
//    @Composable
//    private fun mayHighlightBackgroundModifier(highlight: Boolean) = when {
//        highlight -> Modifier.background(Theme.studio.warningBackground.copy(alpha = FADED_OPACITY / 5))
//        else -> Modifier
//    }
//
//    @Composable
//    protected fun AdvancedSections(sections: @Composable (separator: @Composable () -> Unit) -> Unit) {
//        SectionColumn(mayHighlightBackgroundModifier(showAdvanced)) {
//            Separator(showAdvanced)
//            SectionRow {
//                Form.Text(value = Label.ADVANCED)
//                Spacer(Modifier.weight(1f))
//                Form.IconButton(
//                    icon = if (showAdvanced) Icon.PREVIOUS_UP else Icon.NEXT_DOWN
//                ) { showAdvanced = !showAdvanced }
//            }
//            if (showAdvanced) {
//                Separator(showAdvanced)
//                sections { Separator(showAdvanced) }
//            }
//            Separator(showAdvanced)
//        }
//    }
//
//    @Composable
//    private fun TypeSections() {
//        LabelSection()
//        Separator()
//        SupertypeSection()
//        AbstractSection()
//        MainSections()
//        ButtonsSection()
//    }
//
//    @Composable
//    private fun LabelSection() {
//        SectionRow {
//            Form.TextBox(
//                text = ConceptDetailedLabel(typeState.conceptType),
//                leadingIcon = conceptIcon(typeState.conceptType)
//            )
//            EditButton { typeState.initiateRename() }
//            Spacer(Modifier.weight(1f))
//        }
//    }
//
//    @Composable
//    private fun SupertypeSection() {
//        val supertypeState = typeState.supertype ?: typeState
//        SectionRow {
//            Form.Text(value = Label.SUPERTYPE)
//            Spacer(Modifier.weight(1f))
//            Form.TextButton(
//                text = ConceptDetailedLabel(supertypeState.conceptType),
//                leadingIcon = conceptIcon(supertypeState.conceptType),
//                enabled = !typeState.isRoot,
//            ) { supertypeState.tryOpen() }
//            EditButton { typeState.initiateChangeSupertype() }
//        }
//    }
//
//    @Composable
//    private fun AbstractSection() {
//        SectionColumn(mayHighlightBackgroundModifier(typeState.isAbstract)) {
//            Separator(typeState.isAbstract)
//            SectionRow {
//                Form.Text(value = Label.ABSTRACT)
//                Spacer(Modifier.weight(1f))
//                Form.TextBox(((if (typeState.isAbstract) "" else Label.NOT + " ") + Label.ABSTRACT).lowercase())
//                EditButton(typeState.canBeAbstract) { typeState.initiateChangeAbstract() }
//            }
//            Separator(typeState.isAbstract)
//        }
//    }
//
//    @Composable
//    protected fun OwnsAttributeTypesSection() {
//        SectionRow { Form.Text(value = Label.OWNS) }
//        OwnsAttributeTypesTable()
//        OwnsAttributeTypeAddition()
//    }
//
//    @Composable
//    private fun OwnsAttributeTypesTable() {
//        val tableHeight = TABLE_ROW_HEIGHT * (typeState.ownsAttributeTypes.size + 1).coerceAtLeast(2)
//        SectionRow {
//            Table.Layout(
//                items = typeState.ownsAttributeTypeProperties.sortedBy { it.attributeType.name },
//                modifier = Modifier.border(1.dp, Theme.studio.border).weight(1f).height(tableHeight),
//                rowHeight = TABLE_ROW_HEIGHT,
//                columns = listOf(
//                    Table.Column(header = Label.ATTRIBUTE_TYPES, contentAlignment = Alignment.CenterStart) { props ->
//                        ClickableText(ConceptDetailedLabel(props.attributeType.conceptType)) {
//                            props.attributeType.tryOpen()
//                        }
//                    },
//                    Table.Column(header = Label.OVERRIDDEN, contentAlignment = Alignment.CenterStart) { props ->
//                        props.overriddenType?.let { ot ->
//                            ClickableText(ConceptDetailedLabel(ot.conceptType)) { ot.tryOpen() }
//                        }
//                    },
//                    Table.Column(header = Label.KEY, size = Either.second(ICON_COL_WIDTH)) { MayTickIcon(it.isKey) },
//                    Table.Column(header = Label.INHERITED, size = Either.second(ICON_COL_WIDTH)) {
//                        MayTickIcon(it.isInherited)
//                    },
//                    Table.Column(header = null, size = Either.second(ICON_COL_WIDTH)) {
//                        MayRemoveIconButton(Label.REMOVE_OWNS_ATTRIBUTE_TYPE, it.isInherited, it.canBeUndefined) {
//                            typeState.initiateRemoveOwnsAttributeType(it.attributeType)
//                        }
//                    },
//                )
//            )
//        }
//    }
//
//    @Composable
//    private fun OwnsAttributeTypeAddition() {
//        var attributeType: TypeState.Attribute? by remember { mutableStateOf(null) }
//        val attributeTypeList = StudioState.schema.rootAttributeType?.subtypes
//            ?.filter { !typeState.ownsAttributeTypes.contains(it) }
//            ?.sortedBy { it.name }
//            ?: listOf()
//
//        var overriddenType: TypeState.Attribute? by remember { mutableStateOf(null) }
//        val overridableTypeList: List<TypeState.Attribute> = attributeType?.supertypes
//            ?.intersect(typeState.supertype!!.ownsAttributeTypes.toSet())
//            ?.sortedBy { it.name } ?: listOf()
//
//        val isOwnable = isEditable && attributeType != null
//        val isOverridable = isEditable && overridableTypeList.isNotEmpty()
//        val isKeyable = isEditable && attributeType?.isKeyable == true
//        var isKey: Boolean by remember { mutableStateOf(false) }
//
//        SectionRow {
//            Box(Modifier.weight(1f)) {
//                Form.Dropdown(
//                    selected = attributeType,
//                    placeholder = Label.ATTRIBUTE_TYPE.lowercase().hyphenate(),
//                    onExpand = { StudioState.schema.rootAttributeType?.loadSubtypesRecursivelyAsync() },
//                    onSelection = { attributeType = it; it.loadSupertypes() },
//                    displayFn = { ConceptDetailedLabel(it.conceptType) },
//                    modifier = Modifier.fillMaxSize(),
//                    enabled = isEditable,
//                    values = attributeTypeList
//                )
//            }
//            Form.Text(value = Label.AS.lowercase(), enabled = isOverridable)
//            Box(Modifier.weight(1f)) {
//                Form.Dropdown(
//                    selected = overriddenType,
//                    placeholder = (Label.OVERRIDDEN_TYPE.hyphenate() + " (" + Label.OPTIONAL + ")").lowercase(),
//                    onSelection = { overriddenType = it },
//                    displayFn = { ConceptDetailedLabel(it.conceptType) },
//                    modifier = Modifier.fillMaxWidth(),
//                    enabled = isOverridable,
//                    values = overridableTypeList
//                )
//            }
//            Form.Text(value = Label.KEY.lowercase(), enabled = isKeyable)
//            Form.Checkbox(
//                value = isKey,
//                enabled = isKeyable
//            ) { isKey = it }
//            Form.TextButton(
//                text = Label.OWNS,
//                leadingIcon = Form.IconArg(Icon.ADD_OWNS) { Theme.studio.secondary },
//                enabled = isOwnable,
//                tooltip = Tooltip.Arg(Label.DEFINE_OWNS_ATTRIBUTE_TYPE, Sentence.EDITING_TYPES_REQUIREMENT_DESCRIPTION),
//                onClick = { typeState.tryDefineOwnsAttributeType(attributeType!!, overriddenType, isKey) }
//            )
//        }
//    }
//
//    @Composable
//    protected fun PlaysRoleTypesSection() {
//        SectionRow { Form.Text(value = Label.PLAYS) }
//        RoleTypesTable(typeState.playsRoleTypeProperties) {
//            MayRemoveIconButton(Label.REMOVE_PLAYS_ROLE_TYPE, it.isInherited, it.canBeUndefined) {
//                typeState.initiateRemovePlaysRoleType(it.roleType)
//            }
//        }
//        PlaysRoleTypeAddition()
//    }
//
//    @Composable
//    protected fun RoleTypesTable(
//        roleTypeProperties: List<TypeState.RoleTypeProperties>,
//        buttonsFn: @Composable RowScope.(TypeState.RoleTypeProperties) -> Unit
//    ) {
//        val tableHeight = TABLE_ROW_HEIGHT * (roleTypeProperties.size + 1).coerceAtLeast(2)
//        SectionRow {
//            Table.Layout(
//                items = roleTypeProperties.sortedBy { it.roleType.scopedName },
//                modifier = Modifier.border(1.dp, Theme.studio.border).weight(1f).height(tableHeight),
//                rowHeight = TABLE_ROW_HEIGHT,
//                columns = listOf(
//                    Table.Column(header = Label.ROLE_TYPES, contentAlignment = Alignment.CenterStart) { props ->
//                        ClickableText(props.roleType.scopedName) { props.roleType.relationType.tryOpen() }
//                    },
//                    Table.Column(header = Label.OVERRIDDEN, contentAlignment = Alignment.CenterStart) { props ->
//                        props.overriddenType?.let { ot -> ClickableText(ot.scopedName) { ot.relationType.tryOpen() } }
//                    },
//                    Table.Column(header = Label.INHERITED, size = Either.second(ICON_COL_WIDTH)) {
//                        MayTickIcon(it.isInherited)
//                    },
//                    Table.Column(header = null, size = Either.second(ICON_COL_WIDTH)) {
//                        Row(horizontalArrangement = Arrangement.spacedBy(LINE_SPACING)) { buttonsFn(it) }
//                    },
//                )
//            )
//        }
//    }
//
//    @Composable
//    private fun PlaysRoleTypeAddition() {
//        var roleType: TypeState.Role? by remember { mutableStateOf(null) }
//        val roleTypeList = StudioState.schema.rootRelationType?.subtypes
//            ?.flatMap { it.relatesRoleTypes }
//            ?.filter { !typeState.playsRoleTypes.contains(it) }
//            ?.sortedBy { it.scopedName }
//            ?: listOf()
//
//        var overriddenType: TypeState.Role? by remember { mutableStateOf(null) }
//        val overridableTypeList: List<TypeState.Role> = roleType?.supertypes
//            ?.intersect(typeState.supertype!!.playsRoleTypes.toSet())
//            ?.sortedBy { it.scopedName } ?: listOf()
//
//        val isPlayable = isEditable && roleType != null
//        val isOverridable = isEditable && overridableTypeList.isNotEmpty()
//
//        SectionRow {
//            Box(Modifier.weight(1f)) {
//                Form.Dropdown(
//                    selected = roleType,
//                    placeholder = Label.ROLE_TYPE.lowercase().hyphenate(),
//                    onExpand = { StudioState.schema.rootRelationType?.loadRelatesRoleTypesRecursivelyAsync() },
//                    onSelection = { roleType = it; it.loadSupertypes() },
//                    displayFn = { ConceptDetailedLabel(it.conceptType) },
//                    modifier = Modifier.fillMaxSize(),
//                    enabled = isEditable,
//                    values = roleTypeList
//                )
//            }
//            Form.Text(value = Label.AS.lowercase(), enabled = isOverridable)
//            Box(Modifier.weight(1f)) {
//                Form.Dropdown(
//                    selected = overriddenType,
//                    placeholder = (Label.OVERRIDDEN_TYPE.hyphenate() + " (" + Label.OPTIONAL + ")").lowercase(),
//                    onSelection = { overriddenType = it },
//                    displayFn = { ConceptDetailedLabel(it.conceptType) },
//                    modifier = Modifier.fillMaxWidth(),
//                    enabled = isOverridable,
//                    values = overridableTypeList
//                )
//            }
//            Form.TextButton(
//                text = Label.PLAYS,
//                leadingIcon = Form.IconArg(Icon.ADD_PLAYS) { Theme.studio.secondary },
//                enabled = isPlayable,
//                tooltip = Tooltip.Arg(Label.DEFINE_PLAYS_ROLE_TYPE, Sentence.EDITING_TYPES_REQUIREMENT_DESCRIPTION),
//                onClick = { typeState.tryDefinePlaysRoleType(roleType!!, overriddenType) }
//            )
//        }
//    }
//
//    @Composable
//    protected fun SubtypesSection() {
//        val visibleSize = typeState.subtypes.size.coerceAtMost(MAX_VISIBLE_SUBTYPES)
//        SectionRow { Form.Text(value = Label.SUBTYPES) }
//        SectionRow {
//            Navigator.Layout(
//                state = subtypesNavState,
//                modifier = Modifier.weight(1f)
//                    .height((Navigator.ITEM_HEIGHT * visibleSize).coerceAtLeast(EMPTY_BOX_HEIGHT))
//                    .border(1.dp, Theme.studio.border)
//                    .background(Theme.studio.backgroundMedium),
//                itemHeight = if (typeState.subtypes.size > 1) Navigator.ITEM_HEIGHT else EMPTY_BOX_HEIGHT,
//                bottomSpace = 0.dp,
//                iconArg = { conceptIcon(it.item.conceptType) }
//            )
//        }
//        LaunchedEffect(subtypesNavState) { subtypesNavState.launch() }
//    }
//
//    @Composable
//    private fun ButtonsSection() {
//        SectionRow {
//            Spacer(Modifier.weight(1f))
//            DeleteButton()
//            ExportButton()
//            RefreshButton()
//        }
//    }
//
//    @Composable
//    private fun DeleteButton() {
//        Form.TextButton(
//            text = Label.DELETE,
//            textColor = Theme.studio.errorStroke,
//            leadingIcon = Form.IconArg(Icon.DELETE) { Theme.studio.errorStroke },
//            enabled = isEditable && typeState.canBeDeleted,
//            tooltip = Tooltip.Arg(Label.DELETE, Sentence.EDITING_TYPES_REQUIREMENT_DESCRIPTION)
//        ) { typeState.initiateDelete() }
//    }
//
//    @Composable
//    private fun ExportButton() {
//        Form.TextButton(
//            text = Label.EXPORT,
//            leadingIcon = Form.IconArg(Icon.EXPORT),
//            enabled = StudioState.project.current != null,
//            tooltip = Tooltip.Arg(Label.EXPORT_SYNTAX)
//        ) {
//            typeState.exportSyntaxAsync { syntax ->
//                StudioState.project.tryCreateUntitledFile()?.let { file ->
//                    file.content(syntax)
//                    file.tryOpen()
//                }
//            }
//        }
//    }
//
//    @Composable
//    private fun RefreshButton() {
//        Form.TextButton(
//            text = Label.REFRESH,
//            leadingIcon = Form.IconArg(Icon.REFRESH),
//            tooltip = Tooltip.Arg(Label.REFRESH)
//        ) {
//            StudioState.schema.closeReadTx()
//            typeState.loadConstraintsAsync()
//        }
//    }
//
//    @Composable
//    protected fun MayRemoveIconButton(
//        tooltip: String,
//        isVisible: Boolean,
//        enabled: Boolean = true,
//        onClick: () -> Unit
//    ) {
//        if (!isVisible) Form.IconButton(
//            icon = Icon.REMOVE,
//            iconColor = Theme.studio.errorStroke,
//            enabled = isEditable && enabled,
//            tooltip = Tooltip.Arg(tooltip, Sentence.EDITING_TYPES_REQUIREMENT_DESCRIPTION),
//            onClick = onClick
//        )
//    }
//
//    @Composable
//    protected fun MayDeleteIconButton(
//        tooltip: String,
//        isVisible: Boolean,
//        enabled: Boolean = true,
//        onClick: () -> Unit
//    ) {
//        if (!isVisible) Form.IconButton(
//            icon = Icon.DELETE,
//            iconColor = Theme.studio.errorStroke,
//            enabled = isEditable && enabled,
//            tooltip = Tooltip.Arg(tooltip, Sentence.EDITING_TYPES_REQUIREMENT_DESCRIPTION),
//            onClick = onClick
//        )
//    }
//
//    @Composable
//    protected fun MayTickIcon(boolean: Boolean) {
//        if (boolean) Icon.Render(icon = Icon.TICK, color = Theme.studio.secondary)
//    }
//
//    @Composable
//    protected fun EditButton(enabled: Boolean = true, onClick: () -> Unit) {
//        Form.IconButton(
//            icon = Icon.RENAME,
//            enabled = isEditable && enabled,
//            tooltip = Tooltip.Arg(Label.RENAME, Sentence.EDITING_TYPES_REQUIREMENT_DESCRIPTION),
//            onClick = onClick
//        )
//    }
//
//    class Entity constructor(
//        typeState: TypeState.Entity, coroutines: CoroutineScope
//    ) : TypePage<EntityType, TypeState.Entity>(typeState, false, coroutines) {
//
//        @Composable
//        override fun MainSections() {
//            OwnsAttributeTypesSection()
//            Separator()
//            PlaysRoleTypesSection()
//            Separator()
//            SubtypesSection()
//            Separator()
//        }
//    }
//
//    class Relation constructor(
//        typeState: TypeState.Relation, coroutines: CoroutineScope
//    ) : TypePage<RelationType, TypeState.Relation>(typeState, typeState.playsRoleTypes.isNotEmpty(), coroutines) {
//
//        @Composable
//        override fun MainSections() {
//            RelatesRoleTypesSection()
//            Separator()
//            OwnsAttributeTypesSection()
//            Separator()
//            SubtypesSection()
//            AdvancedSections {
//                PlaysRoleTypesSection()
//            }
//        }
//
//        @Composable
//        private fun RelatesRoleTypesSection() {
//            SectionRow { Form.Text(value = Label.RELATES) }
//            RoleTypesTable(typeState.relatesRoleTypeProperties) {
//                EditButton { it.roleType.initiateRename() }
//                MayDeleteIconButton(Label.DELETE_ROLE_TYPE, it.isInherited, it.canBeUndefined) {
//                    typeState.initiateDeleteRoleType(it.roleType)
//                }
//            }
//            RelatesRoleTypeAddition()
//        }
//
//        @Composable
//        private fun RelatesRoleTypeAddition() {
//            var roleType: String by remember { mutableStateOf("") }
//            var overriddenType: TypeState.Role? by remember { mutableStateOf(null) }
//            val overridableTypeList = typeState.supertype?.relatesRoleTypes
//                ?.filter { StudioState.schema.rootRelationType?.relatesRoleTypes?.contains(it) != true }
//                ?.sortedBy { it.scopedName } ?: listOf()
//
//            val isRelatable = isEditable && roleType.isNotEmpty()
//            val isOverridable = isEditable && overridableTypeList.isNotEmpty()
//
//            SectionRow {
//                Form.TextInput(
//                    value = roleType,
//                    placeholder = Label.ROLE.lowercase(),
//                    modifier = Modifier.weight(1f),
//                    onValueChange = { roleType = it },
//                    enabled = isEditable,
//                )
//                Form.Text(value = Label.AS.lowercase(), enabled = isOverridable)
//                Box(Modifier.weight(1f)) {
//                    Form.Dropdown(
//                        selected = overriddenType,
//                        placeholder = (Label.OVERRIDDEN_TYPE.hyphenate() + " (" + Label.OPTIONAL + ")").lowercase(),
//                        onSelection = { overriddenType = it },
//                        displayFn = { ConceptDetailedLabel(it.conceptType) },
//                        modifier = Modifier.fillMaxWidth(),
//                        enabled = isOverridable,
//                        values = overridableTypeList
//                    )
//                }
//                Form.TextButton(
//                    text = Label.RELATES,
//                    leadingIcon = Form.IconArg(Icon.ADD_RELATES) { Theme.studio.secondary },
//                    enabled = isRelatable,
//                    tooltip = Tooltip.Arg(
//                        Label.DEFINE_RELATES_ROLE_TYPE,
//                        Sentence.EDITING_TYPES_REQUIREMENT_DESCRIPTION
//                    ),
//                    onClick = { typeState.tryDefineRelatesRoleType(roleType, overriddenType) }
//                )
//            }
//        }
//    }
//
//    class Attribute constructor(
//        typeState: TypeState.Attribute, coroutines: CoroutineScope
//    ) : TypePage<AttributeType, TypeState.Attribute>(
//        typeState = typeState,
//        showAdvanced = typeState.ownsAttributeTypes.isNotEmpty() || typeState.playsRoleTypes.isNotEmpty(),
//        coroutines = coroutines
//    ) {
//
//        @Composable
//        override fun MainSections() {
//            OwnersSection()
//            Separator()
//            SubtypesSection()
//            Separator()
//        }
//
//        @Composable
//        private fun OwnersSection() {
//            val tableHeight = TABLE_ROW_HEIGHT * (typeState.ownerTypes.size + 1).coerceAtLeast(2)
//            SectionRow { Form.Text(value = Label.OWNERS) }
//            SectionRow {
//                Table.Layout(
//                    items = typeState.ownerTypeProperties.values.sortedBy { it.ownerType.name },
//                    modifier = Modifier.border(1.dp, Theme.studio.border).weight(1f).height(tableHeight),
//                    rowHeight = TABLE_ROW_HEIGHT,
//                    columns = listOf(
//                        Table.Column(header = Label.THING_TYPES, contentAlignment = Alignment.CenterStart) { props ->
//                            ClickableText(ConceptDetailedLabel(props.ownerType.conceptType)) { props.ownerType.tryOpen() }
//                        },
//                        Table.Column(header = Label.KEY, size = Either.second(ICON_COL_WIDTH)) {
//                            MayTickIcon(it.isKey)
//                        },
//                        Table.Column(header = Label.INHERITED, size = Either.second(ICON_COL_WIDTH)) {
//                            MayTickIcon(it.isInherited)
//                        }
//                    )
//                )
//            }
//        }
//    }
//}
