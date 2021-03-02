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
    <div class="database-handler-panel">

        <button @click="showNewDatabasePanel = true" class="btn"><vue-icon icon="plus" className="vue-icon"></vue-icon></button>
        <div v-show="showNewDatabasePanel" class="new-database-container z-depth-3">
            <div class="new-database-header">
                <div class="preferences-title">Add New Database</div>
                <div class="close-container" @click="showNewDatabasePanel = false"><vue-icon icon="cross" iconSize="12" className="tab-icon"></vue-icon></div>
            </div>
            <div class="panel-content">
                <div class="label">Name</div>
                <div class="database-name-input"><input class="input-small" v-model="databaseName"></div>
            </div>
            <div class="row">
                <button @click="showNewDatabasePanel = false" class="btn">CANCEL</button>
                <loading-button v-on:clicked="addNewDatabase" text="SAVE" className="btn" :loading="loadSpinner"></loading-button>
            </div>
        </div>

        <div class="database-list">
            <div class="database-item" v-for="ks in allDatabases" :key="ks">
                <div class="database-label">
                    {{ks}}
                </div>
                <div class="right-side" @click="deleteDatabase(ks)" >
                    <vue-icon icon="trash" className="vue-icon delete-icon" iconSize="14"></vue-icon>
                </div>
            </div>
        </div>

    </div>
</template>
<style scoped>

    .database-handler-panel {
        display: flex;
        flex-direction: column;
        padding: var(--container-padding);
        width: 100%;
    }

    .database-item {
        margin: var(--element-margin);
        padding: var(--container-padding);
        background-color: var(--gray-3);
        display: flex;
        align-items: center;
        justify-content: space-between;
        height: 22px;
        border: var(--container-light-border)
    }

    .new-database-container {
        width: 500px;
        border: var(--container-darkest-border);
        position: absolute;
        background-color: var(--gray-2);
        top: 35%;
        right: 18.8%;
    }

    .new-database-header {
        height: 22px;
        background-color: var(--gray-1);
        display: flex;
        justify-content: center;
        align-items: center;
        border-bottom: var(--container-darkest-border);
    }

    .close-container {
        position: absolute;
        right: 2px;
    }

    .panel-content {
         display: flex;
         align-items: center;
         padding: 5px 10px 0px 10px;
     }

    .database-name-input {
        width: 100%;
    }

    .label {
        margin-right: 10px;
    }

    .row {
        display: flex;
        padding: 5px 5px 5px 10px;
        justify-content: flex-end;
    }

</style>



<script>
  import { mapGetters } from 'vuex';

  export default {
    name: 'DatabaseHandler',
    data() {
      return {
        showNewDatabasePanel: false,
        databaseName: '',
        loadSpinner: false,
      };
    },
    computed: {
      ...mapGetters(['allDatabases']),
    },
    methods: {
      addNewDatabase() {
        if (!this.databaseName.length) return;
        this.loadSpinner = true;
        this.$store.dispatch('createDatabase', this.databaseName)
          .then(() => { this.$notifyInfo(`New database, ${this.databaseName}, successfully created!`); })
          .catch((error) => { this.$notifyError(error, 'Create database'); })
          .then(() => { this.loadSpinner = false; this.databaseName = ''; this.showNewDatabasePanel = false; });
      },
      deleteDatabase(database) {
        this.$store.dispatch('deleteDatabase', database)
          .then(() => { this.$notifyInfo(`Database, ${database}, successfully deleted!`); })
          .catch((error) => { this.$notifyError(error, 'Delete database'); });
      },
    },
  };
</script>
