export const META_CONCEPTS = new Set(['entity', 'relation', 'attribute', 'role']);

export const edgeDefaultOptions = {
  label: { show: false },
  arrow: { show: false, type: 'arrow' },
};

export function constructEdge(base, options) {
  const edge = {
    ...base,
    options,
    arrows: { to: { enabled: !options.hideArrow } },
    label: options.hideLabel ? '' : base.label,
    hiddenLabel: options.hideLabel ? base.label : '',
  };
  return edge;
}

export async function ownerHasEdges(nodes, edgeOptions) {
  const edges = [];

  await Promise.all(nodes.map(async (node) => {
    const sup = await node.sup();
    if (sup) {
      const type = await sup.label();
      let attributes;
      if (META_CONCEPTS.has(type)) {
        attributes = await node.attributes();
        attributes = await attributes.collect();
      } else { // if node has a super type which is not a META_CONCEPT construct edges to attributes expect those which are inherited from its super type
        const supAttributeIds = (await (await sup.attributes()).collect()).map(x => x.id);
        attributes = (await (await node.attributes()).collect()).filter(attr => !supAttributeIds.includes(attr.id));
      }
      attributes.forEach(attr => edges.push(constructEdge({ from: node.id, to: attr.id, label: 'has' }, edgeOptions)));
    }
  }));
  return edges;
}

export async function relationTypesOutboundEdges(nodes, edgeOptions) {
  const edges = [];
  const promises = nodes.filter(x => x.isRelationType())
    .map(async rel =>
      Promise.all(((await (await rel.roles()).collect())).map(async (role) => {
        const types = await (await role.players()).collect();
        const label = await role.label();
        return types.forEach((type) => {
          edges.push(constructEdge(
            { from: rel.id, to: type.id, label },
            edgeOptions,
          ));
        });
      })),
    );
  await Promise.all(promises);
  return edges;
}


export async function computeSubConcepts(nodes, edgeOptions) {
  const edges = [];
  const subConcepts = [];
  await Promise.all(nodes.map(async (concept) => {
    const sup = await concept.sup();
    if (sup) {
      const supLabel = await sup.label();
      if (!META_CONCEPTS.has(supLabel)) {
        const edge = constructEdge({ from: concept.id, to: sup.id, label: 'sub' }, edgeOptions);
        edges.push(edge);
        subConcepts.push(concept);
      }
    }
  }));
  return { nodes: subConcepts, edges };
}
