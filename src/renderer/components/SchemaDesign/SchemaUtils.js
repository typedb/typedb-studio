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
  const entities = await (await graknTx.query('match $x sub entity; get;')).collectConcepts();
  const rels = await (await graknTx.query('match $x sub relation; get;')).collectConcepts();
  const attributes = await (await graknTx.query('match $x sub attribute; get;')).collectConcepts();
  const roles = await (await graknTx.query('match $x sub role; get;')).collectConcepts();

  // Get types labels
  const metaTypeInstances = {};
  metaTypeInstances.entities = await Promise.all(entities.map(type => type.label()))
    .then(labels => labels.filter(l => l !== 'entity')
      .concat()
      .sort());
  metaTypeInstances.relations = await Promise.all(rels.map(async type => ((!await type.isImplicit()) ? type.label() : null)))
    .then(labels => labels.filter(l => l && l !== 'relation')
      .concat()
      .sort());
  metaTypeInstances.attributes = await Promise.all(attributes.map(type => type.label()))
    .then(labels => labels.filter(l => l !== 'attribute')
      .concat()
      .sort());
  metaTypeInstances.roles = await Promise.all(roles.map(async type => ((!await type.isImplicit()) ? type.label() : null)))
    .then(labels => labels.filter(l => l && l !== 'role')
      .concat()
      .sort());
  return metaTypeInstances;
}

// attach attribute labels and data types to each node
export async function computeAttributes(nodes) {
  return Promise.all(nodes.map(async (node) => {
    const attributes = await (await node.attributes()).collect();
    node.attributes = await Promise.all(attributes.map(async concept => ({ type: await concept.label(), dataType: await concept.dataType() })));
    return node;
  }));
}

// attach role labels to each node
export async function computeRoles(nodes) {
  return Promise.all(nodes.map(async (node) => {
    const roles = await (await node.playing()).collect();
    node.roles = await Promise.all(roles.map(async concept => ((await concept.isImplicit()) ? null : concept.label()))).then(nodes => nodes.filter(x => x));
    return node;
  }));
}

