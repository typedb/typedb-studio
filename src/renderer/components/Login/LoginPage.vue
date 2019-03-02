<template>
  <transition name="slide-fade" appear>
    <div class="wrapper noselect" v-if="showLoginPage">
      <div class="login-header">
        <img src="static/img/logo-text.png" class="icon">
        <div v-if="showConnectionPanel" class="workbase3">WORKBASE</div>
        <div v-else class="workbase2">WORKBASE FOR KGMS</div>
      </div>

      <div class="login-panel" v-if="showLoginPanel">
        <div class="header">
          Log In
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
              <loading-button v-on:clicked="loginToKgms()" text="Login" :loading="isLoading" className="btn login-btn"></loading-button>
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

  .icon {
    width: 250px;
    margin-top: 50px;
  }

  .login-header {
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 400px;
  }

   .workbase3 {
    right: 30%;
    font-size: 150%;
    color: #00eca2;
    margin-left: 170px;
  }


  .workbase2 {
    right: 30%;
    font-size: 150%;
    color: #00eca2;
    margin-left: 90px;
  }

  .wrapper{
    display: flex;
    flex-direction: column;
    align-items: center;
    padding-top: 30px;
  }

</style>
<script>
import Grakn from 'grakn-client';
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
      showLoginPanel: false,
      showLoginPage: false,
      showConnectionPanel: false,
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
  beforeCreate() {
    const grakn = new Grakn(ServerSettings.getServerUri(), { username: this.username, password: this.password });
    grakn.session('grakn').transaction().then(() => {
      this.$router.push('develop/data');
      this.$store.dispatch('initGrakn');
    })
      .catch((e) => {
        if (e.message.includes('2 UNKNOWN')) { // -> show login panel for kgms
          this.showLoginPage = true;
          this.showLoginPanel = true;
        } else if (e.message.includes('14 UNAVAILABLE')) { // -> show connection panel for core
          this.showLoginPage = true;
          this.showConnectionPanel = true;
        } else {
          this.$notifyError(e);
        }
      });
  },
  created() {
    window.addEventListener('keyup', (e) => {
      if (e.keyCode === 13 && !e.shiftKey && this.username.length && this.password.length) this.loginToKgms();
    });
  },
  mounted() {
    this.$nextTick(() => {
      this.serverHost = ServerSettings.getServerHost();
      this.serverPort = ServerSettings.getServerPort();
    });
  },
  methods: {
    loginToKgms() {
      this.$toasted.clear();
      this.isLoading = true;
      const grakn = new Grakn(ServerSettings.getServerUri(), { username: this.username, password: this.password });
      grakn.session('grakn').transaction().then(() => {
        this.$store.dispatch('login', { username: this.username, password: this.password });
        storage.set('user-credentials', JSON.stringify({ username: this.username, password: this.password }));
        this.isLoading = false;
        this.$router.push('develop/data');
      })
        .catch((e) => {
          this.isLoading = false;
          let error;
          if (e.message.includes('2 UNKNOWN')) {
            error = 'Login failed: <br> - check if credentials are correct';
          } else if (e.message.includes('14 UNAVAILABLE')) {
            error = 'Login failed: <br> - make sure Grakn KGMS is running <br> - check that host and port are correct';
          } else {
            error = e;
          }
          this.$notifyError(error);
        });
    },
    connectToCore() {
      this.$toasted.clear();
      this.isLoading = true;
      const grakn = new Grakn(ServerSettings.getServerUri());
      grakn.session('grakn').transaction().then(() => {
        this.$router.push('develop/data');
        this.$store.dispatch('initGrakn');
        this.isLoading = false;
      })
        .catch((e) => {
          this.isLoading = false;
          if (e.message.includes('2 UNKNOWN')) { // -> show login panel for kgms
            this.showLoginPage = true;
            this.showLoginPanel = true;
            this.showConnectionPanel = false;
          } else if (e.message.includes('14 UNAVAILABLE')) { // -> show connection panel for core
            this.$notifyError('Looks like Grakn is not running: <br> - Verify Grakn is running, check the Host and Port, then refresh workbase');
          } else {
            this.$notifyError(e);
          }
        });
    },
  },
};
</script>
