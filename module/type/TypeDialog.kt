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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.client.api.concept.type.AttributeType.ValueType
import com.vaticle.typedb.studio.framework.common.Util.hyphenate
import com.vaticle.typedb.studio.framework.material.Dialog
import com.vaticle.typedb.studio.framework.material.Form
import com.vaticle.typedb.studio.framework.material.Form.Checkbox
import com.vaticle.typedb.studio.framework.material.Form.Field
import com.vaticle.typedb.studio.framework.material.Form.Submission
import com.vaticle.typedb.studio.framework.material.Form.TextInput
import com.vaticle.typedb.studio.state.StudioState
import com.vaticle.typedb.studio.state.common.util.Label
import com.vaticle.typedb.studio.state.common.util.Sentence
import com.vaticle.typedb.studio.state.schema.SchemaService
import com.vaticle.typedb.studio.state.schema.TypeState

object TypeDialog {

    private class CreateTypeFormState<T : TypeState.Thing<*, T>> constructor(
        supertypeState: T,
        valueType: ValueType? = null,
        val isValid: ((CreateTypeFormState<T>) -> Boolean)? = null,
        val onCancel: () -> Unit,
        val onSubmit: (supertypeState: T, label: String, isAbstract: Boolean, valueType: ValueType?) -> Unit,
    ) : Form.State {

        var supertypeState: T by mutableStateOf(supertypeState)
        var label: String by mutableStateOf(""); internal set
        var isAbstract: Boolean by mutableStateOf(false); internal set
        var valueType: ValueType? by mutableStateOf(valueType); internal set

        override fun cancel() = onCancel()
        override fun isValid(): Boolean = label.isNotEmpty() && isValid?.invoke(this) ?: true

        override fun trySubmit() {
            assert(isValid())
            onSubmit(supertypeState, label, isAbstract, valueType)
        }
    }

    private val DIALOG_WIDTH = 500.dp
    private val DIALOG_HEIGHT = 300.dp

    @Composable
    fun MayShowDialogs() {
        if (StudioState.schema.renameTypeDialog.isOpen) RenameTypeDialog()
        if (StudioState.schema.createEntityTypeDialog.isOpen) CreateEntityTypeDialog()
        if (StudioState.schema.createRelationTypeDialog.isOpen) CreateRelationTypeDialog()
        if (StudioState.schema.createAttributeTypeDialog.isOpen) CreateAttributeTypeDialog()
        if (StudioState.schema.changeEntitySupertypeDialog.isOpen) ChangeEntitySupertypeDialog()
        if (StudioState.schema.changeAttributeSupertypeDialog.isOpen) ChangeAttributeSupertypeDialog()
        if (StudioState.schema.changeRelationSupertypeDialog.isOpen) ChangeRelationSupertypeDialog()
        if (StudioState.schema.changeOverriddenRoleTypeDialog.isOpen) ChangeRoleOverriddenTypeDialog()
        if (StudioState.schema.changeAbstractDialog.isOpen) ChangeAbstractDialog()
    }

    @Composable
    private fun CreateEntityTypeDialog() = CreateThingTypeDialog(
        dialogState = StudioState.schema.createEntityTypeDialog,
        rootTypeState = StudioState.schema.rootEntityType!!,
        title = Label.CREATE_ENTITY_TYPE
    ) { supertypeState, label, isAbstract, _ -> supertypeState.tryCreateSubtype(label, isAbstract) }

    @Composable
    private fun CreateRelationTypeDialog() = CreateThingTypeDialog(
        dialogState = StudioState.schema.createRelationTypeDialog,
        rootTypeState = StudioState.schema.rootRelationType!!,
        title = Label.CREATE_RELATION_TYPE
    ) { supertypeState, label, isAbstract, _ -> supertypeState.tryCreateSubtype(label, isAbstract) }

    @Composable
    private fun CreateAttributeTypeDialog() =
        CreateThingTypeDialog(dialogState = StudioState.schema.createAttributeTypeDialog,
            rootTypeState = StudioState.schema.rootAttributeType!!,
            title = Label.CREATE_ATTRIBUTE_TYPE,
            isValidFn = { it.valueType != null }) { supertypeState, label, isAbstract, valType ->
            supertypeState.tryCreateSubtype(
                label,
                isAbstract,
                valType!!
            )
        }

