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

package com.vaticle.typedb.studio.state

import com.vaticle.typedb.studio.state.app.ConfirmationManager
import com.vaticle.typedb.studio.state.app.DataManager
import com.vaticle.typedb.studio.state.app.EditorManager
import com.vaticle.typedb.studio.state.app.NotificationManager
import com.vaticle.typedb.studio.state.app.StatusManager
import com.vaticle.typedb.studio.state.common.util.Settings
import com.vaticle.typedb.studio.state.connection.ClientStateImpl
import com.vaticle.typedb.studio.state.project.ProjectManager
import com.vaticle.typedb.studio.state.resource.ResourceManager

object GlobalState {

    val settings = Settings()
    val appData = DataManager()
    val editor = EditorManager()
    val confirmation = ConfirmationManager()
    val notification = NotificationManager()
    val status = StatusManager()
    val client = ClientStateImpl(notification)
    val project = ProjectManager(settings, notification)
    val resource = ResourceManager(notification)
}
