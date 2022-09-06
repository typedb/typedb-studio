package com.vaticle.typedb.studio.module.preference;

import androidx.compose.runtime.Composable
import com.vaticle.typedb.studio.state.page.Navigable

class PreferenceGroup(
    override val name: String,
    override var entries: List<PreferenceGroup> = emptyList(),
    val content: @Composable () -> Unit = {},
) : Navigable<PreferenceGroup> {

    override var parent: Navigable<PreferenceGroup>? = null
    override val info: String? = null
    override val isExpandable = entries.isNotEmpty()
    override val isBulkExpandable = false
    val isRoot: Boolean
        get() = parent == null

    override fun reloadEntries() {}

    override fun compareTo(other: Navigable<PreferenceGroup>): Int {
      return this.name.compareTo(other.name);
    }

    private fun addParent(parent: PreferenceGroup) {
        this.parent = parent
    }

    fun addEntry(entry: PreferenceGroup) {
        this.entries = entries + entry
        entry.addParent(this)
    }
}
