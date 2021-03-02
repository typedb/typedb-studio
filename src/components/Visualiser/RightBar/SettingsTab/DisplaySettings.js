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


function getPreferencesMap() {
  let loadedMap = storage.get('databases-preferences');

  if (loadedMap) {
    loadedMap = JSON.parse(loadedMap);
  } else {
    loadedMap = {};
    storage.set('databases-preferences', '{}');
  }
  return loadedMap;
}

function emptyDatabaseMap() {
  return {
    label: {},
    colour: {},
    position: {},
  };
}

// Getter and setter for preferences map

function getMap(database) {
  const prefMap = getPreferencesMap();
  if (database in prefMap) {
    return prefMap[database];
  }
  // If current database does not have preferences map create one
  prefMap[database] = emptyDatabaseMap();
  storage.set('databases-preferences', JSON.stringify(prefMap));
  return prefMap[database];
}


function flushMap(database, map) {
  const fullMap = JSON.parse(storage.get('databases-preferences'));
  fullMap[database] = map;
  storage.set('databases-preferences', JSON.stringify(fullMap));
}

// Labels on types

function getTypeLabels(type) {
  const database = storage.get('current_database_data');
  const map = getMap(database);
  return map.label[type] || [];
}


function toggleLabelByType({ type, attribute }) {
  const database = storage.get('current_database_data');
  const map = getMap(database);

  // Create map for current type if it does not exist
  if (!map.label[type]) {
    map.label[type] = [];
  }
  // If map includes current type - remove it
  if (map.label[type].includes(attribute)) {
    map.label[type].splice(map.label[type].indexOf(attribute), 1);
  } else if (attribute === undefined) { // attribute is undefined when we reset the labels
    map.label[type] = [];
  } else { // If map does not include current type - add it
    map.label[type].push(attribute);
  }
  flushMap(database, map);
}

function getTypeColours(type) {
  const database = storage.get('current_database_data');
  const map = getMap(database);
  return map.colour[type] || [];
}

function toggleColourByType({ type, colourString }) {
  const database = storage.get('current_database_data');
  const map = getMap(database);

  // Create map for current type if it does not exist
  if (!map.colour[type]) {
    map.colour[type] = [];
  }
  // If map includes current type - remove it
  if (map.colour[type].includes(colourString)) {
    map.colour[type] = '';
  } else if (map.colour[type].length) {
    map.colour[type] = '';
    map.colour[type] = colourString;
  } else { // If map does not include current type - add it
    map.colour[type] = colourString;
  }
  flushMap(database, map);
}


export default {
  getTypeLabels,
  toggleLabelByType,
  getTypeColours,
  toggleColourByType,
};

