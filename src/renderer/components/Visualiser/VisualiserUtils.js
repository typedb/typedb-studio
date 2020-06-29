import QuerySettings from './RightBar/SettingsTab/QuerySettings';
const LETTER_G_KEYCODE = 71;

export function limitQuery(query) {
  const getRegex = /^((.|\n)*;)\s*(get;|get\s*.\w*;)/;
  let limitedQuery = query;

  if (getRegex.test(query)) {
    const limitRegex = /^((.|\n)*;.*)(limit\b.*?;).*/;
    const offsetRegex = /.*;\s*(offset\b.*?;).*/;

    if (!(offsetRegex.test(query)) && !limitRegex.test(query)) {
      limitedQuery = `${query} offset 0; limit ${QuerySettings.getQueryLimit()};`;
    } else if (!(offsetRegex.test(query))) {
      limitedQuery = `${query.match(limitRegex)[1]}offset 0; ${query.match(limitRegex)[3]}`;
    } else if (!(limitRegex.test(query))) {
      limitedQuery = `${query} limit ${QuerySettings.getQueryLimit()};`;
    }
  }

  return limitedQuery;
}

/**
 * for each node, retrieves the label and value for all the node's attributes and adds them to
 * `attributes` property of the node. the `attributes` property of the node is read to list the
 * attributes' label and value in the sidebar for the selected node.
 * @param {*} nodes nodes that are currently visualised in the graph
 * @param {*} graknTx
 * @return array of node objects, where each object includes the `attributes` property
 */
export async function computeAttributes(nodes, graknTx) {
  const concepts = await Promise.all(nodes.map(node => graknTx.getConcept(node.id)));
  const attrIters = await Promise.all(concepts.map(concept => concept.attributes()));
  const attrGroups = await Promise.all(attrIters.map(iter => iter.collect()));

  return Promise.all(attrGroups.map(async (attrGroup, i) => {
    nodes[i].attributes = await Promise.all(attrGroup.map(attr => new Promise((resolve) => {
      const attribute = {};
      if (attr.isType()) {
        attr.label().then((label) => {
          attribute.type = label;
          resolve(attribute);
        });
      } else {
        attr.type().then(type => type.label()).then((label) => {
          attribute.type = label;
          attr.value().then((value) => {
            attribute.value = value;
            resolve(attribute);
          });
        });
      }
    })));
    return nodes[i];
  }));
}

export async function loadMetaTypeInstances(graknTx) {
  // Fetch types
  const rels = await (await graknTx.query('match $x sub relation; get;')).collectConcepts();
  const entities = await (await graknTx.query('match $x sub entity; get;')).collectConcepts();
  const attributes = await (await graknTx.query('match $x sub attribute; get;')).collectConcepts();
  const roles = await (await graknTx.query('match $x sub role; get;')).collectConcepts();

  // Get types labels
  const metaTypeInstances = {};
  metaTypeInstances.entities = await Promise.all(entities.map(type => type.label()))
    .then(labels => labels.filter(l => l !== 'entity')
      .concat()
      .sort());
  metaTypeInstances.relations = await Promise.all(rels.map(type => type.label()))
    .then(labels => labels.filter(l => l && l !== 'relation')
      .concat()
      .sort());
  metaTypeInstances.attributes = await Promise.all(attributes.map(type => type.label()))
    .then(labels => labels.filter(l => l !== 'attribute')
      .concat()
      .sort());
  metaTypeInstances.roles = await Promise.all(roles.map(type => type.label()))
    .then(labels => labels.filter(l => l && l !== 'role')
      .concat()
      .sort());
  return metaTypeInstances;
}

export function validateQuery(query) {
  const trimmedQuery = query.trim();
  if (
    /^insert/.test(trimmedQuery)
    || /^((.|\n)*)(insert\b.*;)$/.test(trimmedQuery)
    || /^((.|\n)*)(delete\b.*;)$/.test(trimmedQuery)
    || /^((.|\n)*)(count\b.*;)$/.test(trimmedQuery)
    || /^((.|\n)*)(sum\b.*;)$/.test(trimmedQuery)
    || /^((.|\n)*)(max\b.*;)$/.test(trimmedQuery)
    || /^((.|\n)*)(min\b.*;)$/.test(trimmedQuery)
    || /^((.|\n)*)(mean\b.*;)$/.test(trimmedQuery)
    || /^((.|\n)*)(median\b.*;)$/.test(trimmedQuery)
    || /^((.|\n)*)(group\b.*;)$/.test(trimmedQuery)
    || (/^compute/.test(trimmedQuery) && !(/^compute path/.test(trimmedQuery)))
  ) {
    throw new Error('At the moment, only `match get` and `compute path` queries are supported.');
  }
}

