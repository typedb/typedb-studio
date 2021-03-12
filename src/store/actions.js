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

import { GraknClient } from 'grakn-client/GraknClient';
import ServerSettings from '@/components/ServerSettings';

export const loadDatabases = async (context) => {
  try {
    const resp = (await global.grakn.databases().all()).map(db => db.name());
    context.commit('setIsGraknRunning', true);
    context.commit('setDatabases', resp);
  } catch (e) {
    context.commit('setIsGraknRunning', false);
  }
};

export const createDatabase = async (context, name) => {
  await global.grakn.databases().create(name).then(() => { context.dispatch('loadDatabases'); });
};

export const deleteDatabase = async (context, name) => global.grakn.databases().get(name)
  .then(db => db.delete())
  .then(() => context.dispatch('loadDatabases'));

export const login = (context, credentials) =>
  context.dispatch('initGrakn', credentials).then(() => {
    context.commit('setCredentials', credentials);
    context.commit('userLogged', true);
  });

export const initGrakn = (context, credentials) => {
  global.grakn = new GraknClient.core(ServerSettings.getServerUri(), /* credentials */);
  context.dispatch('loadDatabases', credentials);
};

export const logout = async (context) => {
  context.commit('setCredentials', undefined);
  context.commit('setDatabases', undefined);
  context.commit('userLogged', false);
  // Need to notify all the other states that they need to invalidate GraknClient
};
