package com.vaticle.typedb.studio.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun FormFieldGroup(content: @Composable () -> Unit) = Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    content()
}
