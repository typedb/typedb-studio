/* eslint-disable no-await-in-loop */
import { buildLabel } from '../Visualiser/VisualiserGraphBuilder';
import QuerySettings from '../Visualiser/RightBar/SettingsTab/QuerySettings';

export const META_LABELS = new Set(['entity', 'relation', 'attribute', 'role', 'thing']);

export async function ownerHasEdges(nodes) {
  const edges = [];

  await Promise.all(nodes.map(async (node) => {
    const sup = await node.sup();
    if (sup) {
      const supLabel = await sup.label();
      if (META_LABELS.has(supLabel)) {
        let attributes = await node.attributes();
        attributes = await attributes.collect();
        attributes.map(attr => edges.push({ from: node.id, to: attr.id, label: 'has' }));
      } else { // if node has a super type which is not a META_CONCEPT construct edges to attributes expect those which are inherited from its super type
        const supAttributeIds = (await (await sup.attributes()).collect()).map(x => x.id);
        const attributes = (await (await node.attributes()).collect()).filter(attr => !supAttributeIds.includes(attr.id));
        attributes.map(attr => edges.push({ from: node.id, to: attr.id, label: 'has' }));
      }
    }
  }));
  return edges;
}

export async function relationTypesOutboundEdges(nodes) {
  const edges = [];
  const promises = nodes.filter(x => x.isRelationType())
    .map(async rel =>
      Promise.all(((await (await rel.roles()).collect())).map(async (role) => {
        const types = await (await role.players()).collect();
        const label = await role.label();
        return types.forEach((type) => { edges.push({ from: rel.id, to: type.id, label }); });
      })),
    );
  await Promise.all(promises);
  return edges;
}

export async function computeSubConcepts(nodes) {
  const edges = [];
  const subConcepts = [];
  await Promise.all(nodes.map(async (concept) => {
    const sup = await concept.sup();
    if (sup) {
      const supLabel = await sup.label();
      if (!META_LABELS.has(supLabel)) {
        edges.push({ from: concept.id, to: sup.id, label: 'sub' });
        subConcepts.push(concept);
      }
    }
  }));
  return { nodes: subConcepts, edges };
}

export const baseTypes = {
  TYPE: 'META_TYPE',
  ENTITY_TYPE: 'ENTITY_TYPE',
  RELATION_TYPE: 'RELATION_TYPE',
  ATTRIBUTE_TYPE: 'ATTRIBUTE_TYPE',
  RULE: 'RULE',
  ROLE: 'ROLE',
  ENTITY_INSTANCE: 'ENTITY',
  RELATION_INSTANCE: 'RELATION',
  ATTRIBUTE_INSTANCE: 'ATTRIBUTE',
};

async function getTypeOfInstance(concept) {
  const type = await (await concept.type()).label();
  return type;
}

async function getSubEdges(concept) {
  const sup = await concept.sup();
  if (sup && !META_LABELS.has(await sup.label())) return { from: concept.id, to: sup.id, label: 'sub' };
  return {};
}

export async function getAttributeEdges(concept) {
  const edges = [];

  const sup = await concept.sup();

  if (sup) {
    const supLabel = await sup.label();
    const conceptsAttrs = await (await concept.attributes()).collect();

    if (META_LABELS.has(supLabel)) {
      conceptsAttrs.forEach(attr => edges.push({ from: concept.id, to: attr.id, label: 'has' }));
    } else { // if concept has a super type which is not a META_CONCEPT construct edges to attributes except those which are inherited from its super type
      const supAttrIds = (await (await sup.attributes()).collect()).map(x => x.id);
      const supAttrs = conceptsAttrs.filter(attr => !supAttrIds.includes(attr.id));
      supAttrs.forEach(attr => edges.push({ from: concept.id, to: attr.id, label: 'has' }));
    }
  }

  return edges;
}

async function getAutoRPData(concept, shouldLimit, graknTx) {
  const queryToGetRPs =
    `match $r id ${concept.id}; $r($rl: $rp); not { $rl type role; }; get $rp, $rl; offset 0; ${shouldLimit ? `limit ${QuerySettings.getNeighboursLimit()};` : ''}`;
  const rpsResult = await (await graknTx.query(queryToGetRPs)).collect();

  const edges = [];

  for (let k = 0; k < rpsResult.length; k += 1) {
    const rolesAndRps = Array.from(rpsResult[k].map().values());
    const role = rolesAndRps.filter(x => x.baseType === baseTypes.ROLE)[0];
    const edgeLabel = await role.label();
    const roleplayers = rolesAndRps.filter(x => x.baseType !== baseTypes.ROLE);
    roleplayers.forEach((rp) => {
      const relEdge = { from: concept.id, to: rp.id, label: edgeLabel };
      edges.push(relEdge);
    });
  }

  // eslint-disable-next-line no-use-before-define
  const { nodes } = await getNodesAndEdges(rpsResult, false);

  return { nodes, edges };
}

/** GET EDGES */
async function getEdgesForType(concept) {
  const edges = [];
  edges.push(await getSubEdges(concept));
  edges.push(...await getAttributeEdges(concept));

  return edges;
}

export async function getRoleEdges(concept) {
  const edges = [];

  const roles = await (await concept.roles()).collect();

  for (let i = 0; i < roles.length; i += 1) {
    const role = roles[i];
    const roleplayers = await (await role.players()).collect();
    const roleLabel = await role.label();
    roleplayers.forEach((rp) => { edges.push({ from: concept.id, to: rp.id, label: roleLabel }); });
  }

  return edges;
}

