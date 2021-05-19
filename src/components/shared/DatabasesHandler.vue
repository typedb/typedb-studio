<!--
 Copyright (C) 2021 Vaticle

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
<div class="databases-wrapper">

    <button :class="(this.showDatabaseList) ? 'btn databases database-btn' : 'btn databases'" @click="toggleDatabaseList">
        <div>{{currentDatabase | truncate}}</div>
        <vue-icon icon="database" className="vue-icon database-icon"></vue-icon>
    </button>

    <tool-tip class="database-tooltip" msg="Please select a database" :isOpen="showDatabaseTooltip" arrowPosition="right"></tool-tip>

        <ul id="databases-list" class="databases-list arrow_box z-depth-1" v-if="showDatabaseList">
            <div style="text-align:center;" v-if="allDatabases && !allDatabases.length">no existing database</div>
            <input class="input-small" v-model="searchedDatabase" placeholder="search">
            <li :id="ks" v-bind:class="(ks === currentDatabase)? 'ks-key active noselect' : 'ks-key noselect'" v-for="ks in databaseList" :key="ks" @click="setDatabase(ks)">{{ks}}</li>
        </ul>
</div>
</template>

<style scoped lang="scss">

    .databases-wrapper {
      z-index: 3;
    }

    .database-tooltip {
        right: 136px;
        top: 8px;
    }

    .databases {
        display: flex;
        > * {
            padding-top: 5px;
        }
    }

    .arrow_box {
        position: relative;
        background: var(--gray-1);
        border: var(--container-darkest-border);
    }
    .arrow_box:after, .arrow_box:before {
        bottom: 100%;
        left: 50%;
        border: solid transparent;
        content: " ";
        height: 0;
        width: 0;
        position: absolute;
        pointer-events: none;
    }

    .arrow_box:after {
        border-bottom-color: var(--gray-1);
        border-width: 10px;
        margin-left: -10px;
    }
    .arrow_box:before {
        border-bottom-color: var(--border-darkest-color);
        border-width: 11px;
        margin-left: -11px;
    }


.databases-list {
    position: absolute;
    top: 100%;
    margin-top: 5px;
    /*padding: 5px 10px;*/
    right:5px;
    background-color: #282828;
    z-index: 3;
    min-width: 100px;
    max-width:  130px;
    word-break: break-word;
    background-color: var(--gray-1);
    border: var(--container-darkest-border);
}

/*dynamic class*/

    .ks-key {
        position: relative;
        cursor: pointer;
        padding: 5px;
        min-height: 22px;

    }

    .ks-key:hover {
        background-color: var(--purple-4);
    }

</style>

<script>
import { createNamespacedHelpers, mapGetters } from 'vuex';

import storage from '@/components/shared/PersistentStorage';

import { CURRENT_DATABASE_CHANGED } from './StoresActions';
import ToolTip from '../UIElements/ToolTip';

export default {
  name: 'DatabasesList',
  props: ['tabId', 'showDatabaseTooltip'],
  components: { ToolTip },
  data() {
    return {
      showDatabaseList: false,
      clickEvent: () => {
        this.showDatabaseList = false;
      },
      searchedDatabase: '',
    };
  },
  beforeCreate() {
    const namespace = (this.$route.path === '/develop/data') ? `tab-${this.$options.propsData.tabId}` : 'schema-design';
    const { mapGetters, mapActions } = createNamespacedHelpers(namespace);

    // computed
    this.$options.computed = {
      ...(this.$options.computed || {}),
      ...mapGetters(['currentDatabase', 'showSpinner']),
    };

    // methods
    this.$options.methods = {
      ...(this.$options.methods || {}),
      ...mapActions([CURRENT_DATABASE_CHANGED]),
    };
  },
  computed: {
    ...mapGetters(['allDatabases', 'isTypeDBRunning']),
    databaseList() {
      return this.allDatabases.filter(database => database.toLowerCase().includes(this.searchedDatabase.toLowerCase()));
    },
  },
  filters: {
    truncate(ks) {
      if (!ks) return 'database';
      if (ks.length > 15) return `${ks.substring(0, 15)}...`;
      return ks;
    },
  },
  watch: {
    allDatabases(val) {
      // If user deletes current database from Databases page, set new current database to null
      if (val && !val.includes(this.currentDatabase)) { this[CURRENT_DATABASE_CHANGED](null); }
    },
    isTypeDBRunning(val) {
      if (!val) {
        this.$notifyInfo('It was not possible to retrieve databases <br> - make sure TypeDB is running <br> - check that host and port in connection settings are correct');
      }
    },
    // showDatabaseList(show) {
    //   // Close databases list when user clicks anywhere else
    //   if (show) window.addEventListener('click', this.clickEvent);
    //   else window.removeEventListener('click', this.clickEvent);
    // },
  },
  methods: {
    setDatabase(name) {
      this.$emit('database-selected');
      storage.set('current_database_data', name);
      this[CURRENT_DATABASE_CHANGED](name);
      this.showDatabaseList = false;
    },
    toggleDatabaseList() {
      if (this.showSpinner) {
        this.$notifyInfo('Please wait for action to complete');
      } else {
        this.$emit('database-selected');
        this.showDatabaseList = !this.showDatabaseList;
      }
    },
  },
};
</script>
