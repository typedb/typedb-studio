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
    <div class="panel-container noselect">
        <div @click="toggleContent" class="panel-header">
            <vue-icon :icon="(showQuerySettings) ?  'chevron-down' : 'chevron-right'" iconSize="14" className="vue-icon"></vue-icon>
            <h1>Query Settings</h1>
        </div>
        <div v-show="showQuerySettings">

        <div class="panel-content">
            <div class="panel-content-item">
                <h1 class="panel-label">Query Limit:</h1>
                <input class="input-small panel-value query-limit-input" type="number" v-model="queryLimit">
            </div>
            <div class="panel-content-item">
                <h1 class="panel-label">Neighbour Limit:</h1>
                <input class="input-small panel-value neighbour-limit-input" type="number" v-model="neighboursLimit">
            </div>
            <div class="panel-content-item">
                <h1 class="panel-label">Load Roleplayers:</h1>
                <div class="panel-value load-roleplayers-switch"><vue-switch :isToggled="loadRolePlayers" v-on:toggled="updateLoadRoleplayers"></vue-switch></div>
            </div>
            <div class="panel-content-item">
                <h1 class="panel-label">Reasoning:</h1>
                <div class="panel-value reasoning-switch"><vue-switch :isToggled="reasoning" v-on:toggled="updateReasoning"></vue-switch></div>
            </div>
        </div>
        </div>
    </div>
</template>

<script>

  import QueryUtils from './QuerySettings';
  import { REOPEN_GLOBAL_GRAKN_TX } from '@/components/shared/StoresActions';
  import { createNamespacedHelpers } from 'vuex';
  import getters from "../../../Visualiser/store/getters";
  import state from "../../../Visualiser/store/tabState";
  import mutations from "../../../Visualiser/store/mutations";
  import actions from "../../../Visualiser/store/actions";

  export default {

    name: 'QuerySettings',
    data() {
      return {
        showQuerySettings: true,
        queryLimit: QueryUtils.getQueryLimit(),
        neighboursLimit: QueryUtils.getNeighboursLimit(),
        loadRolePlayers: QueryUtils.getRolePlayersStatus(),
        reasoning: QueryUtils.getReasoning(),
      };
    },
    beforeCreate() {
      if (!this.$store.state.hasOwnProperty('query-settings')) {
        this.$store.registerModule('query-settings', { namespaced: true, getters, state, mutations, actions });
      }
      const { mapActions } = createNamespacedHelpers('query-settings');

      // methods
      this.$options.methods = {
          ...(this.$options.methods || {}),
          ...mapActions([REOPEN_GLOBAL_GRAKN_TX]),
      };
    },
    watch: {
      queryLimit(newVal) {
        QueryUtils.setQueryLimit(newVal);
      },
      neighboursLimit(newVal) {
        QueryUtils.setNeighboursLimit(newVal);
      },
    },
    methods: {
      toggleContent() {
        this.showQuerySettings = !this.showQuerySettings;
      },
      updateLoadRoleplayers(newVal) {
        QueryUtils.setRolePlayersStatus(newVal);
      },
      updateReasoning(newVal) {
        QueryUtils.setReasoning(newVal);
        this[REOPEN_GLOBAL_GRAKN_TX]().catch((err) => { this.$notifyError(err, 'Failed to reopen transaction'); });
      },
    },
  };
</script>

<style scoped>


    .panel-content {
        padding: var(--container-padding);
        border-bottom: var(--container-darkest-border);
        display: flex;
        flex-direction: column;
        z-index: 10;
    }

    .panel-content-item {
        padding: var(--container-padding);
        display: flex;
        flex-direction: row;
        align-items: center;
        height: var(--line-height);
    }

    .panel-label {
        width: 90px;
    }

    .panel-value {
        width: 100px;
        justify-content: center;
        display: flex;
    }

</style>
