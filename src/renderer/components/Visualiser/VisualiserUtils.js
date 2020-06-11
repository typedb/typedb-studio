import QuerySettings from './RightBar/SettingsTab/QuerySettings';
const LETTER_G_KEYCODE = 71;


function getNeighboursQuery(node, neighboursLimit) {
  switch (node.baseType) {
    case 'ENTITY_TYPE':
    case 'ATTRIBUTE_TYPE':
    case 'RELATION_TYPE':
      return `match $x id ${node.id}; $y isa $x; get $y; offset ${node.offset}; limit ${neighboursLimit};`;
    case 'ENTITY':
      return `match $x id ${node.id}; $r ($x, $y); get $r, $y; offset ${node.offset}; limit ${neighboursLimit};`;
    case 'ATTRIBUTE':
      return `match $x has attribute $y; $y id ${node.id}; get $x; offset ${node.offset}; limit ${neighboursLimit};`;
    case 'RELATION':
      return `match $r id ${node.id}; $r ($x, $y); get $x; offset ${node.offset}; limit ${node.offset + neighboursLimit};`;
    default:
      throw new Error(`Unrecognised baseType of thing: ${node.baseType}`);
  }
}

export function limitQuery(query) {
  const getRegex = /^((.|\n)*;)\s*(get;|get\s*.\w*;)/;
  let limitedQuery = query;

  if (getRegex.test(query)) {
    const limitRegex = /^((.|\n)*;.*)(limit\b.*?;).*/;
    const offsetRegex = /.*;\s*(offset\b.*?;).*/;

    if (!(offsetRegex.test(query)) && !limitRegex.test(query)) {
      limitedQuery = `${query} offset 0; limit ${QuerySettings.getQueryLimit()};`;
    } else if (!(offsetRegex.test(query))) {
      limitedQuery = `${query.match(limitRegex)[1]} offset 0; ${query.match(limitRegex)[3]}`;
    } else if (!(limitRegex.test(query))) {
      limitedQuery = `${query} limit ${QuerySettings.getQueryLimit()};`;
    }
  }

  limitedQuery = limitedQuery.replace(/\s+/g, ' ').trim();
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
    || /^(.*;)\s*(insert\b.*;)$/.test(trimmedQuery)
    || /^(.*;)\s*(delete\b.*;)$/.test(trimmedQuery)
    || /^(.*;)\s*(count\b.*;)$/.test(trimmedQuery)
    || /^(.*;)\s*(sum\b.*;)$/.test(trimmedQuery)
    || /^(.*;)\s*(max\b.*;)$/.test(trimmedQuery)
    || /^(.*;)\s*(min\b.*;)$/.test(trimmedQuery)
    || /^(.*;)\s*(mean\b.*;)$/.test(trimmedQuery)
    || /^(.*;)\s*(median\b.*;)$/.test(trimmedQuery)
    || /^(.*;)\s*(group\b.*;)$/.test(trimmedQuery)
    || (/^compute/.test(trimmedQuery) && !(/^compute path/.test(trimmedQuery)))
  ) {
    throw new Error('Only match get and compute path queries are supported by workbase for now.');
  }
}

export function addResetGraphListener(dispatch, action) {
  window.addEventListener('keydown', (e) => {
  // Reset canvas when metaKey(CtrlOrCmd) + G are pressed
    if ((e.keyCode === LETTER_G_KEYCODE) && e.metaKey) { dispatch(action); }
  });
}

/**
 * Executes query to load neighbours of given node and filters our all the answers that contain implicit concepts, given that we
 * don't want to show implicit concepts (relations to attributes) to the user, for now.
 * @param {Object} node VisJs node of which we want to load the neighbours
 * @param {Object} graknTx Grakn transaction used to execute query
 * @param {Number} limit Limit of neighbours to load
 */
export async function getFilteredNeighbourAnswers(node, graknTx, limit) {
  const query = getNeighboursQuery(node, limit);
  const resultAnswers = await (await graknTx.query(query)).collect();
  return resultAnswers;
}
