package com.vaticle.typedb.studio.module.preference

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.framework.material.Dialog
import com.vaticle.typedb.studio.framework.material.Frame.Row
import com.vaticle.typedb.studio.module.connection.ServerDialog
import com.vaticle.typedb.studio.state.StudioState
import com.vaticle.typedb.studio.state.common.util.Label

object PreferenceDialog {

    private val WIDTH = 500.dp
    private val HEIGHT = 340.dp
    @Composable
    fun MayShowDialogs() {
        if (StudioState.preference.openPreferenceDialog.isOpen) Preferences()
    }

    @Composable
    private fun Preferences() {
        Dialog.Layout(StudioState.preference.openPreferenceDialog, Label.CONNECT_TO_TYPEDB, WIDTH, HEIGHT) {
//            Row { }/
        }
    }
}