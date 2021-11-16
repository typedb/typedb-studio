package com.vaticle.typedb.studio.common.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.common.theme.Theme

object Separator {

    private val WEIGHT = 1.dp

    @Composable
    fun Horizontal() {
        Spacer(
            modifier = Modifier.fillMaxWidth().height(WEIGHT).background(Theme.colors.uiElementBorder)
        )
    }

    @Composable
    fun Vertical() {
        Spacer(
            modifier = Modifier.fillMaxHeight().width(WEIGHT).background(Theme.colors.uiElementBorder)
        )
    }
}