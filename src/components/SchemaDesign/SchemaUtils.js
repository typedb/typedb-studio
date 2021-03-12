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

export function updateNodePositions(nodes) {
  let positionMap = storage.get('schema-node-positions');
  if (positionMap) {
    positionMap = JSON.parse(positionMap);
    nodes.forEach((node) => {
      if (node.id in positionMap) {
        const { x, y } = positionMap[node.id];
        Object.assign(node, { x, y, fixed: { x: true, y: true } });
      }
    });
  }
  return nodes;
}

export async function loadMetaTypeInstances(graknTx) {
  // Fetch types
  const rels = (await (await graknTx.query().match('match $x sub relation;')).collect()).map(cm => cm.get("x"));
  const entities = (await (await graknTx.query().match('match $x sub entity;')).collect()).map(cm => cm.get("x"));
  const attributes = (await (await graknTx.query().match('match $x sub attribute;')).collect()).map(cm => cm.get("x"));
  const roles = (await (await graknTx.query().match('match $x sub relation:role;')).collect()).map(cm => cm.get("x"));

  // Get types labels
  const metaTypeInstances = {};
  metaTypeInstances.entities = await Promise.all(entities.map(type => type.getLabel()))
    .then(labels => labels.filter(l => l !== 'entity')
      .concat()
      .sort());
  metaTypeInstances.relations = await Promise.all(rels.map(async type => type.getLabel()))
    .then(labels => labels.filter(l => l && l !== 'relation')
      .concat()
      .sort());
  metaTypeInstances.attributes = await Promise.all(attributes.map(type => type.getLabel()))
    .then(labels => labels.filter(l => l !== 'attribute')
      .concat()
      .sort());
  metaTypeInstances.roles = await Promise.all(roles.map(async type => type.getScopedLabel()))
    .then(labels => labels.filter(l => l && l !== 'relation:role')
      .concat()
      .sort());
  return metaTypeInstances;
}

// attach attribute labels and data types to each node
export async function computeAttributes(nodes, tx) {
  return Promise.all(nodes.map(async (node) => {
    const concept = await tx.concepts().getThingType(node.typeLabel);
    const attributes = await concept.asRemote(tx).getOwns().collect();
    node.attributes = await Promise.all(attributes.map(async concept => ({ type: await concept.getLabel(), valueType: await concept.getValueType() })));
    return node;
  }));
}

// attach role labels to each node
export async function computeRoles(nodes, tx) {
  return Promise.all(nodes.map(async (node) => {
    const concept = await tx.concepts().getThingType(node.typeLabel);
    const roles = await concept.asRemote(tx).getPlays().collect();
    node.roles = await Promise.all(roles.map(concept => concept.getScopedLabel()));
    return node;
  }));
}

