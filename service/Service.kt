/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.vaticle.typedb.studio.service

import com.vaticle.typedb.studio.service.common.ConfirmationService
import com.vaticle.typedb.studio.service.common.DataService
import com.vaticle.typedb.studio.service.common.EditorService
import com.vaticle.typedb.studio.service.common.NotificationService
import com.vaticle.typedb.studio.service.common.PreferenceService
import com.vaticle.typedb.studio.service.common.StatusService
import com.vaticle.typedb.studio.service.connection.DriverState
import com.vaticle.typedb.studio.service.page.PageService
import com.vaticle.typedb.studio.service.project.ProjectService
import com.vaticle.typedb.studio.service.schema.SchemaService

object Service {

    lateinit var preference: PreferenceService
    lateinit var data: DataService
    lateinit var editor: EditorService
    lateinit var status: StatusService
    lateinit var notification: NotificationService
    lateinit var confirmation: ConfirmationService
    lateinit var pages: PageService
    lateinit var driver: DriverState
    lateinit var project: ProjectService
    lateinit var schema: SchemaService

    init {
        init()
    }

    fun init() {
        data = DataService()
        preference = PreferenceService(data)
        editor = EditorService()
        status = StatusService()
        notification = NotificationService()
        confirmation = ConfirmationService()
        pages = PageService()
        driver = DriverState(notification, preference, data)
        project = ProjectService(preference, data, notification, confirmation, driver, pages)
        schema = SchemaService(driver.session, pages, notification, confirmation, status)
    }
}
