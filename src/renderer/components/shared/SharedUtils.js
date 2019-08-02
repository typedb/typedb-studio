export const META_CONCEPTS = new Set(['entity', 'relation', 'attribute', 'role']);

export const interfaceTypes = {
  SCHEMA_DESIGNER: 'SCHEMA_DESIGNER',
  VISUALISER: 'VISUALISER',
};

export function getEdgeDefaultOptions() {
  return {
    label: { show: false },
    arrow: { show: false, type: 'arrow' },
  };
}

export async function ownerHasEdges(nodes) {
  const edges = [];

  await Promise.all(nodes.map(async (node) => {
    const sup = await node.sup();
    if (sup) {
      const options = { ...getEdgeDefaultOptions(), interfaceType: interfaceTypes.SCHEMA_DESIGNER, label: { show: true }, arrow: { show: true } };
      const supLabel = await sup.label();
      if (META_CONCEPTS.has(supLabel)) {
        let attributes = await node.attributes();
        attributes = await attributes.collect();
        attributes.map(attr => edges.push({ from: node.id, to: attr.id, label: 'has', options }));
      } else { // if node has a super type which is not a META_CONCEPT construct edges to attributes expect those which are inherited from its super type
        const supAttributeIds = (await (await sup.attributes()).collect()).map(x => x.id);
        const attributes = (await (await node.attributes()).collect()).filter(attr => !supAttributeIds.includes(attr.id));
        attributes.map(attr => edges.push({ from: node.id, to: attr.id, label: 'has', options }));
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
        const options = { ...getEdgeDefaultOptions(), interfaceType: interfaceTypes.SCHEMA_DESIGNER, label: { show: true }, arrow: { show: true } };
        return types.forEach((type) => { edges.push({ from: rel.id, to: type.id, label, options }); });
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
        const options = { ...getEdgeDefaultOptions(), interfaceType: interfaceTypes.SCHEMA_DESIGNER, label: { show: true }, arrow: { show: true } };
        const edge = { from: concept.id, to: sup.id, label: 'sub', arrows: { to: { enabled: true } }, options };
        edges.push(edge);
        subConcepts.push(concept);
      }
    }
  }));
  return { nodes: subConcepts, edges };
}
