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

import Style from './Style';
import NodeSettings from './RightBar/SettingsTab/DisplaySettings';
import CDB from '../shared/CanvasDataBuilder';

function buildValue(array) {
  if (!array) return '';
  return array.join(', ');
}

async function labelFromStorage(node, attributeTypes) {
  const map = {};
  // Populate map with map[attributeType] = array of values (can be undefined)
  // This is because we need to handle 2 cases:
  // - when the current node does not have any attribute of type `attributeType`
  // - when the current node has multiple attributes of type 'attributeType'
  const promises = (await (await node.attributes()).collect()).map(async (attr) => {
    const label = await (await attr.type()).label();
    const value = await attr.value();
    if (!map[label]) map[label] = [value];
    else map[label].push(value);
  });
  await Promise.all(promises);
  const label = attributeTypes.map(type => `${type}: ${buildValue(map[type])}`).join('\n');
  // Always show node type when displaying node attributes on label
  return `${node.type}\n${label}`;
}

function buildLabel(node) {
  const labels = NodeSettings.getTypeLabels(node.typeLabel);
  if (labels.length) return labelFromStorage(node, labels); // this function is async
  let label;
  switch (node.baseType) {
    case 'ENTITY':
      label = `${node.typeLabel}: ${node.id}`;
      break;
    case 'ATTRIBUTE':
      label = `${node.typeLabel}: ${node.value}`;
      break;
    case 'RELATION':
      label = '';
      break;
    default:
      label = node.typeLabel;
  }
  return label;
}

async function prepareSchemaConcept(schemaConcept) {
  schemaConcept.label = await schemaConcept.label();
  // schemaConcept.attributes = await computeAttributes(schemaConcept);
}

async function prepareEntity(entity) {
  entity.type = await (await entity.type()).label();
  entity.label = buildLabel(entity);
  entity.isInferred = await entity.isInferred();
}

async function prepareRelation(rel) {
  rel.type = await (await rel.type()).label();
  rel.isInferred = await rel.isInferred();
}

async function prepareAttribute(attribute) {
  attribute.type = await (await attribute.type()).label();
  attribute.value = attribute.value();
  attribute.label = buildLabel(attribute);
  attribute.isInferred = await attribute.isInferred();
}

/**
 * For each node contained in concepts param, based on baseType, fetch type, value
 * and build label
 * @param {Object[]} concepts array of gRPC concepts to prepare
 */
async function prepareNodes(concepts) {
  const nodes = [];

  await Promise.all(concepts.map(async (concept) => {
    switch (concept.baseType) {
      case 'ENTITY_TYPE':
      case 'ATTRIBUTE_TYPE':
      case 'RELATION_TYPE':
        await prepareSchemaConcept(concept);
        break;
      case 'ENTITY':
        await prepareEntity(concept);
        break;
      case 'ATTRIBUTE':
        await prepareAttribute(concept);
        break;
      case 'RELATION': {
        await prepareRelation(concept);
        break;
      }
      default:
        break;
    }
    concept.attrOffset = 0;
    nodes.push(concept);
  }));

  return nodes;
}

async function loadRolePlayers(relation) {
  const nodes = [];
  const edges = [];
  let roleplayers = await relation.rolePlayersMap();
  roleplayers = Array.from(roleplayers.entries());

  // Build array of promises
  const promises = Array.from(roleplayers, async ([role, setOfThings]) => {
    const roleLabel = await role.label();
    await Promise.all(Array.from(setOfThings.values()).map(async (thing) => {
      switch (thing.baseType) {
        case 'ENTITY':
          await prepareEntity(thing);
          break;
        case 'ATTRIBUTE':
          await prepareAttribute(thing);
          break;
        case 'RELATION':
          await prepareRelation(thing);
          break;
        default:
          throw new Error(`Unrecognised baseType of thing: ${thing.baseType}`);
      }
      thing.attrOffset = 0;

      nodes.push(thing);
      edges.push(CDB.getEdge(relation, thing, CDB.edgeTypes.instance.PLAYS, roleLabel));
    }));
  });
  return Promise.all(promises).then((() => ({ nodes, edges })));
}

async function relationsRolePlayers(relations) {
  const results = await Promise.all(relations.map(rel => loadRolePlayers(rel)));
  return {
    nodes: results.flatMap(x => x.nodes),
    edges: results.flatMap(x => x.edges),
  };
}

async function buildFromConceptList(path, pathNodes) {
  const data = { nodes: await prepareNodes(pathNodes), edges: [] };

  const relations = data.nodes.filter(x => x.baseType === 'RELATION');

  const roleplayers = await relationsRolePlayers(relations);

  roleplayers.edges = roleplayers.edges.filter(x => (path.list().includes(x.from) && path.list().includes(x.to)));

  data.edges.push(...roleplayers.edges);

  data.edges.map(edge => Object.assign(edge, Style.computeShortestPathEdgeStyle()));
  return data;
}


export default {
  buildFromConceptList,
  prepareNodes,
  relationsRolePlayers,
};
