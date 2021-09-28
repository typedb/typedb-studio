package com.vaticle.typedb.studio.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.data.QueryResponseStream
import com.vaticle.typedb.studio.ui.elements.Icon
import com.vaticle.typedb.studio.ui.elements.StudioIcon
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

@Composable
fun Toolbar(modifier: Modifier = Modifier, dbName: String) {

    Column(modifier = modifier.height(28.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioTheme.colors.panelSeparator)) {}
        Row(modifier = modifier.fillMaxHeight().background(StudioTheme.colors.background),
            verticalAlignment = Alignment.CenterVertically) {

            Spacer(modifier = modifier.width(8.dp))
            StudioIcon(Icon.Database)

            Spacer(modifier = modifier.weight(1F))
        }
        Row(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioTheme.colors.panelSeparator)) {}
    }
}
