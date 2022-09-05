package com.vaticle.typedb.studio.module.preference;

import androidx.compose.runtime.Composable
import com.vaticle.typedb.studio.state.page.Navigable

class PreferenceGroup(
    override val name: String,
    override val entries: List<PreferenceGroup> = emptyList(),
    val content: @Composable () -> Unit = {},
) : Navigable<PreferenceGroup> {

    override val parent: Navigable<PreferenceGroup>? = null
    override val info: String? = null
    override val isExpandable = entries.isNotEmpty()
    override val isBulkExpandable = false

    override fun reloadEntries() {}

    override fun compareTo(other: Navigable<PreferenceGroup>): Int {
      return this.name.compareTo(other.name);
    }

    @Composable
    fun showContent() {
        content()
    }
}
