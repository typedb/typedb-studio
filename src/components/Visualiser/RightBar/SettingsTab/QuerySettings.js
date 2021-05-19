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

import storage from '@/components/shared/PersistentStorage';

// ------------ Limit number of results ---------------- //
const DEFAULT_QUERY_LIMIT = '30';

function getQueryLimit() {
  const queryLimit = storage.get('query_limit');
  if (queryLimit == null) {
    this.setQueryLimit(DEFAULT_QUERY_LIMIT);
    return DEFAULT_QUERY_LIMIT;
  }
  return queryLimit;
}

function setQueryLimit(value) {
  let parsedValue = parseInt(value, 10) || 0;
  if (parsedValue < 0) parsedValue = 0;
  storage.set('query_limit', parsedValue);
}

// -------------- Relation Settings ------------ //

const DEFAULT_ROLE_PLAYERS = true;
const DEFAULT_REASONING = false;

function setRolePlayersStatus(status) {
  storage.set('load_role_players', status);
}

function getRolePlayersStatus() {
  const rolePlayers = storage.get('load_role_players');
  if (rolePlayers == null) {
    this.setRolePlayersStatus(DEFAULT_ROLE_PLAYERS);
    return DEFAULT_ROLE_PLAYERS;
  }
  return rolePlayers;
}

function setReasoning(status) {
  storage.set('reasoning', status);
}

function getReasoning() {
  const reasoning = storage.get('reasoning');
  if (reasoning == null) {
    this.setReasoning(DEFAULT_REASONING);
    return DEFAULT_REASONING;
  }
  return reasoning;
}

// -------------- Neighbor Settings ------------ //

const DEFAULT_NEIGHBOUR_LIMIT = 20;

function setNeighboursLimit(value) {
  let parsedValue = parseInt(value, 10) || 0;
  if (parsedValue < 0) parsedValue = 0;
  storage.set('neighbours_limit', parsedValue);
}

function getNeighboursLimit() {
  const neighbourLimit = storage.get('neighbours_limit');
  if (neighbourLimit == null) {
    this.setNeighboursLimit(DEFAULT_NEIGHBOUR_LIMIT);
    return DEFAULT_NEIGHBOUR_LIMIT;
  }
  return neighbourLimit;
}

export default {
  getQueryLimit,
  setQueryLimit,
  setRolePlayersStatus,
  getRolePlayersStatus,
  setNeighboursLimit,
  getNeighboursLimit,
  setReasoning,
  getReasoning,
};
