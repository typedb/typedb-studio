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

package com.vaticle.typedb.studio.session

import com.vaticle.typedb.client.TypeDB
import com.vaticle.typedb.client.api.connection.TypeDBClient
import com.vaticle.typedb.client.api.connection.TypeDBCredential
import java.nio.file.Path

class ClusterClient(override val serverAddress: String, val username: String, password: String, rootCAPath: String?): BaseClient() {

    override val typeDBClient: TypeDBClient = if (rootCAPath.isNullOrBlank()) {
        TypeDB.clusterClient(serverAddress, TypeDBCredential(username, password, false))
    } else {
        TypeDB.clusterClient(serverAddress, TypeDBCredential(username, password, true, Path.of(rootCAPath)))
    }
}
