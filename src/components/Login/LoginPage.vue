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
  <transition name="slide-fade" appear>
    <div class="wrapper noselect" v-if="showLoginPage">
      <div class="login-header">
        <img src="img/grakn-workbase-logo.png" class="logo">
      </div>

      <div class="login-panel">
        <div class="header">
          <div class="header-item" :class="isCluster ? '' : 'header-item-selected'" @click="chooseCore">Grakn Core</div>
          <div class="header-item" :class="isCluster ? 'header-item-selected' : ''" @click="chooseCluster">Grakn Cluster</div>
        </div>
        <div class="row">
          <div class="column-2">
            <div class="row">
              <h1 class="label">Host:</h1>
              <input class="input left-input" v-model="serverHost">
            </div>
          </div>
          <div class="column-2">
            <div class="row">
              <h1 class="label">Port:</h1>
              <input class="input" type="number" v-model="serverPort">
            </div>
            <div class="row flex-end">
              <loading-button v-on:clicked="connect()" :text="isCluster ? 'Connect to Grakn Cluster' : 'Connect to Grakn Core'" :loading="isLoading" className="btn login-btn"></loading-button>
            </div>
          </div>
        </div>
      </div>
      
    </div>
  </transition>
</template>
<style scoped>
  .arrow-left {
    padding-right: 2px;
  }

  .flex-end {
    justify-content: flex-end;
  }

  .non-btn {
    background-color: var(--gray-2) !important;
    border: 0px;
    padding-left: 0px;
  }

  .input {
    width: 100%;
  }

  .column-1 {
    display: flex;
    flex-direction: column;
    width: 100%;
    height: 120px;
  }

  .column-2 {
    display: flex;
    flex-direction: column;
    width: 100%;
    height: 80px;
  }

  .row {
    display: flex;
    flex-direction: row;
    align-items: center;
    padding: var(--container-padding);
  }

  .header {
    height: 22px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-bottom: var(--container-darkest-border);
  }

  .header-item {
    flex: 1;
    text-align: center;
    cursor: pointer;
    height: 100%;
    padding-top: 5px;
    background-color: var(--gray-1);
  }

  .header-item-selected {
    background-color: var(--gray-2);
  }

  .label {
    margin-right: 5px;
    width: 85px;
  }

  .login-panel {
    margin-top: 50px;
    border: var(--container-darkest-border);
    display: flex;
    flex-direction: column;
    background-color: var(--gray-2);
    width: 384px;
  }

  .btn-row {
    display: flex;
    flex-direction: row;
    width: 100%;
  }

  .slide-fade-enter-active {
      transition: all 1s ease;
  }
  .slide-fade-enter,
  .slide-fade-leave-active {
      opacity: 0;
  }

  .logo {
    width: 100%;
    margin-top: 50px;
  }

  .login-header {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    width: 384px;
  }

  .wrapper{
    display: flex;
    flex-direction: column;
    align-items: center;
    padding-top: 30px;
  }

</style>
<script>
import ServerSettings from '@/components/ServerSettings';

export default {
  name: 'LoginPage',
  data() {
    return {
      isLoading: false,
      serverHost: ServerSettings.getServerHost(),
      serverPort: ServerSettings.getServerPort(),
      username: '',
      password: '',
      showLoginPage: true,
      isCluster: false,
    };
  },
  watch: {
    serverHost(newVal) {
      ServerSettings.setServerHost(newVal);
    },
    serverPort(newVal) {
      ServerSettings.setServerPort(newVal);
    },
  },
  created() {
    window.addEventListener('keyup', (e) => {
      if (e.keyCode === 13 && !e.shiftKey && this.serverHost && this.serverPort) this.connect();
    });
  },
  mounted() {
    this.$nextTick(() => {
      this.serverHost = ServerSettings.getServerHost();
      this.serverPort = ServerSettings.getServerPort();
    });
  },
  methods: {
    chooseCore() {
      this.isCluster = false;
    },

    chooseCluster() {
      this.isCluster = true;
    },

    async connect() {
      this.$toasted.clear();
      this.$store.dispatch('initGrakn', this.isCluster);
      this.$router.push('develop/data');
    },
  },
};
</script>
