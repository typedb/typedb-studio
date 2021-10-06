package com.vaticle.typedb.studio.ui.elements

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.ui.elements.IconSize.*

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun StudioDropdownBox(text: String, onTextChange: (value: String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val suggestions = listOf("Item1", "Item2", "Item3")
    var textfieldSize by remember { mutableStateOf(Size.Zero)}

    Column {
        Row {
            StudioTextField(value = text, onValueChange = { onTextChange(it) },
                modifier = Modifier.size(width = 96.dp, height = 24.dp)
                    .pointerIcon(PointerIcon.Default)
                    .onGloballyPositioned { coordinates ->
                    // This is used to keep the DropdownMenu the same width as the TextField
                    textfieldSize = coordinates.size.toSize()
                },
                textStyle = StudioTheme.typography.body2,
                leadingIcon = {
                    val pixelDensity = LocalDensity.current.density
                    when {
                        pixelDensity <= 1f -> Image(painter = painterResource("icons/database.png"),
                            contentDescription = "Database",
                            modifier = Modifier.graphicsLayer(scaleX = pixelDensity, scaleY = pixelDensity))
                        else -> Image(painter = loadSvgPainter(ClassLoader.getSystemResourceAsStream("icons/database.svg")!!, LocalDensity.current),
                            contentDescription = "Database",
                            modifier = Modifier.graphicsLayer(scaleX = 14f / 12f, scaleY = 14f / 12f))
                    }
//                    Image(painter = painterResource("icons/database.png"),
//                        contentDescription = "Database", modifier = Modifier.graphicsLayer(scaleX = pixelDensity, scaleY = pixelDensity))

//                    StudioIcon(Icon.Database, size = Size14)
                },
                trailingIcon = {
                    StudioIcon(if (expanded) Icon.CaretUp else Icon.CaretDown, size = Size16)
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
