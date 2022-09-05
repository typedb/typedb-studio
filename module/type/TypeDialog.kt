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
import com.vaticle.typedb.client.api.concept.type.AttributeType
import com.vaticle.typedb.studio.framework.material.Dialog
import com.vaticle.typedb.studio.framework.material.Form
import com.vaticle.typedb.studio.framework.material.Form.Field
import com.vaticle.typedb.studio.framework.material.Form.Submission
import com.vaticle.typedb.studio.framework.material.Form.TextInput
import com.vaticle.typedb.studio.state.StudioState
import com.vaticle.typedb.studio.state.common.util.Label
import com.vaticle.typedb.studio.state.common.util.Sentence
import com.vaticle.typedb.studio.state.schema.SchemaManager
import com.vaticle.typedb.studio.state.schema.TypeState

object TypeDialog {

    private class TypeLabelForm constructor(
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

    private class CreateAttributeTypeForm constructor(
        valueType: AttributeType.ValueType?,
        val isValid: ((String) -> Boolean)? = null,
        val onCancel: () -> Unit,
        val onSubmit: (String, AttributeType.ValueType) -> Unit,
    ) : Form.State {

        var label: String by mutableStateOf("")
        var valueType: AttributeType.ValueType? by mutableStateOf(valueType)

        override fun cancel() = onCancel()
        override fun isValid(): Boolean = label.isNotEmpty() && valueType != null && isValid?.invoke(label) ?: true

        override fun trySubmit() {
            assert(isValid())
            onSubmit(label, valueType!!)
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
    private fun CreateEntityTypeDialog() = CreateEntOrRelTypeDialog(
        StudioState.schema.createEntTypeDialog, Label.CREATE_ENTITY_TYPE
    )

    @Composable
    private fun CreateRelationTypeDialog() = CreateEntOrRelTypeDialog(
        StudioState.schema.createRelTypeDialog, Label.CREATE_RELATION_TYPE
    )

    @Composable
    private fun <T : TypeState.Thing> CreateEntOrRelTypeDialog(
        dialogState: SchemaManager.EditTypeDialog<T>, title: String,
    ) {
        val supertypeState = dialogState.typeState!!
        val message = createThingTypeMessage(supertypeState, supertypeState.name)
        val formState = remember {
            TypeLabelForm(
                initField = "",
                onCancel = { dialogState.close() },
                onSubmit = { supertypeState.tryCreateSubtype(it) }
            )
        }
        TypeNamingDialog(dialogState, formState, title, message, Label.CREATE)
    }

    @Composable
    private fun CreateAttributeTypeDialog() {
        val dialogState = StudioState.schema.createAttTypeDialog
        val supertypeState = dialogState.typeState!!
        val message = createThingTypeMessage(
            supertypeState, supertypeState.name + (supertypeState.valueType?.let { " (${it.name.lowercase()})" } ?: "")
        )
        val formState = remember {
            CreateAttributeTypeForm(
                valueType = supertypeState.valueType,
                onCancel = { dialogState.close() },
                onSubmit = { label, valueType -> supertypeState.tryCreateSubtype(label, valueType) }
            )
        }
        val valueTypes = remember { AttributeType.ValueType.values().toList() - AttributeType.ValueType.OBJECT }
        Dialog.Layout(dialogState, Label.CREATE_ATTRIBUTE_TYPE, DIALOG_WIDTH, DIALOG_HEIGHT) {
            Submission(state = formState, modifier = Modifier.fillMaxSize(), submitLabel = Label.CREATE) {
                Form.Text(value = message, softWrap = true)
                TypeNamingField(formState.label) { formState.label = it }
                Field(label = Label.VALUE_TYPE) {
                    Form.Dropdown(
                        selected = formState.valueType,
                        values = valueTypes,
                        displayFn = { AnnotatedString(it.name.lowercase()) },
                        placeholder = Label.VALUE_TYPE,
                        onSelection = { formState.valueType = it },
                        enabled = supertypeState.isRoot
                    )
                }
            }
        }
    }


    @Composable
    private fun RenameTypeDialog() {
        val dialogState = StudioState.schema.renameTypeDialog
        val typeState = dialogState.typeState!!
        val message = Sentence.RENAME_TYPE.format(typeState.encoding.label, typeState.name)
        val formState = remember {
            TypeLabelForm(
                initField = typeState.name,
                isValid = { it != typeState.name },
                onCancel = { dialogState.close() },
                onSubmit = { typeState.tryRename(it) }
            )
        }
        TypeNamingDialog(dialogState, formState, Label.RENAME_TYPE, message, Label.RENAME)
    }

    @Composable
    private fun <T : TypeState.Thing> TypeNamingDialog(
        dialogState: SchemaManager.EditTypeDialog<T>, formState: TypeLabelForm,
        title: String, message: String, submitLabel: String
    ) {
        Dialog.Layout(dialogState, title, DIALOG_WIDTH, DIALOG_HEIGHT) {
            Submission(state = formState, modifier = Modifier.fillMaxSize(), submitLabel = submitLabel) {
                Form.Text(value = message, softWrap = true)
                TypeNamingField(formState.label) { formState.label = it }
            }
        }
    }

    @Composable
    private fun TypeNamingField(text: String, onChange: (String) -> Unit) {
        val focusReq = remember { FocusRequester() }
        Field(label = Label.TYPE_LABEL) {
            TextInput(
                value = text,
                placeholder = "type-label",
                onValueChange = onChange,
                modifier = Modifier.focusRequester(focusReq)
            )
        }
        LaunchedEffect(focusReq) { focusReq.requestFocus() }
    }

    @Composable
    private fun ChangeSupertypeDialog() {
        // TODO
    }
}