export function addResetGraphListener(dispatch, action) {
  window.addEventListener('keydown', (e) => {
  // Reset canvas when metaKey(CtrlOrCmd) + G are pressed
    if ((e.keyCode === LETTER_G_KEYCODE) && e.metaKey) { dispatch(action); }
  });
}

/**
 * produces an array of ConceptMap answers containing the neighbours of the given targetNode
 * identified neighbours:
 * 1) differ based on the baseType of targetNode
 * 2) are not already visualised
 * 3) are no more than the user-specified NeighboursLimit
 * @param {Object} targetNode the node for which we want to load the neighbours
 * @param {Array} currentEdges the edges that are currently visualised
 * @param {Object} graknTx Grakn transaction used to execute query
 */
export async function getNeighbourAnswers(targetNode, currentEdges, graknTx) {
  const neighbourAnswers = [];
  const neighboursLimit = QuerySettings.getNeighboursLimit();

  switch (targetNode.baseType) {
    case 'ENTITY_TYPE':
    case 'ATTRIBUTE_TYPE':
    case 'RELATION_TYPE': {
      const targetTypeId = targetNode.id;
      const query = `match $target-type id ${targetTypeId}; $neighbour-instance isa $target-type; get $neighbour-instance;`;
      const iter = await graknTx.query(query);

      let answer = await iter.next();
      while (answer && neighbourAnswers.length !== neighboursLimit) {
        const neighbourInstanceId = answer.map().get('neighbour-instance').id;
        const edgeId = `${targetTypeId}-${neighbourInstanceId}-isa`;
        if (!currentEdges.some(currEdge => currEdge.id === edgeId)) neighbourAnswers.push(answer);
        // eslint-disable-next-line no-await-in-loop
        answer = await iter.next();
      }
      break;
    }
    case 'ENTITY': {
      const targetEntId = targetNode.id;
      const query = `match $target-entity id ${targetEntId}; $neighbour-relation ($target-entity-role: $target-entity); get;`;
      const iter = await graknTx.query(query);
      let answer = await iter.next();
      while (answer && neighbourAnswers.length !== neighboursLimit) {
        const targetEntRoleLabel = answer.map().get('target-entity-role').label();
        if (targetEntRoleLabel !== 'role') {
          const neighbourRelId = answer.map().get('neighbour-relation').id;
          const edgeId = `${neighbourRelId}-${targetEntId}-${targetEntRoleLabel}`;
          if (!currentEdges.some(currEdge => currEdge.id === edgeId)) neighbourAnswers.push(answer);
        }
        // eslint-disable-next-line no-await-in-loop
        answer = await iter.next();
      }
      break;
    }
    case 'ATTRIBUTE': {
      const targetAttrId = targetNode.id;
      const query = `match $neighbour-owner has attribute $target-attribute; $target-attribute id ${targetAttrId}; get $neighbour-owner;`;
      const iter = await graknTx.query(query);
      let answer = await iter.next();
      while (answer && neighbourAnswers.length !== neighboursLimit) {
        const neighbourOwnerId = answer.map().get('neighbour-owner').id;
        const edgeId = `${neighbourOwnerId}-${targetAttrId}-has`;
        if (!currentEdges.some(currEdge => currEdge.id === edgeId)) neighbourAnswers.push(answer);
        // eslint-disable-next-line no-await-in-loop
        answer = await iter.next();
      }
      break;
    }
    case 'RELATION': {
      const targetRelId = targetNode.id;
      const query = `match $target-relation id ${targetRelId}; $target-relation ($neighbour-role: $neighbour-player); get $neighbour-player, $neighbour-role;`;
      const iter = await graknTx.query(query);
      let answer = await iter.next();
      while (answer && neighbourAnswers.length !== neighboursLimit) {
        const neighbourRoleLabel = answer.map().get('neighbour-role').label();
        if (neighbourRoleLabel !== 'role') {
          const neighbourRoleId = answer.map().get('neighbour-player').id;
          const edgeId = `${targetRelId}-${neighbourRoleId}-${neighbourRoleLabel}`;
          if (!currentEdges.some(currEdge => currEdge.id === edgeId)) neighbourAnswers.push(answer);
        }
        // eslint-disable-next-line no-await-in-loop
        answer = await iter.next();
      }
      break;
    }
    default:
      throw new Error(`Unrecognised baseType of thing: ${targetNode.baseType}`);
  }

  return neighbourAnswers;
}
