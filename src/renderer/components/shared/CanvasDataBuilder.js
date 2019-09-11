/* eslint-disable no-await-in-loop */
import QuerySettings from '../Visualiser/RightBar/SettingsTab/QuerySettings';
import { META_LABELS, baseTypes } from './SharedUtils';
const { ENTITY_INSTANCE, RELATION_INSTANCE, ATTRIBUTE_INSTANCE, ENTITY_TYPE, RELATION_TYPE, ATTRIBUTE_TYPE } = baseTypes;

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
    SUB: 'SUBS_INSTANCE',
  },
};

const getConceptLabel = async (concept) => {
  let label;
  if (typeof concept.label === 'string') label = concept.label;
  else if (concept.isType()) label = await concept.label();
  else if (concept.isThing()) label = await (await concept.type()).label();
  return label;
};

const shouldVisualiseInstance = async (instance) => {
  let shouldSkip = false;
  if ((await (await instance.type()).isImplicit())) shouldSkip = true;
  return !shouldSkip;
};

const shouldVisualiseType = async (type) => {
  let shouldSkip = false;
  if (await type.isImplicit()) shouldSkip = true;
  else if (META_LABELS.has(await getConceptLabel(type))) shouldSkip = true;
  return !shouldSkip;
};

const getEdge = async (from, to, edgeType, label) => {
  let edge;

  switch (edgeType) {
    case edgeTypes.type.PLAYS:
    case edgeTypes.type.RELATES:
      edge = { from: from.id, to: to.id, label, options: { hideLabel: false, hideArrow: false } };
      break;
    case edgeTypes.instance.RELATES:
    case edgeTypes.instance.PLAYS:
      edge = { from: from.id, to: to.id, label, options: { hideLabel: true, hideArrow: true } };
      break;
    case edgeTypes.type.HAS:
      edge = { from: from.id, to: to.id, label: 'has', options: { hideLabel: false, hideArrow: false } };
      break;
    case edgeTypes.instance.HAS:
      edge = { from: from.id, to: to.id, label: 'has', options: { hideLabel: true, hideArrow: true } };
      break;
    case edgeTypes.type.SUB:
      edge = { from: from.id, to: to.id, label: 'sub', options: { hideLabel: false, hideArrow: false } };
      break;
    case edgeTypes.instance.SUB:
      edge = { from: from.id, to: to.id, label: 'sub', options: { hideLabel: true, hideArrow: true } };
      break;
    default:
      break;
  }

  return edge;
};

/**
 * produces and returns the common node object for a concept instance
 * most node properties are available on the instance, but graqlVar and explanation
 * need to be passed to here for the ConceptMap that contained the instance
 * @param {Concept} instance guaranteed to be a concept instance
 * @param {String} graqlVar
 * @param {ConceptMap[]} explanation
 */
const buildCommonInstanceNode = async (instance, graqlVar, explanation) => {
  const node = {};
  node.id = instance.id;
  node.baseType = instance.baseType;
  node.var = graqlVar;
  node.explanation = explanation();
  node.attrOffset = 0;
  node.type = await getConceptLabel(instance);
  node.isInferred = await instance.isInferred();
  node.attributes = instance.attributes;
  // this is required for the post-processing jobs
  // TODO: handle post-processing here, so that we don't have to include
  // concept-specific properties within the node objects
  node.txService = instance.txService;

  return node;
};

/**
 * produces and returns the node for the given concept instance based on is basetype
 * @param {Concept} instance guaranteed to be a concept instance
 * @param {String} graqlVar
 * @param {ConceptMap[]} explanation
 */
