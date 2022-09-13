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
import com.vaticle.typedb.studio.state.schema.SchemaManager
import com.vaticle.typedb.studio.state.schema.TypeState

object TypeDialog {

    private class CreateTypeFormState<T : TypeState.Thing> constructor(
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

    private class RenameTypeFormState constructor(
        initField: String,
        val isValid: ((String) -> Boolean)? = null,
        val onCancel: () -> Unit,
        val onSubmit: (String) -> Unit,
    ) : Form.State {

        var label: String by mutableStateOf(initField)

        override fun cancel() = onCancel()
        override fun isValid(): Boolean = label.isNotEmpty() && isValid?.invoke(label) ?: true

        override fun trySubmit() {
            assert(label.isNotBlank())
            onSubmit(label)
        }
    }

    private val DIALOG_WIDTH = 500.dp
    private val DIALOG_HEIGHT = 300.dp

    @Composable
    fun MayShowDialogs() {
        if (StudioState.schema.createEntTypeDialog.isOpen) CreateEntityTypeDialog()
        if (StudioState.schema.createRelTypeDialog.isOpen) CreateRelationTypeDialog()
        if (StudioState.schema.createAttTypeDialog.isOpen) CreateAttributeTypeDialog()
        if (StudioState.schema.renameTypeDialog.isOpen) RenameTypeDialog()
        if (StudioState.schema.editSuperTypeDialog.isOpen) ChangeSupertypeDialog()
    }

    private fun createThingTypeMessage(supertypeState: TypeState.Thing, name: String) = when (supertypeState.name) {
        supertypeState.encoding.label -> Sentence.CREATE_TYPE.format(supertypeState.encoding.label.lowercase())
        else -> Sentence.CREATE_TYPE_AS_SUBTYPE_OF.format(supertypeState.encoding.label.lowercase(), name)
    }

    @Composable
    private fun CreateEntityTypeDialog() = CreateThingTypeDialog(
        dialogState = StudioState.schema.createEntTypeDialog,
        rootTypeState = StudioState.schema.rootEntityType!!,
        title = Label.CREATE_ENTITY_TYPE
    ) { supertypeState, label, isAbstract, _ -> supertypeState.tryCreateSubtype(label, isAbstract) }

    @Composable
    private fun CreateRelationTypeDialog() = CreateThingTypeDialog(
        dialogState = StudioState.schema.createRelTypeDialog,
        rootTypeState = StudioState.schema.rootRelationType!!,
        title = Label.CREATE_RELATION_TYPE
    ) { supertypeState, label, isAbstract, _ -> supertypeState.tryCreateSubtype(label, isAbstract) }

    @Composable
    private fun CreateAttributeTypeDialog() = CreateThingTypeDialog(
        dialogState = StudioState.schema.createAttTypeDialog,
        rootTypeState = StudioState.schema.rootAttributeType!!,
        title = Label.CREATE_ATTRIBUTE_TYPE
    ) { supertypeState, label, isAbstract, valueType ->
        supertypeState.tryCreateSubtype(label, isAbstract, valueType!!)
    }

    @Suppress("UNCHECKED_CAST")
    @Composable
    private fun <T : TypeState.Thing> CreateThingTypeDialog(
        dialogState: SchemaManager.EditTypeDialog<T>, rootTypeState: T, title: String,
        creatorFn: (supertypeState: T, label: String, isAbstract: Boolean, valueType: ValueType?) -> Unit
    ) {
        val supertypeState = dialogState.typeState!!
        createThingTypeMessage(supertypeState, supertypeState.name)
        val formState = remember {
            CreateTypeFormState(
                supertypeState = supertypeState,
                valueType = if (supertypeState is TypeState.Attribute) supertypeState.valueType else null,
                isValid = if (supertypeState is TypeState.Attribute) { { it.valueType != null } } else null,
                onCancel = { dialogState.close() },
                onSubmit = creatorFn
            )
        }
        Dialog.Layout(dialogState, title, DIALOG_WIDTH, DIALOG_HEIGHT) {
            Submission(state = formState, modifier = Modifier.fillMaxSize(), submitLabel = Label.CREATE) {
                LabelField(formState.label) { formState.label = it }
                SupertypeField(formState, rootTypeState)
                if (supertypeState is TypeState.Attribute) ValueTypeField(formState)
                AbstractField(formState.isAbstract) { formState.isAbstract = it }
            }
        }
    }

    @Composable
    private fun RenameTypeDialog() {
        val dialogState = StudioState.schema.renameTypeDialog
        val typeState = dialogState.typeState!!
        val message = Sentence.RENAME_TYPE.format(typeState.encoding.label, typeState.name)
        val formState = remember {
            RenameTypeFormState(
                initField = typeState.name,
                isValid = { it != typeState.name },
                onCancel = { dialogState.close() },
                onSubmit = { typeState.tryRename(it) }
            )
        }
        Dialog.Layout(dialogState, Label.RENAME_TYPE, DIALOG_WIDTH, DIALOG_HEIGHT) {
            Submission(state = formState, modifier = Modifier.fillMaxSize(), submitLabel = Label.RENAME) {
                Form.Text(value = message, softWrap = true)
                LabelField(formState.label) { formState.label = it }
            }
        }
    }


    @Composable
    private fun ChangeSupertypeDialog() {
        // TODO
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
    private fun <T : TypeState.Thing> SupertypeField(formState: CreateTypeFormState<T>, rootTypeState: T) {
        Field(label = Label.SUPERTYPE) {
            Form.Dropdown(
                selected = formState.supertypeState,
                values = (listOf(rootTypeState) + rootTypeState.subtypes).map { it as T },
                displayFn = { AnnotatedString(it.name) },
                onSelection = {
                    formState.supertypeState = it
                    if (it is TypeState.Attribute) formState.valueType = it.valueType
                }
            )
        }
        LaunchedEffect(formState) { rootTypeState.loadSubtypesRecursively() }
    }

    @Composable
    private fun <T : TypeState.Thing> ValueTypeField(formState: CreateTypeFormState<T>) {
        Field(label = Label.VALUE_TYPE) {
            Form.Dropdown(
                selected = formState.valueType,
                values = remember { ValueType.values().toList() - ValueType.OBJECT },
                displayFn = { AnnotatedString(it.name.lowercase()) },
                placeholder = Label.VALUE_TYPE.lowercase().hyphenate(),
                onSelection = { formState.valueType = it },
                enabled = formState.supertypeState.isRoot
            )
        }
    }

    @Composable
    private fun AbstractField(value: Boolean, onChange: (Boolean) -> Unit) {
        Field(label = Label.ABSTRACT) { Checkbox(value = value, onChange = onChange) }
    }
}
