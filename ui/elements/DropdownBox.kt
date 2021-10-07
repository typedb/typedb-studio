package com.vaticle.typedb.studio.ui.elements

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerIcon
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.ui.elements.IconSize.*

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun StudioDropdownBox(text: String, onTextChange: (value: String) -> Unit, modifier: Modifier = Modifier,
                      textStyle: TextStyle = StudioTheme.typography.body1, leadingIcon: (@Composable () -> Unit)? = null) {
    var expanded by remember { mutableStateOf(false) }
    val suggestions = listOf("Item1", "Item2", "Item3")
    var textfieldSize by remember { mutableStateOf(Size.Zero)}

    Column(modifier) {
        Row {
            StudioTextField(value = text, onValueChange = { onTextChange(it) },
                modifier = Modifier.fillMaxSize()
                    .pointerIcon(PointerIcon.Default)
                    .clickable { expanded = !expanded }
                    .onGloballyPositioned { coordinates ->
                    // This is used to keep the DropdownMenu the same width as the TextField
                    textfieldSize = coordinates.size.toSize()
                }, readOnly = true, textStyle = textStyle, leadingIcon = leadingIcon,
                trailingIcon = { StudioIcon(Icon.CaretDown, size = Size16) })
        }

        Row {
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
                modifier = Modifier.width(with(LocalDensity.current) { textfieldSize.width.toDp() })
                    .background(StudioTheme.colors.uiElementBackground)) {

                suggestions.forEach { label ->
                    DropdownMenuItem(onClick = { onTextChange(label) }, contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(28.dp)) {
                        Text(label, style = textStyle)
                    }
                }
            }
        }
    }
}
