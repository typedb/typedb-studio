/* eslint-disable no-await-in-loop */
import QuerySettings from '../Visualiser/RightBar/SettingsTab/QuerySettings';
import { META_LABELS, baseTypes } from './SharedUtils';

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
    case edgeTypes.instance.RELATES:
    case edgeTypes.instance.PLAYS:
      edge = { from: from.id, to: to.id, label };
      break;
    case edgeTypes.type.HAS:
    case edgeTypes.instance.HAS:
      edge = { from: from.id, to: to.id, label: 'has' };
      break;
    case edgeTypes.type.SUB:
    case edgeTypes.instance.SUB:
      edge = { from: from.id, to: to.id, label: 'sub' };
      break;
    default:
      break;
  }

  return edge;
};

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

const getInstanceNode = async (instance, graqlVar, explanation) => {
  const node = await buildCommonInstanceNode(instance, graqlVar, explanation);

  const { ENTITY_INSTANCE, RELATION_INSTANCE, ATTRIBUTE_INSTANCE } = baseTypes;

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

const getInstanceEdges = async (instance, instanceIds) => {
  let edges = [];

  if (instance.baseType === baseTypes.ATTRIBUTE_INSTANCE) {
    const owners = (await (await instance.owners()).collect()).filter(owner => instanceIds.includes(owner.id));
    owners.forEach((owner) => { edges.push(getEdge(owner, instance, edgeTypes.instance.HAS)); });
  }

  edges = await Promise.all(edges);

  return edges;
};

const buildInstances = async (answers) => {
  let nodes = [];
  let edges = [];

  const instanceIds = answers.map(answer => Array.from(answer.map().values())).flatMap(x => x).map(instance => instance.id);

  for (let i = 0; i < answers.length; i += 1) {
    const answer = answers[i];
    const answersGroup = Array.from(answer.map().entries());

    for (let j = 0; j < answersGroup.length; j += 1) {
      const [graqlVar, instance] = answersGroup[j];
      if (instance.isThing() && await shouldVisualiseInstance(instance)) {
        nodes.push(getInstanceNode(instance, graqlVar, answer.explanation));
        edges.push(getInstanceEdges(instance, instanceIds));
      }
    }
  }

  nodes = await Promise.all(nodes);
  edges = (await Promise.all(edges)).flatMap(x => x);

  return { nodes, edges };
};

const getTypeNode = async (type, graqlVar) => {
  const { ENTITY_TYPE, RELATION_TYPE, ATTRIBUTE_TYPE } = baseTypes;
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

const getTypeSubEdge = async (type) => {
  const sup = await type.sup();
  const supLabel = await sup.label();
  if (sup && !META_LABELS.has(supLabel)) return [await getEdge(type, sup, edgeTypes.type.SUB)];
  return [];
};

const getTypeAttributeEdges = async (type) => {
  let edges = [];

  const sup = await type.sup();

  if (sup) {
    const supLabel = await sup.label();
    const typesAttrs = await (await type.attributes()).collect();

    if (META_LABELS.has(supLabel)) {
      typesAttrs.forEach(attr => edges.push(getEdge(type, attr, edgeTypes.type.HAS)));
    } else { // if type has a super type which is not a META_CONCEPT construct edges to attributes except those which are inherited from its super type
      const supAttrIds = (await (await sup.attributes()).collect()).map(x => x.id);
      const supAttrs = typesAttrs.filter(attr => !supAttrIds.includes(attr.id));
      supAttrs.forEach(attr => edges.push(getEdge(type, attr, edgeTypes.type.HAS)));
    }
  }

  edges = await Promise.all(edges);

  return edges;
};

const getTypePlayEdges = async (type) => {
  let edges = [];
  const playRoles = await (await type.playing()).collect();

  for (let i = 0; i < playRoles.length; i += 1) {
    const role = playRoles[i];
    const roleLabel = await role.label();
    const relations = await (await role.relations()).collect();
    // eslint-disable-next-line no-loop-func
    relations.forEach((relation) => { edges.push(getEdge(relation, type, edgeTypes.type.PLAYS, roleLabel)); });
  }

  edges = await Promise.all(edges);

  return Promise.all(edges);
};

const getTypeRoleEdges = async (type) => {
  if (!type.isRelationType()) return [];

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

const getTypeEdges = async (type) => {
  const edges = [];

  const { ENTITY_TYPE, RELATION_TYPE, ATTRIBUTE_TYPE } = baseTypes;

  switch (type.baseType) {
    case ENTITY_TYPE:
      edges.push(...await getTypeSubEdge(type));
      edges.push(...await getTypeAttributeEdges(type));
      edges.push(...await getTypePlayEdges(type));
      break;
    case RELATION_TYPE:
      edges.push(...await getTypeSubEdge(type));
      edges.push(...await getTypeAttributeEdges(type));
      edges.push(...await getTypePlayEdges(type));
      edges.push(...await getTypeRoleEdges(type));
      break;
    case ATTRIBUTE_TYPE:
      edges.push(...await getTypeSubEdge(type));
      edges.push(...await getTypeAttributeEdges(type));
      break;
    default:
      break;
  }

  return edges;
};

const buildType = async (type, graqlVar = '') => {
  const node = await getTypeNode(type, graqlVar);
  const edges = await getTypeEdges(type);
  return { node, edges };
};

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
          const role = rolesAndRps.filter(x => x.baseType === baseTypes.ROLE)[0];
          const roleplayers = rolesAndRps.filter(x => x.baseType !== baseTypes.ROLE);
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
  getTypeRoleEdges,
  getTypeAttributeEdges,
};