    @Composable
    private fun <T : TypeState.Thing<*, T>> CreateThingTypeDialog(
        dialogState: SchemaService.TypeDialogState<T>,
        rootTypeState: T,
        title: String,
        isValidFn: ((formState: CreateTypeFormState<T>) -> Boolean)? = null,
        creatorFn: (supertypeState: T, label: String, isAbstract: Boolean, valueType: ValueType?) -> Unit
    ) {
        val supertypeState = dialogState.typeState!!
        val formState = remember {
            CreateTypeFormState(
                supertypeState = supertypeState,
                valueType = if (supertypeState is TypeState.Attribute) supertypeState.valueType else null,
                isValid = isValidFn,
                onCancel = { dialogState.close() },
                onSubmit = creatorFn
            )
        }
        Dialog.Layout(dialogState, title, DIALOG_WIDTH, DIALOG_HEIGHT) {
            Submission(state = formState, modifier = Modifier.fillMaxSize(), submitLabel = Label.CREATE) {
                LabelField(formState.label) { formState.label = it }
                SupertypeField(formState.supertypeState, (listOf(rootTypeState) + rootTypeState.subtypes).map { it }) {
                    formState.supertypeState = it
                    if (it is TypeState.Attribute) formState.valueType = it.valueType
                }
                if (supertypeState is TypeState.Attribute) ValueTypeField(formState)
                AbstractField(formState.isAbstract) { formState.isAbstract = it }
            }
        }
        LaunchedEffect(formState) { rootTypeState.loadSubtypesRecursivelyAsync() }
    }

    @Composable
    private fun RenameTypeDialog() {
        val dialogState = StudioState.schema.renameTypeDialog
        val typeState = dialogState.typeState!!
        val formState = remember {
            object : Form.State {
                var label: String by mutableStateOf(typeState.name)
                override fun cancel() = dialogState.close()
                override fun isValid() = label.isNotEmpty() && label != typeState.name
                override fun trySubmit() = typeState.tryRename(label)
            }
        }
        Dialog.Layout(dialogState, Label.RENAME_TYPE, DIALOG_WIDTH, DIALOG_HEIGHT) {
            Submission(state = formState, modifier = Modifier.fillMaxSize(), submitLabel = Label.RENAME) {
                Form.Text(Sentence.RENAME_TYPE.format(typeState.encoding.label, typeState.name), softWrap = true)
                LabelField(formState.label) { formState.label = it }
            }
        }
    }

    @Composable
    private fun ChangeEntitySupertypeDialog() = ChangeSupertypeDialog(
        title = Label.CHANGE_ENTITY_SUPERTYPE,
        dialogState = StudioState.schema.changeEntitySupertypeDialog,
        selection = StudioState.schema.rootEntityType!!.subtypesWithSelf
    )

    @Composable
    private fun ChangeAttributeSupertypeDialog() = ChangeSupertypeDialog(
        title = Label.CHANGE_ATTRIBUTE_SUPERTYPE,
        dialogState = StudioState.schema.changeAttributeSupertypeDialog,
        selection = StudioState.schema.rootAttributeType!!.subtypesWithSelf.filter {
            it.valueType == StudioState.schema.changeAttributeSupertypeDialog.typeState!!.valueType
                    || it == StudioState.schema.rootAttributeType
        })

    @Composable
    private fun ChangeRelationSupertypeDialog() = ChangeSupertypeDialog(
        title = Label.CHANGE_RELATION_SUPERTYPE,
        dialogState = StudioState.schema.changeRelationSupertypeDialog,
        selection = StudioState.schema.rootRelationType!!.subtypesWithSelf
    )

    @Composable
    private fun <T : TypeState.Thing<*, T>> ChangeSupertypeDialog(
        title: String, dialogState: SchemaService.TypeDialogState<T>, selection: List<T>,
    ) {
        val typeState = dialogState.typeState!!
        val formState = remember {
            object : Form.State {
                var supertypeState: T by mutableStateOf(typeState.supertype!!)
                override fun cancel() = dialogState.close()
                override fun trySubmit() = typeState.tryChangeSupertype(supertypeState)
                override fun isValid() = true
            }
        }
        Dialog.Layout(dialogState, title, DIALOG_WIDTH, DIALOG_HEIGHT) {
            Submission(state = formState, modifier = Modifier.fillMaxSize()) {
                Form.Text(Sentence.CHANGE_SUPERTYPE.format(typeState.encoding.label, typeState.name), softWrap = true)
                SupertypeField(
                    selected = formState.supertypeState,
                    values = selection.filter { it != typeState }.sortedBy { it.name }
                ) { formState.supertypeState = it }
            }
        }
    }

