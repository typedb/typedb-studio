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

      <div class="login-panel" v-if="showLoginPanel">
        <div class="header">
          Connection to Grakn Cluster
        </div>
        <div class="row">
          <div class="column-1">
            <div class="row">
              <h1 class="label">Host:</h1>
              <input class="input left-input" v-model="serverHost">
            </div>
            <div class="row">
              <h1 class="label">Username:</h1>
              <input class="input left-input" v-model="username">
            </div>
          </div>
          <div class="column-1">
            <div class="row">
              <h1 class="label">Port:</h1>
              <input class="input" type="number" v-model="serverPort">
            </div>
            <div class="row">
              <h1 class="label">Password:</h1>
              <input class="input" type="password" v-model="password">
            </div>
            <div class="row flex-end">
              <loading-button v-on:clicked="loginToCluster()" text="Login" :loading="isLoading" className="btn login-btn"></loading-button>
            </div>
          </div>
        </div>
      </div>

      <div class="login-panel" v-if="showConnectionPanel">
        <div class="header">
          Connection
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
              <loading-button v-on:clicked="connectToCore()" text="Connect" :loading="isLoading" className="btn login-btn"></loading-button>
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
    background-color: var(--gray-1);
    height: 22px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-bottom: var(--container-darkest-border);
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
import storage from '@/components/shared/PersistentStorage';
import ServerSettings from '@/components/ServerSettings';

export default {
  name: 'LoginPage',
  data() {
    return {
      username: '',
      password: '',
      isLoading: false,
      serverHost: ServerSettings.getServerHost(),
      serverPort: ServerSettings.getServerPort(),
      showLoginPage: true,
      showLoginPanel: false,
      showConnectionPanel: true,
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
      if (e.keyCode === 13 && !e.shiftKey && this.username.length && this.password.length) this.loginToCluster();
    });
  },
  mounted() {
    this.$nextTick(() => {
      this.serverHost = ServerSettings.getServerHost();
      this.serverPort = ServerSettings.getServerPort();
    });
  },
  methods: {
    async loginToCluster() {
      this.$toasted.clear();
      this.$store.dispatch('login', { username: this.username, password: this.password });
      storage.set('user-credentials', JSON.stringify({ username: this.username, password: this.password }));
      this.$router.push('develop/data');
    },
    async connectToCore() {
      this.$toasted.clear();
      this.$store.dispatch('initGrakn');
      this.$router.push('develop/data');
    },
  },
};
</script>
