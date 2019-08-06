/* eslint-disable no-await-in-loop */
import { buildLabel } from '../Visualiser/VisualiserGraphBuilder';

export const META_CONCEPTS = new Set(['entity', 'relation', 'attribute', 'role']);

export async function ownerHasEdges(nodes) {
  const edges = [];

  await Promise.all(nodes.map(async (node) => {
    const sup = await node.sup();
    if (sup) {
      const supLabel = await sup.label();
      if (META_CONCEPTS.has(supLabel)) {
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
      if (!META_CONCEPTS.has(supLabel)) {
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

export async function getData(answers, shouldLoadRPs, rpLimit, graknTx) {
  const nodes = [];
  const edges = [];

  try {
    for (let i = 0; i < answers.length; i += 1) {
      const answer = answers[i];
      const answersGroup = Array.from(answers[i].map().entries());
      const thingIds = answersGroup.map(x => x[1].id);

      for (let j = 0; j < answersGroup.length; j += 1) {
        const [graqlVar, concept] = answersGroup[j];

        concept.var = graqlVar;
        concept.explanation = answer.explanation();
        concept.attrOffset = 0;
        concept.offset = 0;


        const { ENTITY_TYPE, RELATION_TYPE, ATTRIBUTE_TYPE, ENTITY_INSTANCE, RELATION_INSTANCE, ATTRIBUTE_INSTANCE } = baseTypes;

        switch (concept.baseType) {
          case ENTITY_TYPE:
          case RELATION_TYPE:
          case ATTRIBUTE_TYPE:
            concept.label = await concept.label();
            break;
          case ENTITY_INSTANCE:
            concept.type = await getTypeOfInstance(concept);
            concept.label = await buildLabel(concept);
            concept.isInferred = await concept.isInferred();
            break;
          case RELATION_INSTANCE:
            concept.type = await getTypeOfInstance(concept);
            concept.isInferred = await concept.isInferred();
            if (shouldLoadRPs) {
              const queryToGetRPs = `match $r id ${concept.id}; $r($rl: $rp); get $rp, $rl; offset 0; limit ${rpLimit + 1};`;
              const rpsResult = await (await graknTx.query(queryToGetRPs)).collect();

              let isRoleTypePresent = false;

              await Promise.all(rpsResult.map(result => Array.from(result.map().values())).flatMap(x => x).map(async (y) => {
                if (y.baseType === baseTypes.ROLE && await y.label() === 'role') {
                  isRoleTypePresent = true;
                }
              }));

              if (!isRoleTypePresent) rpsResult.pop();

              for (let k = 0; k < rpsResult.length; k += 1) {
                const rolesAndRps = Array.from(rpsResult[k].map().values());
                const role = rolesAndRps.filter(x => x.baseType === baseTypes.ROLE)[0];
                const edgeLabel = await role.label();
                const roleplayers = rolesAndRps.filter(x => x.baseType !== baseTypes.ROLE);
                // TODO: role's baseType should be META_TYPE and so excluded that way, whereas it is currenlty ROLE
                if (edgeLabel !== 'role') {
                  roleplayers.forEach((rp) => {
                    const relEdge = { from: concept.id, to: rp.id, label: edgeLabel };
                    edges.push(relEdge);
                  });
                }
              }

              const rpsData = await getData(rpsResult, false);
              nodes.push(...rpsData.nodes);

              concept.offset = rpLimit;
            }
            break;
          case ATTRIBUTE_INSTANCE: {
            concept.type = await getTypeOfInstance(concept);
            concept.value = await concept.value();
            concept.label = await buildLabel(concept);
            concept.isInferred = await concept.isInferred();

            const owners = (await (await concept.owners()).collect()).filter(owner => thingIds.includes(owner.id));
            const attrEdges = owners.map(owner => ({ from: owner.id, to: concept.id, label: 'has' }));
            edges.push(...attrEdges);

            break;
          }
          default:
            break;
        }

        if (concept.baseType !== baseTypes.TYPE && concept.baseType !== baseTypes.ROLE) {
          nodes.push(concept);
        }
      }
    }
  } catch (error) {
    console.log(error);
  }
  return { nodes, edges };
}
