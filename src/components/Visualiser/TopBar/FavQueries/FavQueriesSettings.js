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
// Default constant values
const QUERIES_LS_KEY = 'fav_queries';

export default {
  getFavQueries(currentDatabase) {
    const queries = storage.get(QUERIES_LS_KEY);

    if (queries == null) {
      storage.set(QUERIES_LS_KEY, JSON.stringify({ [currentDatabase]: {} }));
      return {};
    }

    const queriesObject = JSON.parse(queries);
    // If there is not object associated to the current database we return empty object
    if (!(currentDatabase in queriesObject)) {
      return {};
    }
    return queriesObject[currentDatabase];
  },
  addFavQuery(queryName, queryValue, currentDatabase) {
    const queries = this.getFavQueries(currentDatabase);

    queries[queryName] = queryValue;
    this.setFavQueries(queries, currentDatabase);
  },
  removeFavQuery(queryName, currentDatabase) {
    const queries = this.getFavQueries(currentDatabase);
    delete queries[queryName];
    this.setFavQueries(queries, currentDatabase);
  },
  setFavQueries(queriesParam, currentDatabase) {
    const queries = JSON.parse(storage.get(QUERIES_LS_KEY));
    Object.assign(queries, { [currentDatabase]: queriesParam });
    storage.set(QUERIES_LS_KEY, JSON.stringify(queries));
  },
};