    @Composable
    private fun ChangeRoleOverriddenTypeDialog() {
        val dialogState = StudioState.schema.changeOverriddenRoleTypeDialog
        val typeState = dialogState.typeState!!
        val message = Sentence.CHANGE_OVERRIDDEN_TYPE.format(typeState.encoding.label, typeState.scopedName)
        val selection = typeState.relationType.supertype!!.relatesRoleTypes.filter {
            it != StudioState.schema.rootRoleType
        }
        val formState = remember {
            object : Form.State {
                var overriddenType: TypeState.Role? by mutableStateOf(
                    if (typeState.supertype != StudioState.schema.rootRoleType) typeState.supertype else null
                )

                override fun cancel() = dialogState.close()
                override fun isValid() = true
                override fun trySubmit() = typeState.tryChangeOverriddenType(overriddenType)
            }
        }
        Dialog.Layout(dialogState, Label.CHANGE_OVERRIDDEN_ROLE_TYPE, DIALOG_WIDTH, DIALOG_HEIGHT) {
            Submission(state = formState, modifier = Modifier.fillMaxSize()) {
                Form.Text(message, softWrap = true)
                OverriddenTypeField(
                    selected = formState.overriddenType,
                    values = selection.filter { it != typeState }.sortedBy { it.name }
                ) { formState.overriddenType = it }
            }
        }
    }

    @Composable
    private fun ChangeAbstractDialog() {
        val dialogState = StudioState.schema.changeAbstractDialog
        val typeState = dialogState.typeState!!
        val message = Sentence.CHANGE_TYPE_ABSTRACTNESS.format(typeState.encoding.label, typeState.name)
        val formState = remember {
            object : Form.State {
                var isAbstract: Boolean by mutableStateOf(typeState.isAbstract)
                override fun cancel() = dialogState.close()
                override fun trySubmit() = typeState.tryChangeAbstract(isAbstract)
                override fun isValid() = true
            }
        }
        Dialog.Layout(dialogState, Label.CHANGE_TYPE_ABSTRACTNESS, DIALOG_WIDTH, DIALOG_HEIGHT) {
            Submission(state = formState, modifier = Modifier.fillMaxSize(), submitLabel = Label.SAVE) {
                Form.Text(message, softWrap = true)
                AbstractField(formState.isAbstract) { formState.isAbstract = it }
            }
        }
    }

    @Composable
    private fun LabelField(value: String, onChange: (String) -> Unit) {
        val focusReq = remember { FocusRequester() }
        Field(label = Label.LABEL) {
            TextInput(
                value = value,
                placeholder = "type-label",
                onValueChange = onChange,
                modifier = Modifier.focusRequester(focusReq)
            )
        }
        LaunchedEffect(focusReq) { focusReq.requestFocus() }
    }

    @Composable
    private fun <T : TypeState<*, T>> SupertypeField(
        selected: T, values: List<T>, onSelection: (value: T) -> Unit
    ) = Field(label = Label.SUPERTYPE) {
        Form.Dropdown(
            values = values,
            selected = selected,
            displayFn = { AnnotatedString(it.name) },
            onSelection = { onSelection(it!!) },
            modifier = Modifier.fillMaxSize()
        )
    }

    @Composable
    private fun <T : TypeState<*, T>> OverriddenTypeField(
        selected: T?, values: List<T>, onSelection: (value: T?) -> Unit
    ) = Field(label = Label.OVERRIDDEN_TYPE) {
        Form.Dropdown(
            values = values,
            selected = selected,
            displayFn = { AnnotatedString(it.name) },
            onSelection = { onSelection(it) },
            placeholder = Label.OVERRIDDEN_TYPE.hyphenate().lowercase(),
            modifier = Modifier.fillMaxSize(),
            allowNone = true
        )
    }

    @Composable
    private fun <T : TypeState.Thing<*, T>> ValueTypeField(
        formState: CreateTypeFormState<T>
    ) = Field(label = Label.VALUE_TYPE) {
        Form.Dropdown(
            values = remember { ValueType.values().toList() - ValueType.OBJECT },
            selected = formState.valueType,
            displayFn = { AnnotatedString(it.name.lowercase()) },
            onSelection = { formState.valueType = it!! },
            placeholder = Label.VALUE_TYPE.hyphenate().lowercase(),
            enabled = formState.supertypeState.isRoot
        )
    }

    @Composable
    private fun AbstractField(value: Boolean, onChange: (Boolean) -> Unit) = Field(label = Label.ABSTRACT) {
        Checkbox(value = value, onChange = onChange)
    }
}
