/* eslint-disable no-await-in-loop */
import QuerySettings from '../Visualiser/RightBar/SettingsTab/QuerySettings';
import NodeSettings from '../Visualiser/RightBar/SettingsTab/DisplaySettings';
import { META_LABELS, baseTypes } from './SharedUtils';
import store from '../../store';

const convertToRemote = (concept) => {
  if (concept.asRemote) {
    const tx = store.state['schema-design'] ? global.graknTx.schemaDesign : global.graknTx[store.getters.activeTab];
    return concept.asRemote(tx);
  }
  return concept;
};

const { ENTITY_INSTANCE, RELATION_INSTANCE, ATTRIBUTE_INSTANCE, ENTITY_TYPE, RELATION_TYPE, ATTRIBUTE_TYPE } = baseTypes;

const collect = (array, current) => array.concat(current);
const deduplicateConcepts = arr => arr.filter((item, index, self) => index === self.findIndex(t => t.concept.id === item.concept.id));

const edgeTypes = {
  type: {
    HAS: 'HAS_TYPE',
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

const getConceptLabel = (concept) => {
  let label;
  if (typeof concept.label === 'string') label = concept.label;
  else if (concept.isType()) label = concept.label();
  else if (concept.isThing()) label = concept.type().label();
  return label;
};


const shouldVisualiseInstance = (instance) => {
  let shouldSkip = false;
  if (instance.type().isImplicit()) shouldSkip = true;
  return !shouldSkip;
};

const shouldVisualiseType = (type) => {
  let shouldSkip = false;
  if (type.isImplicit()) shouldSkip = true;
  else if (META_LABELS.has(getConceptLabel(type))) shouldSkip = true;
  return !shouldSkip;
};

const getEdge = (from, to, edgeType, label) => {
  const edge = { from: from.id, to: to.id };

  switch (edgeType) {
    // TYPES
    case edgeTypes.type.PLAYS:
    case edgeTypes.type.RELATES:
      edge.label = label;
      edge.arrows = { to: { enabled: true } };
      edge.options = { hideLabel: false, hideArrow: false };
      break;
    case edgeTypes.type.HAS:
      edge.label = 'has';
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
  const selectedAttrs = NodeSettings.getTypeLabels(type);
  if (selectedAttrs.length) {
    const allAttrs = await (await convertToRemote(instance).attributes()).collect();

    const promises = allAttrs.map(async attr => new Promise((resolve) => {
      attr.type().then((type) => {
        type.label().then((label) => {
          attr.value().then((value) => {
            resolve({ label, value });
          });
        });
      });
    }));
    const allAttrsData = await Promise.all(promises);
    const selectedAttrsData = allAttrsData.filter(attrData => selectedAttrs.includes(attrData.label));
    label += selectedAttrsData.map(attrData => `\n${attrData.label}: ${attrData.value}`).join();
  }

  return label;
};

/**
 * produces and returns the common node object for a concept instance
 * most node properties are available on the instance, but graqlVar and explanation
 * need to be passed to here for the ConceptMap that contained the instance
 * @param {Concept} instance guaranteed to be a concept instance
 * @param {String} graqlVar
 * @param {ConceptMap[]} explanation
 */
const buildCommonInstanceNode = (instance, graqlVar, explanation, queryPattern) => {
  const node = {};
  node.id = instance.id;
  node.baseType = instance.baseType;
  node.var = graqlVar;
  node.attrOffset = 0;
  node.type = getConceptLabel(instance);
  node.isInferred = instance.isInferred();
  node.attributes = convertToRemote(instance).attributes;
  if (node.isInferred) {
    node.explanation = explanation;
    node.queryPattern = queryPattern();
  }

  return node;
};

/**
 * produces and returns the node for the given concept instance based on is basetype
 * @param {Concept} instance guaranteed to be a concept instance
 * @param {String} graqlVar
 * @param {ConceptMap[]} explanation
 */
const getInstanceNode = async (instance, graqlVar, explanation, queryPattern) => {
  const node = buildCommonInstanceNode(instance, graqlVar, explanation, queryPattern);
  switch (instance.baseType) {
    case ENTITY_INSTANCE: {
      node.label = await getNodeLabelWithAttrs(`${node.type}: ${node.id}`, node.type, instance);
      node.offset = 0;
      break;
    }
    case RELATION_INSTANCE: {
      node.label = '';
      if (QuerySettings.getRolePlayersStatus()) {
        node.offset = QuerySettings.getNeighboursLimit();
      } else {
        node.offset = 0;
      }
      break;
    }
    case ATTRIBUTE_INSTANCE: {
      node.label = await getNodeLabelWithAttrs(`${node.type}: ${node.value}`, node.type, instance);
      node.value = instance.value();
      node.offset = 0;
      break;
    }
    default:
      throw new Error(`Instance type [${instance.baseType}] is not recoganised`);
  }

  return node;
};

/**
 * produces the `has` edges from owners to the given attribute instance
 * @param {Concept} attribute must be an attribute instance
 */
const getInstanceHasEdges = async (attribute) => {
  const owners = (await (await convertToRemote(attribute).owners()).collect());
  const edges = owners.map(owner => getEdge(owner, attribute, edgeTypes.instance.HAS));
  return edges;
};

/**
 * produces the `role` edges from the given relation instance to its roleplayers
 * @param {Concept} relation must be a relation instance
 */
// eslint-disable-next-line no-unused-vars
const getInstanceRelatesEdges = async (relation) => {
  const rpMap = await convertToRemote(relation).rolePlayersMap();
  const rpMapEntries = Array.from(rpMap.entries());

  const edges = (await Promise.all(
    rpMapEntries.map(([role, players]) => role.label().then(label =>
      Array.from(players.values()).reduce(collect, []).map(player => getEdge(relation, player, edgeTypes.instance.RELATES, label)),
    )))
  ).reduce(collect, []);
  return [edges];
};

const getInstanceIsaEdges = (instance) => {
  const type = instance.type();
  return getEdge(instance, type, edgeTypes.instance.ISA);
};

/**
 * produces and returns the edges for the given concept instance
 * @param {Thing} instance must be a concept instance
 */
const getInstanceEdges = async (instance, existingNodeIds) => {
  const edges = [];
  switch (instance.baseType) {
    case ATTRIBUTE_INSTANCE:
      edges.push(getInstanceIsaEdges(instance));
      edges.push(...await getInstanceHasEdges(instance));
      break;
    case RELATION_INSTANCE:
      edges.push(getInstanceIsaEdges(instance));
      edges.push(...await getInstanceRelatesEdges(instance));
      break;
    case ENTITY_INSTANCE:
      edges.push(getInstanceIsaEdges(instance));
      break;
    default:
      throw new Error(`Instance type [${instance.baseType}] is not recoganised`);
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
  let data = answers.map((answerGroup) => {
    const { explanation, queryPattern } = answerGroup;
    return Array.from(answerGroup.map().entries()).map(([graqlVar, concept]) => ({
      graqlVar,
      concept,
      explanation,
      queryPattern,
    }));
  }).reduce(collect, []);

  const shouldVisualiseVals = data.map(item => item.concept.isThing() && shouldVisualiseInstance(item.concept));
  data = data.map((item, index) => {
    item.shouldVisualise = shouldVisualiseVals[index];
    return item;
  });

  data = deduplicateConcepts(data);

  const nodes = (await Promise.all(data.filter(item => item.shouldVisualise).map(item => getInstanceNode(item.concept, item.graqlVar, item.explanation, item.queryPattern))))
    .reduce(collect, []);
  const nodeIds = nodes.map(node => node.id);
  const edges = (await Promise.all(data.filter(item => item.shouldVisualise).map(item => getInstanceEdges(item.concept, nodeIds)))).reduce(collect, []);

  return { nodes, edges };
};

/**
 * produce and return the node for the given concept type based on its subtype
 * @param {Concept} type guaranteed to be a concept type
 * @param {String} graqlVar
 */
const getTypeNode = (type, graqlVar) => {
  const node = {};
  switch (type.baseType) {
    case ENTITY_TYPE:
    case RELATION_TYPE:
    case ATTRIBUTE_TYPE: {
      node.id = type.id;
      node.baseType = type.baseType;
      node.var = graqlVar;
      node.attrOffset = 0;
      node.offset = 0;
      node.label = getConceptLabel(type);
      node.attributes = type.attributes;
      node.playing = type.playing;
      break;
    }
    default:
      throw new Error(`Concept type [${type.baseType}] is not recoganised`);
  }

  return node;
};

/**
 * produces and returns the edge from the given concept type to its super type
 * given that the supertype in question isn't a meta type
 * @param {Concept} type must be a concept type
 */
const getTypeSubEdge = async (type) => {
  const sup = await convertToRemote(type).sup();
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

  const sup = await convertToRemote(type).sup();

  if (sup) {
    const typesAttrs = await (await convertToRemote(type).attributes()).collect();
    if (sup.baseType === 'META_TYPE') {
      edges = typesAttrs.map(attr => getEdge(type, attr, edgeTypes.type.HAS));
    } else { // if type has a super type which is not a META_CONCEPT construct edges to attributes except those which are inherited from its super type
      const supAttrIds = (await (await sup.attributes()).collect()).map(x => x.id);
      const supAttrs = typesAttrs.filter(attr => !supAttrIds.includes(attr.id));
      edges = supAttrs.map(attr => getEdge(type, attr, edgeTypes.type.HAS));
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
  const playRoles = await (await convertToRemote(type).playing()).collect();
  const edges = (await Promise.all(playRoles.map(role =>
    role.label().then(label =>
      role.relations().then(relationsIterator =>
        relationsIterator.collect().then(relations =>
          relations.map(relation => getEdge(relation, type, edgeTypes.type.PLAYS, label)),
        ),
      ),
    ),
  ))).reduce(collect, []);
  return edges;
};

/**
 * produces and returns the edges from the relation type to its roleplayer types
 * @param {Concept} type must be a concept relation type
 */
const getTypeRelatesEdges = async (type) => {
  const roles = await (await convertToRemote(type).roles()).collect();
  const edges = (await Promise.all(roles.map(role =>
    role.label().then(label =>
      role.players().then(playersIterator =>
        playersIterator.collect().then(players =>
          players.map(player => getEdge(type, player, edgeTypes.type.RELATES, label)),
        ),
      ),
    ),
  ))).reduce(collect, []);

  return edges;
};

/**
 * produces and returns edges for the given type depending on its basetype
 * @param {Concept} type must be a concept type
 */
const getTypeEdges = async (type, existingNodeIds) => {
  const edges = [];

  switch (type.baseType) {
    case ENTITY_TYPE:
    case ATTRIBUTE_TYPE:
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
      throw new Error(`Concept type [${type.baseType}] is not recoganised`);
  }

  // exclude any edges that connect nodes which do not exist
  return edges.filter(edge => existingNodeIds.includes(edge.from) && existingNodeIds.includes(edge.to));
};

/**
 * produces and returns nodes and edges for the given concept type
 * @param {Concept} type guaranteed to be a concept type
 * @param {String} graqlVar the Graql variable (as written in the original query) which holds the concept
 */
const buildType = async (type, graqlVar = '') => {
  const node = getTypeNode(type, graqlVar);
  const edges = await getTypeEdges(type, [node.id]);
  return { node, edges };
};

/**
 * produces and returns nodes and edges for the given answers
 * only the types stored within the answers are processed
 * @param {ConceptMap[]} answers the untouched response of a transaction.query()
 */
const buildTypes = async (answers) => {
  let data = answers.map(answerGroup => Array.from(answerGroup.map().entries()).map(([graqlVar, concept]) => ({
    graqlVar,
    concept,
  }))).reduce(collect, []);

  const shouldVisualiseVals = data.map(item => item.concept.isType() && shouldVisualiseType(item.concept));

  data = data.map((item, index) => {
    item.shouldVisualise = shouldVisualiseVals[index];
    return item;
  });

  const nodes = data.filter(item => item.shouldVisualise).map(item => getTypeNode(item.concept, item.graqlVar));
  const nodeIds = nodes.map(node => node.id);
  const edges = (await Promise.all(data.filter(item => item.shouldVisualise).map(item => getTypeEdges(item.concept, nodeIds)))).reduce(collect, []);

  return { nodes, edges };
};

/**
 * produces and returns the node for the given concept based on its type
 * @param {*} concept
 * @param {*} graqlVar
 * @param {*} explanation
 */
const getNeighbourNode = (concept, graqlVar, explanation, queryPattern) => {
  let node;
  if (concept.isType()) {
    node = getTypeNode(concept, graqlVar);
  } else if (concept.isThing()) {
    node = getInstanceNode(concept, graqlVar, explanation, queryPattern);
  }
  return node;
};

/**
 * Prroduces and returns the edges that connect the given targetNode with its neighbourNode
 * @param {*} neighbourConcept
 * @param {*} targetNode the node whose neighbour edges are to be produced
 * @param {*} graknTx
 */
const getNeighbourEdges = async (neighbourConcept, targetConcept) => {
  const edges = [];

  switch (targetConcept.baseType) {
    case ENTITY_TYPE:
    case ATTRIBUTE_TYPE:
    case RELATION_TYPE:
      edges.push(getEdge(neighbourConcept, targetConcept, edgeTypes.instance.ISA));
      break;

    case ENTITY_INSTANCE:
      if (neighbourConcept.baseType === 'RELATION') {
        const relation = neighbourConcept;
        edges.push(...await getInstanceRelatesEdges(relation));
      }
      break;

    case ATTRIBUTE_INSTANCE: {
      const owner = neighbourConcept;
      edges.push(getEdge(owner, targetConcept, edgeTypes.instance.HAS));
      break;
    }

    case RELATION_INSTANCE: {
      edges.push(...await getInstanceRelatesEdges(targetConcept));
      break;
    }

    default:
      throw new Error(`Instance type [${targetConcept.baseType}] is not recoganised`);
  }
  return edges.reduce(collect, []);
};

/**
 * Produces and returns nodes and edges for the neihbours of the given targetNode
 * @param {*} targetNode the node that has been double clicked, whose neighbours are to be produced
 * @param {*} answers the untouched response of a transaction.query() that contains the neioghbour concepts of targetNode
 * @param {*} graknTx
 */
const buildNeighbours = async (targetConcept, answers) => {
  let data = answers.map((answerGroup) => {
    const { explanation, queryPattern } = answerGroup;
    return Array.from(answerGroup.map().entries()).map(([graqlVar, concept]) => ({
      graqlVar,
      concept,
      explanation,
      queryPattern,
    }));
  }).reduce(collect, []);

  const shouldVisualiseVals = data.map(item => item.concept.isThing() && shouldVisualiseInstance(item.concept));

  data = data.map((item, index) => {
    item.shouldVisualise = shouldVisualiseVals[index];
    return item;
  });

  data = deduplicateConcepts(data);

  const nodes = data.filter(item => item.shouldVisualise).map(item => getNeighbourNode(item.concept, item.graqlVar, item.explanation, item.queryPattern));
  const edges = (await Promise.all(data.filter(item => item.shouldVisualise).map(item => getNeighbourEdges(item.concept, targetConcept)))).reduce(collect, []);

  return { nodes, edges };
};

const getNodesWithUpdatedLabel = async (nodes, type) => {
  const targetNodes = nodes.filter(x => x.type === type);
  const updatedLabels = await Promise.all(targetNodes.map(async (node) => {
    const instance = await global.graknTx[store.getters.activeTab].getConcept(node.id);
    const baseLabel = node.label.split('\n')[0];
    return getNodeLabelWithAttrs(baseLabel, type, instance);
  }));

  const updatedNodes = targetNodes.map((node, i) => {
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
 * @param {*} graknTx
 */
const buildRPInstances = async (answers, currentData, shouldLimit, graknTx) => {
  const getRolePlayersData = () => {
    const promises = [];
    const edges = [];
    const nodes = [];

    answers.forEach((answer) => {
      Array.from(answer.map().entries()).forEach(([graqlVar, concept]) => {
        if (concept.isRelation() && shouldVisualiseInstance(concept)) {
          const relation = concept;

          promises.push(new Promise((resolve) => {
            relation.asRemote(graknTx).rolePlayersMap().then((rolePlayersMap) => {
              let rpEntries = Array.from(rolePlayersMap.entries());
              if (shouldLimit) rpEntries = rpEntries.slice(0, QuerySettings.getNeighboursLimit());
              rpEntries.forEach(([role, players], i) => {
                role.label().then((edgeLabel) => {
                  players.forEach((player, j) => {
                    player.type().then(type => type.label().then((playerLabel) => {
                      player.label = playerLabel;
                      const edge = getEdge(relation, player, edgeTypes.instance.RELATES, edgeLabel);
                      getInstanceNode(player, graqlVar, answer.explanation, answer.queryPattern).then((node) => {
                        edges.push(edge);
                        nodes.push(node);
                        const isLastRole = i === rpEntries.length - 1;
                        const isLastPlayer = j === players.length - 1;
                        if (isLastRole && isLastPlayer) resolve({ edges, nodes });
                      });
                    }));
                  });
                });
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

  // exclude any edges that have already been produced by this module (i.e. currentData)
  if (currentData) data.edges = data.edges.filter(nEdge => !currentData.edges.some(cEdge => cEdge.id === nEdge.id));
  return data;
};

export default {
  buildInstances,
  buildTypes,
  buildType,
  buildRPInstances,
  getTypeEdges,
  getTypeSubEdge,
  getTypeRelatesEdges,
  getTypeAttributeEdges,
  buildNeighbours,
  getNodesWithUpdatedLabel,
  // ideally the following functions should be private functions
  // of this module. However, this can be the case only when this
  // module becomes the only place that contains the logic for
  // constructing the nodes and edges. This will require further
  // refactoring
  getEdge,
  edgeTypes,
};
