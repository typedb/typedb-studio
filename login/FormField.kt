package com.vaticle.typedb.studio.login

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment

@Composable
fun FormField(content: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        content()
    }
}
