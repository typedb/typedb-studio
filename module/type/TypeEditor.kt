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

package com.vaticle.typedb.studio.module.type

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
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.client.api.concept.type.AttributeType
import com.vaticle.typedb.client.api.concept.type.EntityType
import com.vaticle.typedb.client.api.concept.type.RelationType
import com.vaticle.typedb.client.api.concept.type.ThingType
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.framework.common.Util.hyphenate
import com.vaticle.typedb.studio.framework.common.Util.toDP
import com.vaticle.typedb.studio.framework.common.theme.Color.FADED_OPACITY
import com.vaticle.typedb.studio.framework.common.theme.Theme
import com.vaticle.typedb.studio.framework.material.ConceptDisplay.TypeLabelWithDetails
import com.vaticle.typedb.studio.framework.material.ConceptDisplay.iconOf
import com.vaticle.typedb.studio.framework.material.ContextMenu
import com.vaticle.typedb.studio.framework.material.Form
import com.vaticle.typedb.studio.framework.material.Form.ClickableText
import com.vaticle.typedb.studio.framework.material.Icon
import com.vaticle.typedb.studio.framework.material.Navigator
import com.vaticle.typedb.studio.framework.material.Pages
import com.vaticle.typedb.studio.framework.material.Scrollbar
import com.vaticle.typedb.studio.framework.material.Separator
import com.vaticle.typedb.studio.framework.material.Table
import com.vaticle.typedb.studio.framework.material.Tooltip
import com.vaticle.typedb.studio.service.Service
import com.vaticle.typedb.studio.service.common.util.Label
import com.vaticle.typedb.studio.service.common.util.Sentence
import com.vaticle.typedb.studio.service.page.Pageable
import com.vaticle.typedb.studio.service.schema.AttributeTypeState
import com.vaticle.typedb.studio.service.schema.EntityTypeState
import com.vaticle.typedb.studio.service.schema.RelationTypeState
import com.vaticle.typedb.studio.service.schema.RoleTypeState
import com.vaticle.typedb.studio.service.schema.ThingTypeState
import com.vaticle.typedb.studio.service.schema.TypeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