const getInstanceNode = async (instance, graqlVar, explanation) => {
  const node = await buildCommonInstanceNode(instance, graqlVar, explanation);

  switch (instance.baseType) {
    case ENTITY_INSTANCE: {
      node.label = `${node.type}: ${node.id}`;
      node.offset = 0;
      break;
    }
    case RELATION_INSTANCE: {
      node.label = '';
      node.offset = QuerySettings.getNeighboursLimit();
      break;
    }
    case ATTRIBUTE_INSTANCE: {
      node.value = await instance.value();
      node.label = `${node.type}: ${node.value}`;
      node.offset = 0;
      break;
    }
    default:
      break;
  }

  return node;
};

/**
 * produces and returns the edges for the given concept instance
 * @param {Thing} instance must be a concept instance
 */
const getInstanceEdges = async (instance) => {
  if (instance.isAttribute()) {
    const owners = (await (await instance.owners()).collect());
    const edges = await Promise.all(owners.map(owner => getEdge(owner, instance, edgeTypes.instance.HAS)));
    return edges;
  }
  return [];
};

/**
 * produces and returns nodes and edges for the given answers
 * only the instances stored within the answers are processed.
 * @param {ConceptMap[]} answers the untouched response of a transaction.query()
 */
const buildInstances = async (answers) => {
  const nodes = [];
  const edges = [];

  for (let i = 0; i < answers.length; i += 1) {
    const answer = answers[i];
    const answersGroup = Array.from(answer.map().entries());

    for (let j = 0; j < answersGroup.length; j += 1) {
      const [graqlVar, instance] = answersGroup[j];
      if (instance.isThing() && await shouldVisualiseInstance(instance)) {
        nodes.push(await getInstanceNode(instance, graqlVar, answer.explanation));
        edges.push(...await getInstanceEdges(instance));
      }
    }
  }

  return { nodes, edges };
};

/**
 * produce and return the node for the given concept type based on its subtype
 * @param {Concept} type guaranteed to be a concept type
 * @param {String} graqlVar
 */
const getTypeNode = async (type, graqlVar) => {
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
      node.label = await getConceptLabel(type);
      node.attributes = type.attributes;
      node.playing = type.playing;
      // this is required for the post-processing jobs
      // TODO: handle post-processing here, so that we don't have to include
      // concept-specific properties within the node objects
      node.txService = type.txService;
      break;
    }
    default:
      break;
  }

  return node;
};

/**
 * produces and returns the edge from the given concept type to its super type
 * given that the supertype in question isn't a meta type
 * @param {Concept} type must be a concept type
 */
const getTypeSubEdge = async (type) => {
  const sup = await type.sup();
  const supLabel = await sup.label();
  if (sup && !META_LABELS.has(supLabel)) return [await getEdge(type, sup, edgeTypes.type.SUB)];
  return [];
};

/**
 * produces and returns the edges from the given concept type to the attribute
 * types which it owns
 * @param {Concept} type must be a concept type
 */
