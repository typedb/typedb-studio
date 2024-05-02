/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.vaticle.typedb.studio.service.page

interface Navigable<out T : Navigable<T>> : Comparable<Navigable<@UnsafeVariance T>> {
    val name: String
    val parent: Navigable<T>?
    val info: String?
    val isExpandable: Boolean
    val isBulkExpandable: Boolean
    val entries: List<T>
    fun reloadEntries()
}
