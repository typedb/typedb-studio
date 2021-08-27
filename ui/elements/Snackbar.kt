package com.vaticle.typedb.studio.ui.elements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.appearance.StudioTheme

@Composable
fun StudioSnackbarHost(snackbarHostState: SnackbarHostState){

    SnackbarHost(
        hostState = snackbarHostState,
        snackbar = {
            // TODO: it's too big
            Snackbar (
                action = {
                    Text(
                        text = snackbarHostState.currentSnackbarData?.actionLabel ?: "",
                        modifier = Modifier
                            .padding(16.dp)
                            .clickable {
                                snackbarHostState.currentSnackbarData?.dismiss()
                            },
                        style = StudioTheme.typography.body1.copy(color = StudioTheme.colors.onPrimary, fontWeight = FontWeight.SemiBold)
                    )
                },
            ) {
                Text(
                    text = snackbarHostState.currentSnackbarData?.message ?: "",
                    style = StudioTheme.typography.body1.copy(color = StudioTheme.colors.onPrimary))
            }
        },
    )
}
