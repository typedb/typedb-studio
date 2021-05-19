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

/* eslint-disable no-await-in-loop */
import QuerySettings from '../Visualiser/RightBar/SettingsTab/QuerySettings';
import DisplaySettings from '../Visualiser/RightBar/SettingsTab/DisplaySettings';
import {baseTypes, META_LABELS} from './SharedUtils';
import store from '../../store';
import {getConcept} from "../Visualiser/VisualiserUtils";
import {router} from "../../main";

const convertToRemote = (concept) => {
  if (concept.asRemote) {
    const tx = router.history.current.path === "/design/schema" ? global.typeDBTx.schemaDesign : global.typeDBTx[store.getters.activeTab];
    return concept.asRemote(tx);
  }
  return concept;
};

const { ENTITY_INSTANCE, RELATION_INSTANCE, ATTRIBUTE_INSTANCE, THING_TYPE, ENTITY_TYPE, RELATION_TYPE, ATTRIBUTE_TYPE, ROLE_TYPE } = baseTypes;

const collect = (array, current) => array.concat(current);
const deduplicateConcepts = arr => arr.filter((item, index, self) => {
  return index === self.findIndex(t => getConceptIdentifier(t.concept) === getConceptIdentifier(item.concept));
});

const getConceptIdentifier = (concept) => {
  if (concept.isThing()) {
    return concept.getIID();
  } else if (concept.isRoleType()) {
    return concept.getLabel().scopedName();
  } else if (concept.isType()) {
    return concept.getLabel().name();
  } else {
    throw "Unrecognised concept type: " + concept;
  }
}

const edgeTypes = {
  type: {
    OWNS: 'OWNS_TYPE',
    PLAYS: 'PLAYS_IN_TYPE',
    RELATES: 'RELATES_TO_TYPE',
    SUB: 'SUBS_TYPE',
  },
  instance: {
    HAS: 'HAS_INSTANCE',
    PLAYS: 'PLAYS_IN_INSTANCE',
    RELATES: 'RELATES_TO_INSTANCE',
    ISA: 'SUBS_INSTANCE',
  },
};

const getThingBaseType = (thing) => {
  if (thing.isEntity()) return ENTITY_INSTANCE;
  else if (thing.isRelation()) return RELATION_INSTANCE;
  else if (thing.isAttribute()) return ATTRIBUTE_INSTANCE;
  else throw "Unrecognised instance type: " + thing;
};

const getTypeBaseType = (type) => {
  if (type.isEntityType()) return ENTITY_TYPE;
  else if (type.isRelationType()) return RELATION_TYPE;
  else if (type.isAttributeType()) return ATTRIBUTE_TYPE;
  else if (type.isRoleType()) return ROLE_TYPE;
  else if (type.isThingType()) return THING_TYPE;
  else throw "Unrecognised type encoding: " + type;
}

const getTypeLabel = (type) => {
  let label;
  if (type.isRoleType()) label = type.getLabel().scopedName();
  else label = type.getLabel().name();
  return label;
};

const shouldVisualiseType = (type) => {
  let shouldSkip = false;
  if (META_LABELS.has(getTypeLabel(type))) shouldSkip = true;
  return !shouldSkip;
};

