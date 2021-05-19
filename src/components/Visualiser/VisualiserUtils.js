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

import QuerySettings from './RightBar/SettingsTab/QuerySettings';

const LETTER_G_KEYCODE = 71;

export function limitQuery(query) {
  let limitedQuery = query;

  if (query.startsWith("match")) {
    const limitRegex = /^((.|\n)*)(limit\b.*?;).*/;
    const offsetRegex = /.*;\s*(offset\b.*?;).*/;

    if (!(offsetRegex.test(query)) && !limitRegex.test(query)) {
      limitedQuery = `${query} offset 0; limit ${QuerySettings.getQueryLimit()};`;
    } else if (!(offsetRegex.test(query))) {
      limitedQuery = `${query.match(limitRegex)[1]}offset 0; ${query.match(limitRegex)[3]}`;
    } else if (!(limitRegex.test(query))) {
      limitedQuery = `${query} limit ${QuerySettings.getQueryLimit()};`;
    }
  }

  return limitedQuery;
}

/**
 * for each node, retrieves the label and value for all the node's attributes and adds them to
 * `attributes` property of the node. the `attributes` property of the node is read to list the
 * attributes' label and value in the sidebar for the selected node.
 * @param {*} nodes nodes that are currently visualised in the graph
 * @param {*} tx
 * @return array of node objects, where each object includes the `attributes` property
 */
export async function computeAttributes(nodes, tx) {
  for (const node of nodes) {
    if (node.iid) {
      const ownedAttrs = await (await tx.concepts().getThing(node.iid)).asRemote(tx).getHas().collect();
      node.attributes = ownedAttrs.map(attr => { return { typeLabel: attr.getType().getLabel().name(), value: attr.getValue() } });
    } else if (node.typeLabel) {
      const ownedAttrTypes = await (await tx.concepts().getThingType(node.typeLabel)).asRemote(tx).getOwns().collect();
      node.attributes = ownedAttrTypes.map(attr => { return { typeLabel: attr.getLabel().name() }; });
    } else {
      throw "Node does not have a Label or an IID";
    }
  }
  return nodes;
}

export async function loadMetaTypeInstances(typeDBTx) {
  // Fetch types
  const rels = (await (await typeDBTx.query().match('match $x sub relation;')).collect()).map(cm => cm.get("x"));
  const entities = (await (await typeDBTx.query().match('match $x sub entity;')).collect()).map(cm => cm.get("x"));
  const attributes = (await (await typeDBTx.query().match('match $x sub attribute;')).collect()).map(cm => cm.get("x"));
  const roles = (await (await typeDBTx.query().match('match $x sub relation:role;')).collect()).map(cm => cm.get("x"));

  // Get types labels
  const metaTypeInstances = {};
  metaTypeInstances.entities = await Promise.all(entities.map(type => type.getLabel().name()))
    .then(labels => labels.filter(l => l !== 'entity')
      .concat()
      .sort());
  metaTypeInstances.relations = await Promise.all(rels.map(type => type.getLabel().name()))
    .then(labels => labels.filter(l => l && l !== 'relation')
      .concat()
      .sort());
  metaTypeInstances.attributes = await Promise.all(attributes.map(type => type.getLabel().name()))
    .then(labels => labels.filter(l => l !== 'attribute')
      .concat()
      .sort());
  metaTypeInstances.roles = await Promise.all(roles.map(type => type.getLabel().scopedName()))
    .then(labels => labels.filter(l => l && l !== 'relation:role')
      .concat()
      .sort());
  return metaTypeInstances;
}

export function validateQuery(query) {
  const trimmedQuery = query.trim();
  const supportedQueryRgx = /^match\s+/;
  if (!supportedQueryRgx.test(trimmedQuery)) {
    throw new Error('At the moment, only `match` queries are supported.');
  }
}

export function addResetGraphListener(dispatch, action) {
  window.addEventListener('keydown', (e) => {
  // Reset canvas when metaKey(CtrlOrCmd) + G are pressed
    if ((e.keyCode === LETTER_G_KEYCODE) && e.metaKey) { dispatch(action); }
  });
}

/**
 * produces an array of ConceptMap answers containing the neighbours of the given targetNode
 * identified neighbours:
 * 1) differ based on the baseType of targetNode
 * 2) are not already visualised
 * 3) are no more than the user-specified NeighboursLimit
 * @param {Object} targetNode the node for which we want to load the neighbours
 * @param {Array} currentEdges the edges that are currently visualised
 * @param {Object} typeDBTx TypeDB transaction used to execute query
 */
