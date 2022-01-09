/*
 * Copyright (C) 2021 Vaticle
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

package com.vaticle.typedb.studio.view.page

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vaticle.typedb.studio.state.project.File
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.theme.Theme

class FilePage(val file: File) : Page(file) {

    override val label: String = file.name
    override val icon: Form.IconArgs = when {
        file.isTypeQL -> Form.IconArgs(Icon.Code.RECTANGLE_CODE) { Theme.colors.secondary }
        else -> Form.IconArgs(Icon.Code.FILE_LINES)
    }

    @Composable
    override fun Layout() {
        FileEditor.Area(
            state = FileEditor.createState(file.content) { file.content = it },
            modifier = Modifier.fillMaxSize()
        )
    }
}