const getEdge = (from, to, edgeType, label) => {
  const edge = { from: getConceptIdentifier(from), to: getConceptIdentifier(to) };

  switch (edgeType) {
    // TYPES
    case edgeTypes.type.PLAYS:
    case edgeTypes.type.RELATES:
      edge.label = label;
      edge.arrows = { to: { enabled: true } };
      edge.options = { hideLabel: false, hideArrow: false };
      break;
    case edgeTypes.type.OWNS:
      edge.label = 'owns';
      edge.arrows = { to: { enabled: true } };
      edge.options = { hideLabel: false, hideArrow: false };
      break;
    case edgeTypes.type.SUB:
      edge.label = 'sub';
      edge.arrows = { to: { enabled: true } };
      edge.options = { hideLabel: false, hideArrow: false };
      break;
    // INSTANCES
    case edgeTypes.instance.RELATES:
    case edgeTypes.instance.PLAYS:
      edge.label = '';
      edge.hiddenLabel = label;
      edge.arrows = { to: { enabled: false } };
      edge.options = { hideLabel: true, hideArrow: true };
      break;
    case edgeTypes.instance.HAS:
      edge.label = '';
      edge.hiddenLabel = 'has';
      edge.arrows = { to: { enabled: false } };
      edge.options = { hideLabel: true, hideArrow: true };
      break;
    case edgeTypes.instance.ISA:
      edge.label = '';
      edge.hiddenLabel = 'isa';
      edge.arrows = { to: { enabled: false } };
      edge.options = { hideLabel: true, hideArrow: true };
      break;
    default:
      throw new Error(`Edge type [${edgeType}] is not recoganised`);
  }

  edge.id = `${edge.from}-${edge.to}-${edge.label === '' ? edge.hiddenLabel : edge.label}`;
  return edge;
};

const getNodeLabelWithAttrs = async (baseLabel, type, instance) => {
  let label = baseLabel;
  if (instance.baseType === RELATION_INSTANCE) return label;

  const selectedAttrs = DisplaySettings.getTypeLabels(type);

  if (selectedAttrs.length > 0) {
    const allAttrs = await convertToRemote(instance).getHas().collect();

    const allAttrsData = allAttrs.map( attr => ({ label: attr.getType().getLabel().name(), value: attr.getValue() }));
    const allAttrsMap = {};
    allAttrsData.forEach((attrData) => { allAttrsMap[attrData.label] = attrData.value; });

    // eslint-disable-next-line no-prototype-builtins
    const selectedAttrsData = selectedAttrs.map(attrLabel => ({ label: attrLabel, value: allAttrsMap.hasOwnProperty(attrLabel) ? allAttrsMap[attrLabel] : '' }));
    label += selectedAttrsData.map(attrData => `\n${attrData.label}: ${attrData.value}`).join('');
  }
  return label;
};

/**
 * produces and returns the common node object for a concept instance
 * most node properties are available on the instance, but typeQLVar and explanation
 * need to be passed to here for the ConceptMap that contained the instance
 * @param {Thing} instance guaranteed to be a concept instance
 * @param {String} typeQLVar
 * @param {Explainable} explainable
 */
const buildCommonInstanceNode = async (instance, typeQLVar, explainable) => {
  const node = {};
  node.id = instance.getIID();
  node.iid = instance.getIID();
  node.baseType = getThingBaseType(instance);
  node.var = typeQLVar;
  node.attrOffset = 0;
  node.typeLabel = getTypeLabel(instance.getType());
  node.isInferred = await convertToRemote(instance).isInferred();
  node.attributes = await convertToRemote(instance).getHas();
  node.explainable = explainable;

  return node;
};

/**
 * produces and returns the node for the given concept instance based on is basetype
 * @param {Thing} instance guaranteed to be a concept instance
 * @param {String} typeQLVar
 * @param {Explainable} explainable
 */
const getInstanceNode = async (instance, typeQLVar, explainable) => {
  const node = await buildCommonInstanceNode(instance, typeQLVar, explainable);
  const baseType = getThingBaseType(instance);
  switch (baseType) {
    case ENTITY_INSTANCE: {
      node.label = await getNodeLabelWithAttrs(`${node.typeLabel}: ${node.iid}`, node.typeLabel, instance);
      break;
    }
    case RELATION_INSTANCE: {
      node.label = '';
      break;
    }
    case ATTRIBUTE_INSTANCE: {
      node.value = instance.getValue();
      const shortValue = node.value.toString().length < 80 ? node.value : `${node.value.toString().slice(0, 80)}...`;
      node.label = await getNodeLabelWithAttrs(`${node.typeLabel}: ${shortValue}`, node.typeLabel, instance);
      break;
    }
    default:
      throw new Error(`Instance type [${baseType}] is not recoganised`);
  }

  return node;
};