const getTypeAttributeEdges = async (type) => {
  let edges = [];

  const sup = await type.sup();

  if (sup) {
    const supLabel = await sup.label();
    const typesAttrs = await (await type.attributes()).collect();

    if (META_LABELS.has(supLabel)) {
      edges = await Promise.all(typesAttrs.map(attr => getEdge(type, attr, edgeTypes.type.HAS)));
    } else { // if type has a super type which is not a META_CONCEPT construct edges to attributes except those which are inherited from its super type
      const supAttrIds = (await (await sup.attributes()).collect()).map(x => x.id);
      const supAttrs = typesAttrs.filter(attr => !supAttrIds.includes(attr.id));
      edges = await Promise.all(supAttrs.map(attr => getEdge(type, attr, edgeTypes.type.HAS)));
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
  const playRoles = await (await type.playing()).collect();

  for (let i = 0; i < playRoles.length; i += 1) {
    const role = playRoles[i];
    const roleLabel = await role.label();
    const relations = await (await role.relations()).collect();
    const edges = await Promise.all(relations.map(relation => getEdge(relation, type, edgeTypes.type.PLAYS, roleLabel)));
    return edges;
  }

  return [];
};

/**
 * produces and returns the edges from the relation type to its roleplayer types
 * @param {Concept} type must be a concept relation type
 */
const getTypeRelatesEdges = async (type) => {
  let edges = [];

  const roles = await (await type.roles()).collect();

  for (let i = 0; i < roles.length; i += 1) {
    const role = roles[i];
    const roleLabel = await role.label();
    const roleplayers = await (await role.players()).collect();
    // eslint-disable-next-line no-loop-func
    roleplayers.forEach((rp) => { edges.push(getEdge(type, rp, edgeTypes.type.RELATES, roleLabel)); });
  }

  edges = await Promise.all(edges);

  return edges;
};

/**
 * produces and returns edges for the given type depending on its basetype
 * @param {Concept} type must be a concept type
 */
const getTypeEdges = async (type) => {
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
      break;
  }

  return edges;
};

/**
 * produces and returns nodes and edges for the given concept type
 * @param {Concept} type guaranteed to be a concept type
 * @param {String} graqlVar the Graql variable (as written in the original query) which holds the concept
 */
const buildType = async (type, graqlVar = '') => {
  const node = await getTypeNode(type, graqlVar);
  const edges = await getTypeEdges(type);
  return { node, edges };
};

/**
 * produces and returns nodes and edges for the given answers
 * only the types stored within the answers are processed
 * @param {ConceptMap[]} answers the untouched response of a transaction.query()
 */
const buildTypes = async (answers) => {
  const nodes = [];
  const edges = [];

  for (let i = 0; i < answers.length; i += 1) {
    const answer = answers[i];
    const answersGroup = Array.from(answer.map().entries());

    for (let j = 0; j < answersGroup.length; j += 1) {
      const [graqlVar, type] = answersGroup[j];
      if (type.isType() && await shouldVisualiseType(type)) {
        const typeData = await buildType(type, graqlVar);
        nodes.push(typeData.node);
        edges.push(...typeData.edges);
      }
    }
  }

  return { nodes, edges };
};

/**
 * Produces and returns nodes and edges for the roleplayers of relation instances.
 * this function is only called when the user has chosen to enable "Load Roleplayers" query settings and set "Neighbours Limit" to higher than 0
 * @param {ConceptMap[]} answers the untouched response of a transaction.query()
 * @param {*} shouldLimit whether or not the roleplayers should be limited. false, when called to buildRPInstances for explanations
 * @param {*} graknTx
 */
const buildRPInstances = async (answers, shouldLimit, graknTx) => {
  const edges = [];
  const nodes = [];

  for (let i = 0; i < answers.length; i += 1) {
    const answer = answers[i];
    const answersGroup = Array.from(answer.map().entries());

    for (let j = 0; j < answersGroup.length; j += 1) {
      const [graqlVar, instance] = answersGroup[j];

      if (instance.isRelation() && await shouldVisualiseInstance(instance)) {
        let queryToGetRPs = `match $r id ${instance.id}; $r($rl: $rp); not { $rl type role; }; get $rp, $rl; offset 0; `;
        if (shouldLimit) queryToGetRPs += `limit ${QuerySettings.getNeighboursLimit()};`;

        const answers = await (await graknTx.query(queryToGetRPs)).collect();

        for (let k = 0; k < answers.length; k += 1) {
          const rolesAndRps = Array.from(answers[k].map().values());
          const role = rolesAndRps.filter(x => x.isRole())[0];
          const roleplayers = rolesAndRps.filter(x => !x.isRole());
          const edgeLabel = await role.label();

          for (let l = 0; l < roleplayers.length; l += 1) {
            const rp = roleplayers[l];
            if (rp.isThing() && await shouldVisualiseInstance(rp)) {
              edges.push(await getEdge(instance, rp, edgeTypes.instance.RELATES, edgeLabel));
              nodes.push(await getInstanceNode(rp, graqlVar, answer.explanation));
            }
          }
        }
      }
    }
  }

  return { nodes, edges };
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
};
