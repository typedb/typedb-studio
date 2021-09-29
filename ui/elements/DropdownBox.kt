package com.vaticle.typedb.studio.ui.elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerIcon
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.ui.elements.IconSize.*

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun StudioDropdownBox() {
    var expanded by remember { mutableStateOf(false) }
    val suggestions = listOf("Item1", "Item2", "Item3")
    var selectedText by remember { mutableStateOf("grabl") }
    var textfieldSize by remember { mutableStateOf(Size.Zero)}

    Column {
        Row {
            StudioTextField(value = selectedText, onValueChange = { selectedText = it },
                modifier = Modifier.size(width = 120.dp, height = 24.dp)
                    .pointerIcon(PointerIcon.Default)
                    .onGloballyPositioned { coordinates ->
                    // This is used to keep the DropdownMenu the same width as the TextField
                    textfieldSize = coordinates.size.toSize()
                },
                textStyle = StudioTheme.typography.small,
                leadingIcon = {
                    StudioIcon(Icon.Database, size = Size16)
                },
                trailingIcon = {
                    StudioIcon(if (expanded) Icon.CaretUp else Icon.CaretDown)
                }
            )
        }

//        Row {
//            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
//                modifier = Modifier.width(with(LocalDensity.current) { textfieldSize.width.toDp() })) {
//
//                suggestions.forEach { label ->
//                    DropdownMenuItem(onClick = { selectedText = label }) {
//                        Text(label)
//                    }
//                }
//            }
//        }
    }
}