/**
 * produces the `has` edges from owners to the given attribute instance
 * @param {Concept} attribute must be an attribute instance
 */
const getInstanceHasEdges = async (attribute) => {
  const owners = await convertToRemote(attribute).getOwners().collect();
  const edges = owners.map(owner => getEdge(owner, attribute, edgeTypes.instance.HAS));
  return edges;
};

/**
 * produces the `role` edges from the given relation instance to its roleplayers
 * @param {Relation} relation
 */
// eslint-disable-next-line no-unused-vars
const getInstanceRelatesEdges = async (relation) => {
  const rpMap = await convertToRemote(relation).getPlayersByRoleType();
  const rpMapEntries = Array.from(rpMap.entries());

  const edges = rpMapEntries.map(([role, players]) => {
    if (!role.isRoot()) {
      return Array.from(players.values()).reduce(collect, []).map(player => getEdge(relation, player, edgeTypes.instance.RELATES, role.getLabel().name()))
    } else {
      return [];
    }
  }).reduce(collect, []);
  return [edges];
};

const getInstanceIsaEdges = async (instance) => {
  return getEdge(instance, instance.getType(), edgeTypes.instance.ISA);
};

/**
 * produces and returns the edges for the given concept instance
 * @param {Thing} thing
 * @param existingNodeIds
 */
const getInstanceEdges = async (thing, existingNodeIds) => {
  const edges = [];
  const baseType = getThingBaseType(thing);
  switch (baseType) {
    case ATTRIBUTE_INSTANCE:
      edges.push(await getInstanceIsaEdges(thing));
      edges.push(...await getInstanceHasEdges(thing));
      break;
    case RELATION_INSTANCE:
      edges.push(await getInstanceIsaEdges(thing));
      edges.push(...await getInstanceRelatesEdges(thing));
      break;
    case ENTITY_INSTANCE:
      edges.push(await getInstanceIsaEdges(thing));
      break;
    default:
      throw new Error(`Instance type [${baseType}] is not recognised`);
  }

  // exclude any edges that connect nodes which do not exist
  return edges.reduce(collect, []).filter(edge => existingNodeIds.includes(edge.from) && existingNodeIds.includes(edge.to));
};

/**
 * produces and returns nodes and edges for the given answers
 * only the instances stored within the answers are processed.
 * @param {ConceptMap[]} answers the untouched response of a transaction.query()
 */
const buildInstances = async (answers) => {
  let data = answers.map((answer) => {
    return Array.from(answer.map().entries()).map(([typeQLVar, concept]) => ({
      typeQLVar,
      concept,
      explainable: answer.explainables().relations().get(typeQLVar),
    }));
  }).reduce(collect, []);

  const shouldVisualiseVals = data.map(item => item.concept.isThing());
  data = data.map((item, index) => {
    item.shouldVisualise = shouldVisualiseVals[index];
    return item;
  });

  data = deduplicateConcepts(data);

  const nodes = (await Promise.all(data.filter(item => item.shouldVisualise).map(item => getInstanceNode(item.concept, item.typeQLVar, item.explainable))))
    .reduce(collect, []);
  const nodeIds = nodes.map(node => node.id);
  const edges = (await Promise.all(data.filter(item => item.shouldVisualise).map(item => getInstanceEdges(item.concept, nodeIds)))).reduce(collect, []);

  return { nodes, edges };
};

/**
 * produce and return the node for the given concept type based on its subtype
 * @param {Concept} type guaranteed to be a concept type
 * @param {String} typeQLVar
 */
const getTypeNode = async (type, typeQLVar) => {
  const node = {};
  const baseType = getTypeBaseType(type);
  switch (baseType) {
    case ENTITY_TYPE:
    case RELATION_TYPE:
    case ATTRIBUTE_TYPE:
    case THING_TYPE:
    case ROLE_TYPE: {
      node.id = getTypeLabel(type);
      node.label = getTypeLabel(type);
      node.baseType = baseType;
      node.var = typeQLVar;
      node.attrOffset = 0;
      node.typeLabel = getTypeLabel(type);
      node.attributes = await convertToRemote(type).getOwns().collect();
      node.playing = await convertToRemote(type).getPlays().collect();
      break;
    }
    default:
      throw new Error(`Concept type [${baseType}] is not recognised`);
  }

  return node;
};

