/*
 * Copyright (C) 2021 Grakn Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

import storage from '@/components/shared/PersistentStorage';
import Vue from 'vue';
import Vuex from 'vuex';

import * as actions from './actions';
import * as getters from './getters';

Vue.use(Vuex);

const debug = process.env.NODE_ENV !== 'production';

export default new Vuex.Store({
  state: {
    databases: undefined,
    credentials: undefined,
    isAuthenticated: undefined,
    landingPage: undefined,
    userLogged: false,
    isGraknRunning: undefined,
    activeTab: undefined,
  },
  actions,
  getters,
  mutations: {
    setIsGraknRunning(state, isGraknRunning) {
      state.isGraknRunning = isGraknRunning;
    },
    setDatabases(state, list) {
      state.databases = list;
    },
    setCredentials(state, credentials) {
      state.credentials = credentials;
    },
    setAuthentication(state, isAuthenticated) {
      state.isAuthenticated = isAuthenticated;
    },
    setLandingPage(state, landingPage) {
      state.landingPage = landingPage;
    },
    deleteCredentials(state) {
      state.credentials = null;
      storage.delete('user-credentials');
    },
    loadLocalCredentials(state, SERVER_AUTHENTICATED) {
      if (!SERVER_AUTHENTICATED) {
        state.credentials = null;
      } else {
        const localCredentials = storage.get('user-credentials');
        state.credentials = (localCredentials) ? JSON.parse(localCredentials) : null;
        state.userLogged = (state.credentials);
      }
    },
    userLogged(state, logged) {
      state.userLogged = logged;
    },
    setActiveTab(state, activeTab) {
      state.activeTab = activeTab;
    },
  },
  strict: debug,
});

