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
import com.vaticle.typedb.studio.state.preference.PreferenceManager
import com.vaticle.typedb.studio.state.connection.ClientState
import com.vaticle.typedb.studio.state.page.PageManager
import com.vaticle.typedb.studio.state.project.ProjectManager
import com.vaticle.typedb.studio.state.schema.SchemaManager

object StudioState {

    val preference = PreferenceManager()
    val appData = DataManager()
    val editor = EditorManager()
    val status = StatusManager()
    val notification = NotificationManager()
    val confirmation = ConfirmationManager()
    val pages = PageManager()
    val client = ClientState(notification)
    val project = ProjectManager(preference, notification, confirmation, client, pages)
    val schema = SchemaManager(client.session, pages, notification)
}
