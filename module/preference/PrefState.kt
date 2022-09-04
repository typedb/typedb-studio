package com.vaticle.typedb.studio.module.preference;

import com.vaticle.typedb.studio.state.page.Navigable

class PrefState(
    override val name: String,
    override val entries: List<PrefState> = emptyList(),
) : Navigable<PrefState> {

    override val parent: Navigable<PrefState>? = null
    override val info: String? = null
    override val isExpandable = true
    override val isBulkExpandable = false

    override fun reloadEntries() {}

    override fun compareTo(other: Navigable<PrefState>): Int {
      return this.name.compareTo(other.name);
    }
}
