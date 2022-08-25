package com.vaticle.typedb.studio.state.preference

import com.vaticle.typedb.studio.state.page.Navigable

class Preference(val _name: String): Navigable<Preference> {
    override val name = _name
    val parent: Navigable<T>?
    val info: String?
    val isExpandable: Boolean
    val isBulkExpandable: Boolean
    val entries: List<T>
    fun reloadEntries()
}