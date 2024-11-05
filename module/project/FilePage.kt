/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.module.project

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.typedb.studio.framework.common.theme.Theme
import com.typedb.studio.framework.editor.TextEditor
import com.typedb.studio.framework.material.Form
import com.typedb.studio.framework.material.Frame
import com.typedb.studio.framework.material.Icon
import com.typedb.studio.framework.material.Pages
import com.typedb.studio.framework.output.RunOutputArea
import com.typedb.studio.service.page.Pageable
import com.typedb.studio.service.project.FileState

class FilePage private constructor(
    private var file: FileState,
    private val editor: TextEditor.State
) : Pages.Page() {

    override val hasSecondary: Boolean = true
    override val icon: Form.IconArg
        get() = when {
            file.isTypeQL -> Form.IconArg(Icon.FILE_TYPEQL) { Theme.studio.secondary }
            else -> Form.IconArg(Icon.FILE_OTHER)
        }

    private var runOutputState: RunOutputArea.State? by mutableStateOf(null)

    companion object {
        @Composable
        fun create(file: FileState): FilePage {
            return FilePage(file, TextEditor.createState(file))
        }
    }

    override fun updatePageable(pageable: Pageable) {
        // TODO: guarantee that new file has identical content as previous, or update content.
        runOutputState?.let { it.pageable = pageable.asRunnable() }
        file = pageable as FileState
        editor.updateFile(file)
    }

    private fun runOutputState(paneState: Frame.PaneState): RunOutputArea.State {
        if (runOutputState == null) runOutputState = RunOutputArea.State(file, paneState)
        return runOutputState!!
    }

    @Composable
    override fun PrimaryContent() = TextEditor.Layout(editor, Modifier.fillMaxSize())

    @Composable
    override fun SecondaryContent(paneState: Frame.PaneState) = RunOutputArea.Layout(runOutputState(paneState))
}