export async function getEdgesForRelationType(concept) {
  const edges = [];
  edges.push(...await getEdgesForType(concept));
  edges.push(...await getRoleEdges(concept));
  return edges;
}

export async function getEdgesForAttributeType(concept) {
  const edges = await getEdgesForType(concept);
  return edges;
}

export async function getEdgesForEntityType(concept) {
  const edges = await getEdgesForType(concept);
  return edges;
}

export async function getEdgesForAttributeInstance(concept, thingIds) {
  const owners = (await (await concept.owners()).collect()).filter(owner => thingIds.includes(owner.id));
  const edges = owners.map(owner => ({ from: owner.id, to: concept.id, label: 'has' }));
  return edges;
}
/** END OF GET EDGES */


/** GET NODE */
export async function getNodeForType(concept, graqlVar = '') {
  concept.var = graqlVar;
  concept.attrOffset = 0;
  concept.offset = 0;
  concept.label = typeof concept.label === 'string' ? concept.label : await concept.label();

  return concept;
}

export async function getNodeForEntityType(concept, graqlVar) {
  const node = await getNodeForType(concept, graqlVar);
  return node;
}

export async function getNodeForRelationType(concept, graqlVar) {
  const node = await getNodeForType(concept, graqlVar);
  return node;
}

export async function getNodeForAttributeType(concept, graqlVar) {
  const node = await getNodeForType(concept, graqlVar);
  return node;
}

async function getNodeForInstance(concept, graqlVar, explanation) {
  concept.var = graqlVar;
  concept.explanation = explanation();
  concept.attrOffset = 0;
  concept.offset = 0;
  concept.type = await getTypeOfInstance(concept);
  concept.isInferred = await concept.isInferred();

  return concept;
}

export async function getNodeForEntityInstance(concept, graqlVar, explanation) {
  const node = await getNodeForInstance(concept, graqlVar, explanation);
  node.label = await buildLabel(node);
  return node;
}

export async function getNodeForRelationInstance(concept, graqlVar, explanation) {
  const node = await getNodeForInstance(concept, graqlVar, explanation);
  node.offset = QuerySettings.getNeighboursLimit();
  return node;
}

export async function getNodeForAttributeInstance(concept, graqlVar, explanation) {
  const node = await getNodeForInstance(concept, graqlVar, explanation);
  node.label = await buildLabel(node);
  node.value = await concept.value();
  return node;
}
/** END OF GET NODE */

export async function constructNodesAndEdges(data, shouldLoadRPs, shouldLimit, graknTx) {
  const nodes = [];
  const edges = [];

  try {
    const { concept, graqlVar, explanation, thingIds } = data;

    const { ENTITY_TYPE, RELATION_TYPE, ATTRIBUTE_TYPE, ENTITY_INSTANCE, RELATION_INSTANCE, ATTRIBUTE_INSTANCE } = baseTypes;

    switch (concept.baseType) {
      case ENTITY_TYPE:
        nodes.push(await getNodeForEntityType(concept, graqlVar));
        edges.push(...await getEdgesForEntityType(concept));
        break;

      case RELATION_TYPE:
        nodes.push(await getNodeForRelationType(concept, graqlVar));
        edges.push(...await getEdgesForRelationType(concept));
        break;

      case ATTRIBUTE_TYPE:
        nodes.push(await getNodeForAttributeType(concept, graqlVar));
        edges.push(...await getEdgesForAttributeType(concept));
        break;

      case ENTITY_INSTANCE:
        nodes.push(await getNodeForEntityInstance(concept, graqlVar, explanation));
        break;

      case RELATION_INSTANCE: {
        nodes.push(await getNodeForRelationInstance(concept, graqlVar, explanation));

        if (shouldLoadRPs) {
          const { edges: autoRpEdges, nodes: autoRpNodes } = await getAutoRPData(concept, shouldLimit, graknTx);
          nodes.push(...autoRpNodes);
          edges.push(...autoRpEdges);
        }
        break;
      }

      case ATTRIBUTE_INSTANCE: {
        nodes.push(await getNodeForAttributeInstance(concept, graqlVar, explanation));
        edges.push(...await getEdgesForAttributeInstance(concept, thingIds));
        break;
      }
      default:
        break;
    }
  } catch (error) {
    console.log(error);
  }

  return { nodes, edges };
}

export async function getNodesAndEdges(answers, shouldLoadRPs, shouldLimit, graknTx) {
  const nodes = [];
  const edges = [];

  try {
    for (let i = 0; i < answers.length; i += 1) {
      const answer = answers[i];
      const answersGroup = Array.from(answers[i].map().entries());
      const thingIds = answersGroup.map(x => x[1].id);

      for (let j = 0; j < answersGroup.length; j += 1) {
        const [graqlVar, concept] = answersGroup[j];

        let shouldSkip = false;
        if (concept.isRole()) shouldSkip = true;
        else if (concept.isType() && await concept.isImplicit()) shouldSkip = true;
        else if (concept.isThing() && (await (await concept.type()).isImplicit())) shouldSkip = true;
        else if (META_LABELS.has(await concept.label())) shouldSkip = true;

        if (!shouldSkip) {
          const data = await constructNodesAndEdges({ concept, graqlVar, explanation: answer.explanation, thingIds }, shouldLoadRPs, shouldLimit, graknTx);
          nodes.push(...data.nodes);
          edges.push(...data.edges);
        }
      }
    }
  } catch (error) {
    console.log(error);
  }
  debugger;
  return { nodes, edges };
}
