/*
 * Copyright (C) 2022 Vaticle
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.vaticle.typedb.studio.module.project

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.vaticle.typedb.studio.framework.common.theme.Theme
import com.vaticle.typedb.studio.framework.editor.TextEditor
import com.vaticle.typedb.studio.framework.material.Form
import com.vaticle.typedb.studio.framework.material.Frame
import com.vaticle.typedb.studio.framework.material.Icon
import com.vaticle.typedb.studio.framework.material.Pages
import com.vaticle.typedb.studio.framework.output.RunOutputArea
import com.vaticle.typedb.studio.state.page.Pageable
import com.vaticle.typedb.studio.state.project.FileState

class FilePage private constructor(
    private var file: FileState,
    private val editor: TextEditor.State
) : Pages.Page() {

    override val hasSecondary: Boolean = true
    override val icon: Form.IconArg
        get() = when {
            file.isTypeQL -> Form.IconArg(Icon.Code.RECTANGLE_CODE) { Theme.studio.secondary }
            else -> Form.IconArg(Icon.Code.FILE_LINES)
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
    override fun PrimaryContent() {
        TextEditor.Layout(state = editor, modifier = Modifier.fillMaxSize())
    }

    @Composable
    override fun SecondaryContent(paneState: Frame.PaneState) {
        RunOutputArea.Layout(runOutputState(paneState))
    }
}
