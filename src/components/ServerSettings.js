/*
 * Copyright (C) 2021 Vaticle
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

import storage from './shared/PersistentStorage';

const SERVER_HOST = 'server-host';
const SERVER_PORT = 'server-port';

const DEFAULT_SERVER_HOST = '127.0.0.1';
const DEFAULT_SERVER_PORT = '1729';

function getServerHost() {
  const host = storage.get(SERVER_HOST);
  if (host) return host;

  storage.set(SERVER_HOST, DEFAULT_SERVER_HOST);
  return getServerHost();
}

function setServerHost(host) {
  storage.set(SERVER_HOST, host);
}

function getServerPort() {
  const port = storage.get(SERVER_PORT);
  if (port) return port;

  storage.set(SERVER_PORT, DEFAULT_SERVER_PORT);
  return getServerPort();
}

function setServerPort(port) {
  storage.set(SERVER_PORT, port);
}

function getServerUri() {
  return `${this.getServerHost()}:${this.getServerPort()}`;
}

export default {
  getServerHost,
  setServerHost,
  getServerPort,
  setServerPort,
  getServerUri,
};