/**
 * produces and returns the edge from the given concept type to its super type
 * given that the supertype in question isn't a meta type
 * @param {Concept} type must be a concept type
 */
const getTypeSubEdge = async (type) => {
  const sup = await convertToRemote(type).getSupertype();
  if (sup && sup.baseType !== 'META_TYPE') return [getEdge(type, sup, edgeTypes.type.SUB)];
  return [];
};

/**
 * produces and returns the edges from the given concept type to the attribute
 * types which it owns
 * @param {Concept} type must be a concept type
 */
const getTypeAttributeEdges = async (type) => {
  let edges = [];

  const sup = await convertToRemote(type).getSupertype();

  if (sup) {
    const typesAttrs = await convertToRemote(type).getOwns().collect();
    if (sup.isRoot()) {
      edges = typesAttrs.map(attr => getEdge(type, attr, edgeTypes.type.OWNS));
    } else { // if type has a super type which is not a META_CONCEPT construct edges to attributes except those which are inherited from its super type
      const supAttrIds = (await convertToRemote(sup).getOwns().collect()).map(x => x.id);
      const supAttrs = typesAttrs.filter(attr => !supAttrIds.includes(attr.id));
      edges = supAttrs.map(attr => getEdge(type, attr, edgeTypes.type.OWNS));
    }
  }

  return edges;
};

/**
 * produces and returns the edges from the given concept type to the relation type,
 * in which the given type plays a role
 * @param {Concept} type must be a concept type
 */
const getTypePlayEdges = async (type) => {
  const playRoles = await convertToRemote(type).getPlays().collect();
  return (await Promise.all(playRoles.map(role =>
    convertToRemote(role).getRelationTypes().collect().then(relations =>
      relations.map(relation => getEdge(relation, type, edgeTypes.type.PLAYS, role.getLabel().name())))
  ))).reduce(collect, []);
};

/**
 * produces and returns the edges from the relation type to its roleplayer types
 * @param {Concept} type must be a concept relation type
 */
const getTypeRelatesEdges = async (type) => {
  const roles = await convertToRemote(type).getRelates().collect();
  return (await Promise.all(roles.map(role =>
    convertToRemote(role).getPlayers().collect().then(players =>
      players.map(player => getEdge(type, player, edgeTypes.type.RELATES, role.getLabel().name())))
  ))).reduce(collect, []);
};

/**
 * produces and returns edges for the given type depending on its basetype
 * @param {Type} type must be a concept type
 * @param existingNodeIds
 */
const getTypeEdges = async (type, existingNodeIds) => {
  const edges = [];

  const baseType = getTypeBaseType(type);
  switch (baseType) {
    case ENTITY_TYPE:
    case ATTRIBUTE_TYPE:
    case THING_TYPE:
      edges.push(...await getTypeSubEdge(type));
      edges.push(...await getTypeAttributeEdges(type));
      edges.push(...await getTypePlayEdges(type));
      break;
    case RELATION_TYPE:
      edges.push(...await getTypeSubEdge(type));
      edges.push(...await getTypeAttributeEdges(type));
      edges.push(...await getTypePlayEdges(type));
      edges.push(...await getTypeRelatesEdges(type));
      break;
    default:
      throw new Error(`Concept type [${baseType}] is not recognised`);
  }

  // exclude any edges that connect nodes which do not exist
  return edges.filter(edge => existingNodeIds.includes(edge.from) && existingNodeIds.includes(edge.to));
};

/**
 * produces and returns nodes and edges for the given answers
 * only the types stored within the answers are processed
 * @param {ConceptMap[]} answers the untouched response of a transaction.query()
 */