sealed class TypeEditor<T : ThingType, TS : ThingTypeState<T, TS>> constructor(
    var typeState: TS, showAdvanced: Boolean = false
) : Pages.Page() {

    override val hasSecondary: Boolean = false
    override val icon: Form.IconArg = iconOf(typeState.conceptType)

    internal val canReadSchema get() = !Service.schema.hasRunningCommand
    internal val canWriteSchema
        get() = !typeState.isRoot && Service.schema.isWritable &&
                !Service.schema.hasRunningCommand && !Service.client.hasRunningCommand

    private val focusReq = FocusRequester()
    private val horScroller = ScrollState(0)
    private val verScroller = ScrollState(0)
    private var width: Dp by mutableStateOf(0.dp)
    private var showAdvanced by mutableStateOf(showAdvanced)
    private var subtypesNavStateLaunched = false
    private val subtypesNavState = Navigator.NavigatorState(
        container = typeState,
        title = Label.SUBTYPES_OF + " " + typeState.name,
        behaviour = Navigator.Behaviour.Menu(),
        initExpandDepth = 1,
        coroutines = CoroutineScope(Dispatchers.Default)
    ) { it.item.tryOpen() }.also { typeState.onSubtypesUpdated { it.reloadEntriesAsync() } }

    internal val coroutines = CoroutineScope(Dispatchers.Default)

    companion object {
        private val MIN_WIDTH = 600.dp
        private val MAX_WIDTH = 900.dp
        private val PAGE_PADDING = 40.dp
        private val LINE_SPACING = 8.dp
        private val LINE_END_PADDING = 4.dp
        private val VERTICAL_SPACING = 20.dp
        private val ICON_COL_WIDTH = 80.dp
        private val TABLE_ROW_HEIGHT = 40.dp
        private val EMPTY_BOX_HEIGHT = Table.ROW_HEIGHT
        private const val MAX_VISIBLE_SUBTYPES = 10

        fun create(type: ThingTypeState<*, *>): TypeEditor<*, *> {
            return when (type) {
                is EntityTypeState -> Entity(type)
                is RelationTypeState -> Relation(type)
                is AttributeTypeState -> Attribute(type)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun updatePageable(pageable: Pageable) {
        typeState = pageable as TS
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
        LaunchedEffect(focusReq) { focusReq.requestFocus() }
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
    protected fun Separator(highlight: Boolean = false) {
        Separator.Horizontal(
            modifier = Modifier.fillMaxWidth(),
            color = when {
                !highlight -> Theme.studio.border.copy(alpha = FADED_OPACITY)
                else -> Theme.studio.warningStroke.copy(alpha = FADED_OPACITY / 3)
            }
        )
    }

    @Composable
    private fun mayHighlightBackgroundModifier(highlight: Boolean) = when {
        highlight -> Modifier.background(Theme.studio.warningBackground.copy(alpha = FADED_OPACITY / 5))
        else -> Modifier
    }

    @Composable
    protected fun TypeTableCellText(typeState: TypeState<*, *>, onClick: () -> Unit) = ClickableText(
        value = TypeLabelWithDetails(typeState.conceptType, typeState.isAbstract),
        onClick = onClick
    )

    @Composable
    protected fun AdvancedSections(sections: @Composable (separator: @Composable () -> Unit) -> Unit) {
        SectionColumn(mayHighlightBackgroundModifier(showAdvanced)) {
            Separator(showAdvanced)
            SectionRow {
                Form.Text(value = Label.ADVANCED)
                Spacer(Modifier.weight(1f))
                Form.IconButton(
                    icon = if (showAdvanced) Icon.PREVIOUS_UP else Icon.NEXT_DOWN
                ) { showAdvanced = !showAdvanced }
            }
            if (showAdvanced) {
                Separator(showAdvanced)
                sections { Separator(showAdvanced) }
            }
            Separator(showAdvanced)
        }
    }

    @Composable
    private fun TypeSections() {
        LabelSection()
        Separator()
        SupertypeSection()
        AbstractSection()
        MainSections()
        ButtonsSection()
    }

    @Composable
    private fun LabelSection() {
        SectionRow {
            Form.TextBox(
                text = TypeLabelWithDetails(typeState.conceptType),
                leadingIcon = iconOf(typeState.conceptType)
            )
            EditButton(Label.RENAME) { typeState.initiateRename() }
            Spacer(Modifier.weight(1f))
        }
    }

    @Composable
    private fun SupertypeSection() {
        val supertypeState = typeState.supertype ?: typeState
        SectionRow {
            Form.Text(value = Label.SUPERTYPE)
            Spacer(Modifier.weight(1f))
            Form.TextButton(
                text = TypeLabelWithDetails(supertypeState.conceptType, supertypeState.isAbstract),
                leadingIcon = iconOf(supertypeState.conceptType),
                enabled = !typeState.isRoot,
            ) { supertypeState.tryOpen() }
            EditButton(Label.CHANGE_SUPERTYPE) { typeState.initiateChangeSupertype() }
        }
    }

    @Composable
    private fun AbstractSection() {
        SectionColumn(mayHighlightBackgroundModifier(typeState.isAbstract)) {
            Separator(typeState.isAbstract)
            SectionRow {
                Form.Text(value = Label.ABSTRACT)
                Spacer(Modifier.weight(1f))
                Form.TextBox(((if (typeState.isAbstract) "" else Label.NOT + " ") + Label.ABSTRACT).lowercase())
                EditButton(Label.CHANGE_TYPE_ABSTRACTNESS, typeState.canBeAbstract) {
                    typeState.initiateChangeAbstract()
                }
            }
            Separator(typeState.isAbstract)
        }
    }

    @Composable
    protected fun OwnedAttributeTypesSection() {
        SectionRow { Form.Text(value = Label.OWNS) }
        OwnedAttributeTypesTable()
        DefineOwnsAttributeType()
    }

    @Composable
    private fun OwnedAttributeTypesTable() {
        val tableHeight = TABLE_ROW_HEIGHT * (typeState.ownedAttTypes.size + 1).coerceAtLeast(2)
        SectionRow {
            Table.Layout(
                items = typeState.ownedAttTypeProperties.sortedBy { it.attributeType.name },
                modifier = Modifier.weight(1f).height(tableHeight).border(1.dp, Theme.studio.border),
                rowHeight = TABLE_ROW_HEIGHT,
                contextMenuFn = { ownedAttributeTypesContextMenu(it) },
                columns = listOf(
                    Table.Column(header = Label.ATTRIBUTE_TYPE, contentAlignment = Alignment.CenterStart) { props ->
                        TypeTableCellText(props.attributeType) { props.attributeType.tryOpen() }
                    },
                    Table.Column(header = Label.OVERRIDDEN_TYPE, contentAlignment = Alignment.CenterStart) { props ->
                        props.overriddenType?.let { ot -> TypeTableCellText(ot) { ot.tryOpen() } }
                    },
                    Table.Column(header = Label.EXTENDED_TYPE, contentAlignment = Alignment.CenterStart) { props ->
                        props.extendedType?.let { ot -> TypeTableCellText(ot) { ot.tryOpen() } }
                    },
                    Table.Column(header = Label.INHERITED, size = Either.second(ICON_COL_WIDTH)) {
                        MayTickIcon(it.isInherited)
                    },
                    Table.Column(header = Label.KEY, size = Either.second(ICON_COL_WIDTH)) {
                        MayTickIcon(it.isKey)
                    }
                )
            )
        }
    }

    private fun ownedAttributeTypesContextMenu(props: AttributeTypeState.OwnedAttTypeProperties) = listOf(
        listOf(
            ContextMenu.Item(
                label = Label.GO_TO_ATTRIBUTE_TYPE,
                icon = Icon.GO_TO,
            ) { props.attributeType.tryOpen() },
            ContextMenu.Item(
                label = Label.GO_TO_OVERRIDDEN_TYPE,
                icon = Icon.GO_TO,
                enabled = props.overriddenType != null
            ) { props.overriddenType?.tryOpen() },
            ContextMenu.Item(
                label = Label.GO_TO_EXTENDED_TYPE,
                icon = Icon.GO_TO,
                enabled = props.extendedType != null
            ) { props.extendedType?.tryOpen() }
        ),
        listOf(
            ContextMenu.Item(
                label = Label.CHANGE_OVERRIDDEN_OWNED_ATTRIBUTE_TYPE,
                icon = Icon.TYPES,
                enabled = canWriteSchema
            ) { typeState.initiateChangeOverriddenOwnedAttributeType(props) }
        ),
        listOf(
            ContextMenu.Item(
                label = Label.REMOVE,
                icon = Icon.REMOVE,
                enabled = canWriteSchema && !props.isInherited && props.canBeUndefined
            ) { typeState.tryUndefineOwnedAttributeType(props.attributeType) }
        )
    )

    @Composable
    private fun DefineOwnsAttributeType() {
        var attributeType: AttributeTypeState? by remember { mutableStateOf(null) }
        val definedAttrTypes = (typeState.ownedAttTypes + typeState.supertypes.flatMap { it.ownedAttTypes }).toSet()
        val attributeTypeList = Service.schema.rootAttributeType?.subtypes
            ?.filter { !definedAttrTypes.contains(it) }
            ?.sortedBy { it.name }
            ?: listOf()

        var overriddenType: AttributeTypeState? by remember { mutableStateOf(null) }
        val overridableTypeList: List<AttributeTypeState> = attributeType?.let {
            typeState.overridableOwnedAttributeTypes(it)
        } ?: listOf()

        val isOwnable = canWriteSchema && attributeType != null
        val isOverridable = canWriteSchema && overridableTypeList.isNotEmpty()
        var isKey: Boolean by remember { mutableStateOf(false) }

        SectionRow {
            Box(Modifier.weight(1f)) {
                Form.Dropdown(
                    values = attributeTypeList,
                    selected = attributeType,
                    displayFn = { TypeLabelWithDetails(it.conceptType, it.isAbstract) },
                    onSelection = { attributeType = it; it?.loadSupertypesAsync() },
                    onExpand = { Service.schema.rootAttributeType?.loadSubtypesRecursivelyAsync() },
                    placeholder = Label.ATTRIBUTE_TYPE.hyphenate().lowercase(),
                    modifier = Modifier.fillMaxSize(),
                    allowNone = true,
                    enabled = canWriteSchema
                )
            }
            Form.Text(value = Label.AS.lowercase(), enabled = isOverridable)
            Box(Modifier.weight(1f)) {
                Form.Dropdown(
                    values = overridableTypeList,
                    selected = overriddenType,
                    displayFn = { TypeLabelWithDetails(it.conceptType, it.isAbstract) },
                    onSelection = { overriddenType = it },
                    placeholder = Label.OVERRIDDEN_TYPE.hyphenate().lowercase(),
                    modifier = Modifier.fillMaxWidth(),
                    allowNone = true,
                    enabled = isOverridable
                )
            }
            Form.Text(value = Label.KEY.lowercase())
            Form.Checkbox(
                value = isKey,
            ) { isKey = it }
            Form.TextButton(
                text = Label.OWNS,
                leadingIcon = Form.IconArg(Icon.ADD) { Theme.studio.secondary },
                enabled = isOwnable,
                tooltip = Tooltip.Arg(Label.DEFINE_OWNS_ATTRIBUTE_TYPE, Sentence.EDITING_TYPES_REQUIREMENT_DESCRIPTION),
                onClick = {
                    typeState.tryDefineOwnsAttributeType(attributeType!!, overriddenType, isKey) {
                        attributeType = null
                        overriddenType = null
                        isKey = false
                    }
                }
            )
        }
    }

    @Composable
    protected fun PlayedRoleTypesSection() {
        SectionRow { Form.Text(value = Label.PLAYS) }
        RoleTypesTable(typeState.playedRoleTypeProperties) { playedRoleTypesContextMenu(it) }
        DefinePlaysRoleType()
    }

    private fun playedRoleTypesContextMenu(props: RoleTypeState.PlayedRoleTypeProperties) = listOf(
        listOf(
            ContextMenu.Item(
                label = Label.GO_TO_ROLE_TYPE,
                icon = Icon.GO_TO,
            ) { props.roleType.relationType.tryOpen() },
            ContextMenu.Item(
                label = Label.GO_TO_OVERRIDDEN_TYPE,
                icon = Icon.GO_TO,
                enabled = props.overriddenType != null,
            ) { props.overriddenType?.relationType?.tryOpen() },
            ContextMenu.Item(
                label = Label.GO_TO_EXTENDED_TYPE,
                icon = Icon.GO_TO,
                enabled = props.extendedType != null,
            ) { props.extendedType?.tryOpen() }
        ),
        listOf(
            ContextMenu.Item(
                label = Label.CHANGE_OVERRIDDEN_PLAYED_ROLE_TYPE,
                icon = Icon.TYPES,
                enabled = canWriteSchema
            ) { typeState.initiateChangeOverriddenPlayedRoleType(props) }
        ),
        listOf(
            ContextMenu.Item(
                label = Label.REMOVE,
                icon = Icon.REMOVE,
                enabled = canWriteSchema && props.canBeUndefined
            ) { typeState.tryUndefinePlayedRoleType(props.roleType) }
        )
    )

    @Composable
    protected fun <T : RoleTypeState.RoleTypeProperties> RoleTypesTable(
        roleTypeProperties: List<T>,
        contextMenuFn: (T) -> List<List<ContextMenu.Item>>,
    ) {
        val tableHeight = TABLE_ROW_HEIGHT * (roleTypeProperties.size + 1).coerceAtLeast(2)
        SectionRow {
            Table.Layout(
                items = roleTypeProperties.sortedBy { it.roleType.scopedName },
                modifier = Modifier.weight(1f).height(tableHeight).border(1.dp, Theme.studio.border),
                rowHeight = TABLE_ROW_HEIGHT,
                contextMenuFn = contextMenuFn,
                columns = listOf(
                    Table.Column(header = Label.ROLE_TYPE, contentAlignment = Alignment.CenterStart) { props ->
                        TypeTableCellText(props.roleType) { props.roleType.relationType.tryOpen() }
                    },
                    Table.Column(header = Label.OVERRIDDEN_TYPE, contentAlignment = Alignment.CenterStart) { props ->
                        props.overriddenType?.let { ot -> TypeTableCellText(ot) { ot.relationType.tryOpen() } }
                    },
                    Table.Column(header = Label.EXTENDED_TYPE, contentAlignment = Alignment.CenterStart) { props ->
                        props.extendedType?.let { ot -> TypeTableCellText(ot) { ot.tryOpen() } }
                    },
                    Table.Column(header = Label.INHERITED, size = Either.second(ICON_COL_WIDTH)) {
                        MayTickIcon(it.isInherited)
                    }
                )
            )
        }
    }

    @Composable
    private fun DefinePlaysRoleType() {
        var roleType: RoleTypeState? by remember { mutableStateOf(null) }
        val definedRoleTypes = (typeState.playedRoleTypes + typeState.supertypes.flatMap { it.playedRoleTypes }).toSet()
        val roleTypeList = Service.schema.rootRelationType?.subtypes
            ?.flatMap { it.relatedRoleTypes }
            ?.filter { !definedRoleTypes.contains(it) }
            ?.sortedBy { it.scopedName }
            ?: listOf()

        var overriddenType: RoleTypeState? by remember { mutableStateOf(null) }
        val overridableTypeList: List<RoleTypeState> = roleType?.let {
            typeState.overridablePlayedRoleTypes(it)
        } ?: listOf()

        val isPlayable = canWriteSchema && roleType != null
        val isOverridable = canWriteSchema && overridableTypeList.isNotEmpty()

        SectionRow {
            Box(Modifier.weight(1f)) {
                Form.Dropdown(
                    values = roleTypeList,
                    selected = roleType,
                    displayFn = { TypeLabelWithDetails(it.conceptType, it.isAbstract) },
                    onSelection = { roleType = it; it?.loadSupertypesAsync() },
                    onExpand = { Service.schema.rootRelationType?.loadRelatedRoleTypesRecursivelyAsync() },
                    placeholder = Label.ROLE_TYPE.hyphenate().lowercase(),
                    modifier = Modifier.fillMaxSize(),
                    allowNone = true,
                    enabled = canWriteSchema
                )
            }
            Form.Text(value = Label.AS.lowercase(), enabled = isOverridable)
            Box(Modifier.weight(1f)) {
                Form.Dropdown(
                    values = overridableTypeList,
                    selected = overriddenType,
                    displayFn = { TypeLabelWithDetails(it.conceptType, it.isAbstract) },
                    onSelection = { overriddenType = it },
                    placeholder = Label.OVERRIDDEN_TYPE.hyphenate().lowercase(),
                    modifier = Modifier.fillMaxWidth(),
                    allowNone = true,
                    enabled = isOverridable
                )
            }
            Form.TextButton(
                text = Label.PLAYS,
                leadingIcon = Form.IconArg(Icon.ADD) { Theme.studio.secondary },
                enabled = isPlayable,
                tooltip = Tooltip.Arg(Label.DEFINE_PLAYS_ROLE_TYPE, Sentence.EDITING_TYPES_REQUIREMENT_DESCRIPTION),
                onClick = {
                    typeState.tryDefinePlaysRoleType(roleType!!, overriddenType) {
                        roleType = null
                        overriddenType = null
                    }
                }
            )
        }
    }

    @Composable
    protected fun SubtypesSection() {
        val visibleSize = typeState.subtypes.size.coerceAtMost(MAX_VISIBLE_SUBTYPES)
        SectionRow { Form.Text(value = Label.SUBTYPES) }
        SectionRow {
            Navigator.Layout(
                state = subtypesNavState,
                modifier = Modifier.weight(1f)
                    .height((Navigator.ITEM_HEIGHT * visibleSize).coerceAtLeast(EMPTY_BOX_HEIGHT))
                    .border(1.dp, Theme.studio.border)
                    .background(Theme.studio.backgroundMedium),
                itemHeight = if (typeState.subtypes.size > 1) Navigator.ITEM_HEIGHT else EMPTY_BOX_HEIGHT,
                bottomSpace = 0.dp,
                iconArg = { iconOf(it.item.conceptType) }
            )
        }
        LaunchedEffect(subtypesNavState) {
            if (subtypesNavStateLaunched) subtypesNavState.reloadEntries()
            else {
                subtypesNavState.launch()
                subtypesNavStateLaunched = true
            }
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
            leadingIcon = Form.IconArg(Icon.DELETE) { Theme.studio.errorStroke },
            enabled = canWriteSchema && typeState.canBeDeleted,
            tooltip = Tooltip.Arg(Label.DELETE, Sentence.EDITING_TYPES_REQUIREMENT_DESCRIPTION)
        ) { typeState.initiateDelete() }
    }

    @Composable
    private fun ExportButton() {
        Form.TextButton(
            text = Label.EXPORT,
            leadingIcon = Form.IconArg(Icon.EXPORT),
            enabled = Service.project.current != null && canReadSchema,
            tooltip = Tooltip.Arg(Label.EXPORT_SYNTAX)
        ) {
            typeState.exportSyntaxAsync { syntax ->
                Service.project.tryCreateUntitledFile()?.let { file ->
                    file.content(syntax)
                    file.tryOpen()
                }
            }
        }
    }

    @Composable
    private fun RefreshButton() {
        Form.TextButton(
            text = Label.REFRESH,
            leadingIcon = Form.IconArg(Icon.REFRESH),
            enabled = canReadSchema,
            tooltip = Tooltip.Arg(Label.REFRESH)
        ) {
            Service.schema.closeReadTx()
            typeState.loadConstraintsAsync()
        }
    }

    @Composable
    protected fun MayTickIcon(boolean: Boolean) {
        if (boolean) Icon.Render(icon = Icon.TICK, color = Theme.studio.secondary)
    }

    @Composable
    protected fun EditButton(title: String, enabled: Boolean = true, onClick: () -> Unit) {
        Form.IconButton(
            icon = Icon.RENAME,
            enabled = canWriteSchema && enabled,
            tooltip = Tooltip.Arg(title, Sentence.EDITING_TYPES_REQUIREMENT_DESCRIPTION),
            onClick = onClick
        )
    }

    class Entity constructor(typeState: EntityTypeState) : TypeEditor<EntityType, EntityTypeState>(
        typeState = typeState, showAdvanced = false
    ) {

        @Composable
        override fun MainSections() {
            OwnedAttributeTypesSection()
            Separator()
            PlayedRoleTypesSection()
            Separator()
            SubtypesSection()
            Separator()
        }
    }

    class Relation constructor(typeState: RelationTypeState) : TypeEditor<RelationType, RelationTypeState>(
        typeState = typeState, showAdvanced = typeState.playedRoleTypes.isNotEmpty()
    ) {

        @Composable
        override fun MainSections() {
            RelatedRoleTypesSection()
            Separator()
            OwnedAttributeTypesSection()
            Separator()
            SubtypesSection()
            AdvancedSections {
                PlayedRoleTypesSection()
            }
        }

        @Composable
        private fun RelatedRoleTypesSection() {
            SectionRow { Form.Text(value = Label.RELATES) }
            RoleTypesTable(typeState.relatedRoleTypeProperties) { relatedRoleTypesContextMenu(it) }
            DefineRelatesRoleType()
        }

        private fun relatedRoleTypesContextMenu(props: RoleTypeState.RelatedRoleTypeProperties) = listOf(
            listOf(
                ContextMenu.Item(
                    label = Label.GO_TO_ROLE_TYPE,
                    icon = Icon.GO_TO,
                    enabled = props.isInherited,
                ) { props.roleType.relationType.tryOpen() },
                ContextMenu.Item(
                    label = Label.GO_TO_OVERRIDDEN_TYPE,
                    icon = Icon.GO_TO,
                    enabled = props.overriddenType != null,
                ) { props.overriddenType?.relationType?.tryOpen() },
                ContextMenu.Item(
                    label = Label.GO_TO_EXTENDED_TYPE,
                    icon = Icon.GO_TO,
                    enabled = props.extendedType != null,
                ) { props.extendedType?.tryOpen() }
            ),
            listOf(
                ContextMenu.Item(
                    label = Label.RENAME,
                    icon = Icon.RENAME,
                    enabled = canWriteSchema && !props.isInherited,
                ) { props.roleType.initiateRename() },
                ContextMenu.Item(
                    label = Label.CHANGE_OVERRIDDEN_RELATED_ROLE_TYPE,
                    icon = Icon.TYPES,
                    enabled = canWriteSchema && !props.isInherited,
                ) { typeState.initiateChangeOverriddenRelatedRoleType(props) },
            ),
            listOf(
                ContextMenu.Item(
                    label = Label.DELETE,
                    icon = Icon.DELETE,
                    enabled = canWriteSchema && !props.isInherited && props.roleType.canBeDeleted
                ) { typeState.initiateDeleteRoleType(props.roleType) }
            )
        )

        @Composable
        private fun DefineRelatesRoleType() {
            var roleType: String by remember { mutableStateOf("") }
            var overriddenType: RoleTypeState? by remember { mutableStateOf(null) }
            val overridableTypeList = typeState.overridableRelatedRoleTypes()

            val isRelatable = canWriteSchema && roleType.isNotEmpty()
            val isOverridable = canWriteSchema && overridableTypeList.isNotEmpty()

            fun submit() = typeState.tryDefineRelatesRoleType(roleType, overriddenType) {
                roleType = ""
                overriddenType = null
            }

            SectionRow {
                Form.TextInput(
                    value = roleType,
                    placeholder = Label.ROLE.lowercase(),
                    modifier = Modifier.weight(1f).onKeyEvent { Form.onKeyEventHandler(it, onEnter = { submit() }) },
                    onValueChange = { roleType = it },
                    enabled = canWriteSchema,
                )
                Form.Text(value = Label.AS.lowercase(), enabled = isOverridable)
                Box(Modifier.weight(1f)) {
                    Form.Dropdown(
                        values = overridableTypeList,
                        selected = overriddenType,
                        displayFn = { TypeLabelWithDetails(it.conceptType, it.isAbstract) },
                        onSelection = { overriddenType = it },
                        placeholder = Label.OVERRIDDEN_TYPE.hyphenate().lowercase(),
                        modifier = Modifier.fillMaxWidth(),
                        allowNone = true,
                        enabled = isOverridable
                    )
                }
                Form.TextButton(
                    text = Label.RELATES,
                    leadingIcon = Form.IconArg(Icon.ADD) { Theme.studio.secondary },
                    enabled = isRelatable,
                    tooltip = Tooltip.Arg(
                        Label.DEFINE_RELATES_ROLE_TYPE,
                        Sentence.EDITING_TYPES_REQUIREMENT_DESCRIPTION
                    ),
                    onClick = { submit() }
                )
            }
        }
    }

    class Attribute constructor(typeState: AttributeTypeState) : TypeEditor<AttributeType, AttributeTypeState>(
        typeState = typeState,
        showAdvanced = typeState.ownedAttTypes.isNotEmpty() || typeState.playedRoleTypes.isNotEmpty()
    ) {

        @Composable
        override fun MainSections() {
            OwnerTypesSection()
            Separator()
            SubtypesSection()
            Separator()
        }

        @Composable
        private fun OwnerTypesSection() {
            val tableHeight = TABLE_ROW_HEIGHT * (typeState.ownerTypes.size + 1).coerceAtLeast(2)
            SectionRow { Form.Text(value = Label.OWNERS) }
            SectionRow {
                Table.Layout(
                    items = typeState.ownerTypeProperties.sortedBy { it.ownerType.name },
                    modifier = Modifier.weight(1f).height(tableHeight).border(1.dp, Theme.studio.border),
                    rowHeight = TABLE_ROW_HEIGHT,
                    contextMenuFn = { ownerTypesContextMenu(it) },
                    columns = listOf(
                        Table.Column(header = Label.OWNER_TYPE, contentAlignment = Alignment.CenterStart) { props ->
                            TypeTableCellText(props.ownerType) { props.ownerType.tryOpen() }
                        },
                        Table.Column(header = Label.EXTENDED_TYPE, contentAlignment = Alignment.CenterStart) { props ->
                            props.extendedType?.let { ot -> TypeTableCellText(ot) { ot.tryOpen() } }
                        },
                        Table.Column(header = Label.INHERITED, size = Either.second(ICON_COL_WIDTH)) {
                            MayTickIcon(it.isInherited)
                        },
                        Table.Column(header = Label.KEY, size = Either.second(ICON_COL_WIDTH)) {
                            MayTickIcon(it.isKey)
                        }
                    )
                )
            }
        }

        private fun ownerTypesContextMenu(props: AttributeTypeState.AttTypeOwnerProperties) = listOf(
            listOf(
                ContextMenu.Item(
                    label = Label.GO_TO_OWNER_TYPE,
                    icon = Icon.GO_TO,
                ) { props.ownerType.tryOpen() },
                ContextMenu.Item(
                    label = Label.GO_TO_EXTENDED_TYPE,
                    icon = Icon.GO_TO,
                    enabled = props.extendedType != null,
                ) { props.extendedType?.tryOpen() }
            )
        )
    }
}
