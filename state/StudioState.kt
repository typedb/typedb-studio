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

package com.vaticle.typedb.studio.state

import com.vaticle.typedb.studio.state.app.ConfirmationManager
import com.vaticle.typedb.studio.state.app.DataManager
import com.vaticle.typedb.studio.state.app.EditorManager
import com.vaticle.typedb.studio.state.app.NotificationManager
import com.vaticle.typedb.studio.state.app.StatusManager
import com.vaticle.typedb.studio.state.common.util.PreferenceManager
import com.vaticle.typedb.studio.state.connection.ClientState
import com.vaticle.typedb.studio.state.page.PageManager
import com.vaticle.typedb.studio.state.project.ProjectManager
import com.vaticle.typedb.studio.state.schema.SchemaManager

object StudioState {

    lateinit var preference: PreferenceManager
    lateinit var appData: DataManager
    lateinit var editor: EditorManager
    lateinit var status: StatusManager
    lateinit var notification: NotificationManager
    lateinit var confirmation: ConfirmationManager
    lateinit var pages: PageManager
    lateinit var client: ClientState
    lateinit var project: ProjectManager
    lateinit var schema: SchemaManager

    init {
        init()
    }

    fun init() {
        preference = PreferenceManager()
        appData = DataManager()
        editor = EditorManager()
        status = StatusManager()
        notification = NotificationManager()
        confirmation = ConfirmationManager()
        pages = PageManager()
        client = ClientState(notification)
        project = ProjectManager(preference, notification, confirmation, client, pages)
        schema = SchemaManager(client.session, pages, notification)
    }
}
