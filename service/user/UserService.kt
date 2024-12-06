/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typedb.studio.service.user

import com.typedb.driver.common.exception.TypeDBDriverException
import com.typedb.studio.service.common.NotificationService
import com.typedb.studio.service.common.util.DialogState
import com.typedb.studio.service.common.util.Message.Connection.Companion.FAILED_TO_UPDATE_PASSWORD
import com.typedb.studio.service.common.util.Message.Connection.Companion.PASSWORD_UPDATED_SUCCESSFULLY
import com.typedb.studio.service.connection.DriverState
import mu.KotlinLogging

class UserService(
    private val notificationSrv: NotificationService,
    private val driverSrv: DriverState
) {

    class UpdatePasswordDialogState : DialogState() {
        var onCancel: (() -> Unit)? = null; private set

        fun open(onCancel: (() -> Unit)? = null) {
            isOpen = true
            this.onCancel = onCancel
        }

        override fun close() {
            super.close()
            onCancel = null
        }
    }

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    val updatePasswordDialog = UpdatePasswordDialogState()

    fun mayUpdateDefaultPassword(onCancel: (() -> Unit)? = null) {
        if (needsToChangeDefaultPassword()) updatePasswordDialog.open(onCancel)
    }

    // TODO: implement properly once TypeDB 3.0 provides the functionality
    private fun needsToChangeDefaultPassword(): Boolean {
        return false
    }

    fun tryUpdatePassword(newPassword: String, onSuccess: (() -> Unit)?) {
        try {
            driverSrv.userManager!!.currentUser.updatePassword(newPassword)
            notificationSrv.info(LOGGER, PASSWORD_UPDATED_SUCCESSFULLY)
            onSuccess?.invoke()
            driverSrv.tryReconnectAsync(newPassword)
        } catch (e: TypeDBDriverException) {
            notificationSrv.userError(LOGGER, FAILED_TO_UPDATE_PASSWORD, e.message ?: e.toString())
        }
    }
}