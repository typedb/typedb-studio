package com.vaticle.typedb.studio.common.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.common.component.Form.Text
import com.vaticle.typedb.studio.common.theme.Theme

object Notification {

    private const val POPUP_MARGIN = 120
    private const val POPUP_WIDTH = 350
    private const val POPUP_HEIGHT = 76
    private val POPUP_SHAPE = RoundedCornerShape(4.dp)
    private const val POPUP_PADDING = 8
    private const val popupContentWidth = POPUP_WIDTH - POPUP_PADDING * 2

    private fun popupYOffset(index: Int): Int {
        return POPUP_MARGIN + (POPUP_HEIGHT + POPUP_MARGIN) * index
    }

    @Composable
    fun Popup(text: String, index: Int, modifier: Modifier = Modifier) {
        androidx.compose.ui.window.Popup(
            alignment = Alignment.BottomEnd,
            offset = -IntOffset(
                (POPUP_MARGIN / LocalDensity.current.density).toInt(),
                (popupYOffset(index) / LocalDensity.current.density).toInt()
            )
        ) {
            Box(
                modifier = modifier
                    .size(POPUP_WIDTH.dp, POPUP_HEIGHT.dp)
                    .background(color = Theme.colors.error, shape = POPUP_SHAPE)
                    .padding(POPUP_PADDING.dp)
            ) {
                Text(value = text, color = Theme.colors.onError, modifier = Modifier.width(popupContentWidth.dp))
            }
        }
    }
}
