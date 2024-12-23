/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.service

import com.typedb.studio.service.common.ConfirmationService
import com.typedb.studio.service.common.DataService
import com.typedb.studio.service.common.EditorService
import com.typedb.studio.service.common.NotificationService
import com.typedb.studio.service.common.PreferenceService
import com.typedb.studio.service.common.StatusService
import com.typedb.studio.service.connection.DriverState
import com.typedb.studio.service.page.PageService
import com.typedb.studio.service.project.ProjectService
import com.typedb.studio.service.schema.SchemaService
import com.typedb.studio.service.user.UserService

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
    lateinit var user: UserService
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
        user = UserService(notification, driver)
        schema = SchemaService(driver.transaction, pages, notification, confirmation, status)
    }
}
