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
    <div class="panel-container noselect">
        <div v-show="showConnectionSettings">
            <div class="panel-content">
                <div class="panel-content-item">
                    <h1 class="panel-label">Host:</h1>
                    <input class="input-small panel-value" v-model="serverHost">
                </div>
                <div class="panel-content-item">
                    <h1 class="panel-label">Port:</h1>
                    <input class="input-small panel-value" type="number" v-model="serverPort">
                </div>
            </div>
        </div>
    </div>
</template>

<script>
  import Settings from '../ServerSettings';

  export default {

    name: 'ConnectionManager',
    data() {
      return {
        showConnectionSettings: true,
        serverHost: Settings.getServerHost(),
        serverPort: Settings.getServerPort(),
      };
    },
    mounted() {
      this.$nextTick(() => {
        this.serverHost = Settings.getServerHost();
        this.serverPort = Settings.getServerPort();
      });
    },
    watch: {
      serverHost(newVal) {
        Settings.setServerHost(newVal);
      },
      serverPort(newVal) {
        Settings.setServerPort(newVal);
      },
    },
    methods: {
      toggleContent() {
        this.showConnectionSettings = !this.showConnectionSettings;
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
    }

    .panel-content-item {
        padding: var(--container-padding);
        display: flex;
        flex-direction: row;
        align-items: center;
        height: var(--line-height);
        justify-content: center;
    }

    .panel-label {
        width: 40px;
    }

    .panel-value {
        width: 100px;
        justify-content: center;
        display: flex;
    }

</style>
