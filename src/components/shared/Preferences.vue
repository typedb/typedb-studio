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
    <div class="preferences-container z-depth-5">
        <div class="preferences-header">
            <div class="preferences-title">Preferences</div>
            <div class="close-container" @click="$emit('close-preferences')"><vue-icon icon="cross" iconSize="12" className="tab-icon"></vue-icon></div>
        </div>
        <div class="preferences-content">


            <div class="connection-container">
                <div class="container-title">
                    Connection
                </div>
                <div class="connection-content">
                    <h1 class="connection-label">Host:</h1>
                    <input class="input" v-model="serverHost">
                    <h1 class="connection-label port">Port:</h1>
                    <input class="input" type="number" v-model="serverPort">
                    <loading-button v-on:clicked="testConnection" :text="connectionTest" className="btn test-btn" :loading="connectionTest === 'testing'" :disabled="connectionTest !== 'Test'"></loading-button>
                </div>
            </div>

            <div class="databases-container">
                <div class="container-title">
                    Databases
                </div>
                <div class="databases-content">
                    
                    <div class="databases-list">
                        <div class="database-item" :class="(index % 2) ? 'even' : 'odd'" v-for="(ks,index) in allDatabases" :key="index">
                            <div class="database-label">
                                {{ks}}
                            </div>
                            <div class="right-side delete-database-btn" @click="deleteDatabase(ks)" >
                                <vue-icon icon="trash" className="vue-icon delete-icon" iconSize="14"></vue-icon>
                            </div>
                        </div>
                    </div>
                    <div class="new-database">
                        <input class="input database-input" v-model="databaseName" placeholder="Database name">
                        <loading-button v-on:clicked="addNewDatabase" text="Create New Database" className="btn new-database-btn" :loading="loadSpinner"></loading-button>
                    </div>
                </div>
            </div>


            <div class="logout-container" v-if="userLogged">
                <div class="databases-content">
                    <button class="btn" @click="logout">Logout</button>

                </div>
            </div>

        </div>
    </div>
</template>
<style scoped>
    .odd {
        background-color:var(--gray-1);
    }

    .database-input {
        width: 100%;
    }

    .new-database {
        display: flex;
        align-items: center;
        width: 100%;
        padding-top: var(--container-padding);
    }

    .databases-list {
        display: flex;
        flex-direction: column;
        width: 100%;
        border: var(--container-darkest-border);
        max-height: 400px;
        overflow: auto;
    }

    .databases-list::-webkit-scrollbar {
        width: 2px;
    }

    .databases-list::-webkit-scrollbar-thumb {
        background: var(--green-4);
    }

    .database-item {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: var(--container-padding);
        min-height: 30px;
    }

    .databases-content {
        padding: var(--container-padding);
        display: flex;
        flex-direction: column;
    }

    .preferences-container {
        position: absolute;
        width: 400px;
        background-color: var(--gray-2);
        top: 10%;
        right: 36%;
        z-index: 2;
        border: var(--container-darkest-border);
        display: flex;
        flex-direction: column;
    }

    .preferences-header {
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

    .preferences-content {
        display: flex;
        flex-direction: column;
        width: 100%;
    }

    .connection-container {
        padding: var(--container-padding);
        width: 100%;
        border-bottom: var(--container-darkest-border);
    }

    .databases-container {
        padding: var(--container-padding);
        width: 100%;
        border-bottom: var(--container-darkest-border);
    }

    .logout-container {
        padding: var(--container-padding);
        width: 100%;
    }

    .container-title {
        padding: var(--container-padding);
    }

    .connection-content {
        padding: var(--container-padding);
        display: flex;
        align-items: center;
        justify-content: space-between;
    }

    .test-btn {
        margin: 0px;
        width: 50px;
    }

    .Valid {
        color: var(--green-4);
    }

    .Invalid {
        color: var(--red-4);
    }

</style>

<script>
import { mapGetters } from 'vuex';
import Settings from '../ServerSettings';


export default {
  name: 'Preferences',
  data() {
    return {
      serverHost: Settings.getServerHost(),
      serverPort: Settings.getServerPort(),
      connectionTest: '',
      databaseName: '',
      loadSpinner: false,
    };
  },
  mounted() {
    this.$nextTick(() => {
      this.serverHost = Settings.getServerHost();
      this.serverPort = Settings.getServerPort();
    });
  },
  created() {
    this.connectionTest = (this.isTypeDBRunning) ? 'Valid' : 'Invalid';
  },
  computed: {
    ...mapGetters(['isTypeDBRunning', 'allDatabases', 'userLogged']),
  },
  watch: {
    serverHost(newVal) {
      this.connectionTest = 'Test';
      Settings.setServerHost(newVal);
    },
    serverPort(newVal) {
      this.connectionTest = 'Test';
      Settings.setServerPort(newVal);
    },
    isTypeDBRunning(newVal) {
      this.connectionTest = (newVal) ? 'Valid' : 'Invalid';
    },
  },
  methods: {
    testConnection() {
      this.connectionTest = 'testing';
      this.$store.dispatch('initTypeDB');
    },
    addNewDatabase() {
      if (!this.databaseName.length) return;
      this.loadSpinner = true;
      this.$store.dispatch('createDatabase', this.databaseName)
        .then(() => { this.$notifyInfo(`New database, ${this.databaseName}, successfully created!`); })
        .catch((error) => { this.$notifyError(error, 'Create database'); })
        .then(() => { this.loadSpinner = false; this.databaseName = ''; this.showNewDatabasePanel = false; });
    },
    deleteDatabase(database) {
      this.$notifyConfirmDelete(`Are you sure you want to delete ${database} database?`,
        () => this.$store.dispatch('deleteDatabase', database)
          .then(() => this.$notifyInfo(`Database, ${database}, successfully deleted!`))
          .catch((error) => { this.$notifyError(error, 'Delete database'); }));
    },
    async logout() {
      await this.$store.dispatch('logout');
      this.$router.push('/login');
    },
  },
};
</script>
