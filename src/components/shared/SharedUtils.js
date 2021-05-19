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

import { TypeDBOptions } from "typedb-client/api/TypeDBOptions";
import { TransactionType } from "typedb-client/api/TypeDBTransaction";
import QueryUtils from '../Visualiser/RightBar/SettingsTab/QuerySettings';

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
  const typeDBTx = global.typeDBTx[state.activeTab];
  const isTypeDBTxOpen = await typeDBTx.isOpen();
  if (!isTypeDBTxOpen) { // typeDBTx has been invalidated because of an error and so it's closed now
    global.typeDBTx[state.activeTab] = await global.typeDBSession.transaction(TransactionType.READ, getTransactionOptions());
    commit('setGlobalErrorMsg', 'The transaction was refreshed and, as a result, the explanation of currently displayed inferred nodes may be incomplete.');
  }
};

export const getTransactionOptions = () => {
  return TypeDBOptions.core({
    infer: QueryUtils.getReasoning(),
    explain: true,
  });
};
