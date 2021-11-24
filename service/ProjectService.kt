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

package com.vaticle.typedb.studio.service

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ProjectService {

    // TODO: initialise from user data
    var pastDirectories: Set<String> by mutableStateOf(
        setOf(
            "/Users/haikalpribadi/Workspace/project-g",
            "/Users/haikalpribadi/Workspace/project-f",
            "/Users/haikalpribadi/Workspace/project-e",
            "/Users/haikalpribadi/Workspace/project-d",
            "/Users/haikalpribadi/Workspace/project-c",
            "/Users/haikalpribadi/Workspace/project-b",
            "/Users/haikalpribadi/Workspace/project-a"
        )
    )
    val currentDirectory: String? by mutableStateOf(null)
    var showWindow: Boolean by mutableStateOf(false)

    fun toggleWindow() {
        showWindow = !showWindow
    }
}