const buildTypes = async (answers) => {
  let data = answers.map(answerGroup => Array.from(answerGroup.map().entries()).map(([typeQLVar, concept]) => ({
    typeQLVar,
    concept,
  }))).reduce(collect, []);

  const shouldVisualiseVals = data.map(item => item.concept.isType() && shouldVisualiseType(item.concept));

  data = data.map((item, index) => {
    item.shouldVisualise = shouldVisualiseVals[index];
    return item;
  });
  const nodes = (await Promise.all(data.filter(item => item.shouldVisualise).map(item => getTypeNode(item.concept, item.typeQLVar)))).reduce(collect, []);
  const nodeIds = nodes.map(node => node.id);
  const edges = (await Promise.all(data.filter(item => item.shouldVisualise).map(item => getTypeEdges(item.concept, nodeIds)))).reduce(collect, []);

  return { nodes, edges };
};

/**
 * produces and returns the node for the given concept based on its type
 * @param {*} concept
 * @param {*} typeQLVar
 * @param {*} explanation
 */
const getNeighbourNode = async (concept, typeQLVar, explanation) => {
  let node;
  if (concept.isType()) {
    node = await getTypeNode(concept, typeQLVar);
  } else if (concept.isThing()) {
    node = await getInstanceNode(concept, typeQLVar, explanation);
  }
  return node;
};

/**
 * Produces and returns the edges that connect the given targetNode with its neighbourNode
 * @param {*} neighbourConcept
 * @param {*} targetNode the node whose neighbour edges are to be produced
 * @param {*} typeDBTx
 */
const getNeighbourEdges = async (neighbourConcept, targetConcept, existingNodeIds) => {
  const edges = [];

  if (targetConcept.isEntityType() || targetConcept.isRelationType() || targetConcept.isAttributeType()) {
    edges.push(getEdge(neighbourConcept, targetConcept, edgeTypes.instance.ISA));
  } else if (targetConcept.isEntity()) {
    if (neighbourConcept.isRelation()) {
      const relation = neighbourConcept;
      edges.push(...await getInstanceRelatesEdges(relation));
    }
  } else if (targetConcept.isAttribute()) {
    const owner = neighbourConcept;
    edges.push(getEdge(owner, targetConcept, edgeTypes.instance.HAS));
  } else if (targetConcept.isRelation()) {
    edges.push(...await getInstanceRelatesEdges(targetConcept));
  } else {
    throw new Error(`Instance type [${targetConcept}] is not recoganised`);
  }

  // exclude any edges that connect nodes which do not exist
  return edges.reduce(collect, []).filter(edge => existingNodeIds.includes(edge.from) && existingNodeIds.includes(edge.to));
};

/**
 * Produces and returns nodes and edges for the neighbours of the given targetNode
 * @param {*} targetConcept the node that has been double clicked, whose neighbours are to be produced
 * @param {*} answers the untouched response of a transaction.query() that contains the neighbour concepts of targetNode
 * @param {*} typeDBTx
 */
const buildNeighbours = async (targetConcept, answers) => {
  let data = answers.map((answerGroup) => {
    const { explanation } = answerGroup;
    return Array.from(answerGroup.map().entries()).map(([typeQLVar, concept]) => ({
      typeQLVar,
      concept,
      explanation,
    }));
  }).reduce(collect, []);

  const shouldVisualiseVals = data.map(item => item.concept.isThing());

  data = data.map((item, index) => {
    item.shouldVisualise = shouldVisualiseVals[index];
    return item;
  });

  data = deduplicateConcepts(data);

  const nodes = (await Promise.all(data.filter(item => item.shouldVisualise).map(item => getNeighbourNode(item.concept, item.typeQLVar, item.explanation))))
    .reduce(collect, []);

  const nodeIds = nodes.map(node => node.id);
  nodeIds.push(targetConcept.isThing() ? targetConcept.getIID() : targetConcept.getLabel().name());
  const edges = (await Promise.all(data.filter(item => item.shouldVisualise).map(item => getNeighbourEdges(item.concept, targetConcept, nodeIds)))).reduce(collect, []);

  return { nodes, edges };
};