export async function getNeighbourAnswers(targetNode, currentEdges, typeDBTx) {
  const answers = [];
  const neighboursLimit = QuerySettings.getNeighboursLimit();

  const isEdgeAlreadyVisualised = edgeId => currentEdges.some(currEdge => currEdge.id === edgeId);

  switch (targetNode.baseType) {
    case 'ENTITY_TYPE':
    case 'ATTRIBUTE_TYPE':
    case 'RELATION_TYPE': {
      const targetTypeLabel = targetNode.id;
      const query = `match $target-type type ${targetTypeLabel}; $neighbour-instance isa $target-type; get $neighbour-instance;`;
      const iter = typeDBTx.query().match(query).iterator();

      let answer = (await iter.next()).value;
      while (answer && answers.length !== neighboursLimit) {
        const neighbourInstanceId = answer.map().get('neighbour-instance').getIID();
        const edgeId = `${targetTypeLabel}-${neighbourInstanceId}-isa`;
        if (!isEdgeAlreadyVisualised(edgeId)) {
          answers.push(answer);
        }
        // eslint-disable-next-line no-await-in-loop
        answer = (await iter.next()).value;
      }
      break;
    }
    case 'ENTITY': {
      const targetEntId = targetNode.id;
      const query = `match $target-entity iid ${targetEntId}; $neighbour-relation ($target-entity-role: $target-entity); get $neighbour-relation, $target-entity-role;`;
      const iter = typeDBTx.query().match(query).iterator();
      let answer = (await iter.next()).value;
      while (answer && answers.length !== neighboursLimit) {
        const targetEntRoleLabel = answer.map().get('target-entity-role').getLabel().name();
        if (targetEntRoleLabel !== 'role') {
          const neighbourRelId = answer.map().get('neighbour-relation').getIID();
          const edgeId = `${neighbourRelId}-${targetEntId}-${targetEntRoleLabel}`;
          if (!isEdgeAlreadyVisualised(edgeId)) {
            answers.push(answer);
          }
        }
        // eslint-disable-next-line no-await-in-loop
        answer = (await iter.next()).value;
      }
      break;
    }
    case 'ATTRIBUTE': {
      const targetAttrId = targetNode.id;
      const query = `match $neighbour-owner has attribute $target-attribute; $target-attribute iid ${targetAttrId}; get $neighbour-owner;`;
      const iter = typeDBTx.query().match(query).iterator();
      let answer = (await iter.next()).value;
      while (answer && answers.length !== neighboursLimit) {
        const neighbourOwnerId = answer.map().get('neighbour-owner').getIID();
        const edgeId = `${neighbourOwnerId}-${targetAttrId}-has`;
        if (!isEdgeAlreadyVisualised(edgeId)) {
          answers.push(answer);
        }
        // eslint-disable-next-line no-await-in-loop
        answer = (await iter.next()).value;
      }
      break;
    }
    case 'RELATION': {
      const targetRelId = targetNode.id;
      const query = `match $target-relation ($neighbour-role: $neighbour-player); $target-relation iid ${targetRelId}; get $neighbour-player, $neighbour-role;`;
      const iter = typeDBTx.query().match(query).iterator();
      let answer = (await iter.next()).value;
      while (answer && answers.length !== neighboursLimit) {
        const neighbourRoleLabel = answer.map().get('neighbour-role').getLabel().name();
        if (neighbourRoleLabel !== 'role') {
          const neighbourRoleId = answer.map().get('neighbour-player').getIID();
          const edgeId = `${targetRelId}-${neighbourRoleId}-${neighbourRoleLabel}`;
          if (!isEdgeAlreadyVisualised(edgeId)) {
            answers.push(answer);
          }
        }
        // eslint-disable-next-line no-await-in-loop
        answer = (await iter.next()).value;
      }
      break;
    }
    default:
      throw new Error(`Unrecognised baseType of thing: ${targetNode.baseType}`);
  }

  return answers;
}

export async function getConcept(node, tx) {
  if (node.iid) {
    return await tx.concepts().getThing(node.iid);
  } else if (node.typeLabel) {
    return await tx.concepts().getThingType(node.typeLabel);
  } else {
    throw "Unable to get concept for node " + node;
  }
}
