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

package com.vaticle.typedb.studio.state.project

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.vaticle.typedb.studio.state.common.Message.Project.Companion.FILE_NOT_READABLE
import com.vaticle.typedb.studio.state.common.Message.System.Companion.ILLEGAL_CAST
import com.vaticle.typedb.studio.state.common.Property
import com.vaticle.typedb.studio.state.notification.NotificationManager
import com.vaticle.typedb.studio.state.page.Editable
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import mu.KotlinLogging

class File internal constructor(path: Path, parent: Directory, notificationMgr: NotificationManager) :
    ProjectItem(Type.FILE, path, parent, notificationMgr), Editable {

    val content: SnapshotStateList<String> = mutableStateListOf()
    val extension: String = this.path.extension
    val isTypeQL: Boolean = Property.File.TYPEQL.extensions.contains(extension)
    val isTextFile: Boolean = checkIsTextFile()

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    private fun checkIsTextFile(): Boolean {
        val type = Files.probeContentType(path)
        return type != null && type.startsWith("text")
    }

    override fun asDirectory(): Directory {
        throw TypeCastException(ILLEGAL_CAST.message(File::class.simpleName, Directory::class.simpleName))
    }

    override fun asFile(): File {
        return this
    }

    override fun tryOpen(): Boolean {
        return try {
            content.clear()
            if (isTextFile) loadTextFile()
            else loadBinaryFile()
            if (content.isEmpty()) content.add("")
            true
        } catch (e: Exception) { // TODO: specialise error message to actual error, e.g. read/write permissions
            notificationMgr.userError(LOGGER, FILE_NOT_READABLE, path)
            false
        }
    }

    private fun loadTextFile() {
        content.addAll(Files.readAllLines(path))
    }

    private fun loadBinaryFile() {
        val reader = BufferedReader(InputStreamReader(FileInputStream(path.toFile())))
        var line: String?
        while (reader.readLine().let { line = it; line } != null) content.add(line!!)
    }

    fun save() {
        Files.write(path, content)
    }

    override fun close() {
        content.clear()
    }
}
