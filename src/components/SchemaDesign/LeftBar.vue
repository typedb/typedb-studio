<!--
 Copyright (C) 2021 Grakn Labs

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation, either version 3 of the
 License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <https://www.gnu.org/licenses/>.
-->

<template>
<div>
  <div class="left-bar-container noselect">
    <div class="panel-header">
      <h1>Define</h1>
    </div>
    <div class="content">
      <new-entity-panel :showPanel="showPanel" v-on:show-panel="togglePanel"></new-entity-panel>
      <new-attribute-panel :showPanel="showPanel" v-on:show-panel="togglePanel"></new-attribute-panel>
      <new-relation-panel :showPanel="showPanel" v-on:show-panel="togglePanel"></new-relation-panel>
      <new-rule-panel :showPanel="showPanel" v-on:show-panel="togglePanel"></new-rule-panel>
    </div>
  </div>
</div>
</template>

<style scoped>

  .left-bar-container {
    background-color: var(--gray-3);
    border-right: var(--container-darkest-border);
    height: 100%;
    position: relative;
    z-index: 1;
    display: flex;
    align-items: center;
    flex-direction: column;
  }

  .content { 
      display: flex;
      flex-direction: column;
      padding: var(--container-padding);
  }

  .panel-header {
    justify-content: center;
    cursor: default;
  }

</style>

<script>
  import { createNamespacedHelpers } from 'vuex';

  import NewEntityPanel from './LeftBar/NewEntityPanel';
  import NewAttributePanel from './LeftBar/NewAttributePanel';
  import NewRelationPanel from './LeftBar/NewRelationPanel';
  import NewRulePanel from './LeftBar/NewRulePanel';


  export default {
    components: { NewEntityPanel, NewAttributePanel, NewRelationPanel, NewRulePanel },
    data() {
      return {
        showPanel: undefined,
      };
    },
    beforeCreate() {
      const { mapGetters } = createNamespacedHelpers('schema-design');

      // computed
      this.$options.computed = {
        ...(this.$options.computed || {}),
        ...mapGetters(['currentDatabase']),
      };
    },
    watch: {
      currentDatabase() {
        this.showPanel = undefined;
      },
    },
    methods: {
      togglePanel(panel) {
        if (!this.currentDatabase) this.$emit('database-not-selected');
        else this.showPanel = panel;
      },
    },
  };
</script>
