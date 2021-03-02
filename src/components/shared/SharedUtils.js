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

import { TransactionType } from "grakn-client/GraknClient";

export const META_LABELS = new Set(['entity', 'relation', 'attribute', 'relation:role', 'thing']);

export const baseTypes = {
  THING_TYPE: 'THING_TYPE',
  ENTITY_TYPE: 'ENTITY_TYPE',
  RELATION_TYPE: 'RELATION_TYPE',
  ATTRIBUTE_TYPE: 'ATTRIBUTE_TYPE',
  ROLE_TYPE: 'ROLE_TYPE',
  ENTITY_INSTANCE: 'ENTITY',
  RELATION_INSTANCE: 'RELATION',
  ATTRIBUTE_INSTANCE: 'ATTRIBUTE',
};

export const reopenTransaction = async (state, commit) => {
  const graknTx = global.graknTx[state.activeTab];
  const isGraknTxOpen = await graknTx.isOpen();
  if (!isGraknTxOpen) { // graknTx has been invalidated because of an error and so it's closed now
    global.graknTx[state.activeTab] = await global.graknSession.transaction(TransactionType.READ);
    commit('setGlobalErrorMsg', 'The transaction was refreshed and, as a result, the explanation of currently displayed inferred nodes may be incomplete.');
  }
};
