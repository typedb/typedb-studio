package com.vaticle.typedb.studio.module.preference

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.framework.material.Dialog
import com.vaticle.typedb.studio.framework.material.Form
import com.vaticle.typedb.studio.state.StudioState
import com.vaticle.typedb.studio.state.common.util.Label

object PreferenceDialog {

    private val WIDTH = 600.dp
    private val HEIGHT = 600.dp
    @Composable
    fun MayShowDialogs() {
        if (StudioState.preference.openPreferenceDialog.isOpen) Preferences()
    }

    @Composable
    private fun Preferences() {
        val dialogState = StudioState.preference.openPreferenceDialog
        Dialog.Layout(dialogState, Label.MANAGE_PREFERENCES, WIDTH, HEIGHT) {
            Row(modifier = Modifier.fillMaxSize()) {
                Form.Text(value = "test", softWrap = true)
                Form.TextButton("Accept") {}
                Form.TextButton("Discard Changes") {}
            }
        }
    }
}