const updateNodesLabel = async (nodes) => {
  const updatedLabels = await Promise.all(nodes.map(async (node) => {
    if (node.iid) {
      const instance = await getConcept(node, global.typeDBTx[store.getters.activeTab]);
      const baseLabel = node.label.split('\n')[0];
      return getNodeLabelWithAttrs(baseLabel, node.typeLabel, instance);
    } else {
      return node.label;
    }
  }));

  const updatedNodes = nodes.map((node, i) => {
    node.label = updatedLabels[i];
    return node;
  });

  return updatedNodes;
};

/**
 * Produces and returns nodes and edges for the roleplayers of relation instances.
 * this function is only called when the user has chosen to enable "Load Roleplayers" query settings and set "Neighbours Limit" to higher than 0
 * @param {ConceptMap[]} answers the untouched response of a transaction.query()
 * @param {*} shouldLimit whether or not the roleplayers should be limited. false, when called to buildRPInstances for explanations
 * @param {*} typeDBTx
 */
const buildRPInstances = async (answers, currentData, shouldLimit, typeDBTx) => {
  const targetRelationIds = [];

  const getRolePlayersData = () => {
    const promises = [];
    const edges = [];
    const nodes = [];

    answers.forEach((answer) => {
      Array.from(answer.map().entries()).forEach(([typeQLVar, concept]) => {
        if (concept.isRelation()) {
          const relation = concept;
          targetRelationIds.push(relation.id);

          promises.push(new Promise((resolve) => {
            relation.asRemote(typeDBTx).getPlayersByRoleType().then((rolePlayersMap) => {
              let rpEntries = Array.from(rolePlayersMap.entries());
              if (shouldLimit) rpEntries = rpEntries.slice(0, QuerySettings.getNeighboursLimit());

              let processedEntriesCount = 0;
              rpEntries.forEach(([role, players]) => {
                if (!role.isRoot()) {
                  players.forEach((player) => {
                    player.label = player.getType().getLabel().name();
                    const edge = getEdge(relation, player, edgeTypes.instance.RELATES, role.getLabel().name());
                    getInstanceNode(player, typeQLVar, answer.explanation).then((node) => {
                      edges.push(edge);
                      nodes.push(node);
                      processedEntriesCount += 1;
                      if (processedEntriesCount === rpEntries.length) resolve({ edges, nodes });
                    });
                  });
                }
              });
            });
          }));
        }
      });
    });

    return promises;
  };

  const data = (await Promise.all(getRolePlayersData())).reduce((accumulator, item) => {
    accumulator.edges.push(...item.edges);
    accumulator.nodes.push(...item.nodes);
    return accumulator;
  }, { edges: [], nodes: [] });

  // deduplicate nodes and edges as multiple relations may share the same roleplayers, especially for highly interconnected datasets
  data.nodes = data.nodes.filter((node, index, self) => index === self.findIndex(t => t.id === node.id));
  data.edges = data.edges.filter((edge, index, self) => index === self.findIndex(t => t.id === edge.id));

  if (currentData) {
    // exclude any nodes and edges that have already been constructed and visualised (i.e. currentData)
    data.edges = data.edges.filter(nEdge => !currentData.edges.some(cEdge => cEdge.id === nEdge.id));
    data.nodes = data.nodes.filter(nNode => !currentData.nodes.some(cNode => cNode.id === nNode.id));
  }

  return data;
};

export default {
  buildInstances,
  buildTypes,
  buildRPInstances,
  getTypeNode,
  getTypeEdges,
  buildNeighbours,
  updateNodesLabel,
  getInstanceNode,
  // ideally the following functions should be private functions
  // of this module. However, this can be the case only when this
  // module becomes the only place that contains the logic for
  // constructing the nodes and edges. This will require further
  // refactoring
  getEdge,
  edgeTypes,
};
