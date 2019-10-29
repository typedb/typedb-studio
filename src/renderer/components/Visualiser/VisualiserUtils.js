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

  // If there is no `get` the user mistyped the query
  if (getRegex.test(query)) {
    const limitRegex = /(.*\s.*;\s*)(limit\b.*?;).*/;
    const offsetRegex = /.*;\s*(offset\b.*?;).*/;
    const deleteRegex = /^(.*;)\s*(delete\b.*;)$/;

    if (!(offsetRegex.test(query)) && !(deleteRegex.test(query)) && limitRegex.test(query)) { // if query does not contain offset and delete but does contain limit
      limitedQuery = `${query.match(limitRegex)[1]}offset 0; ${query.match(limitRegex)[2]}`;
    } else if (!(offsetRegex.test(query)) && !(deleteRegex.test(query)) && !limitRegex.test(query)) { // if query does not contain offset, delete and limit
      limitedQuery = `${limitedQuery} offset 0;`;
    }
    if (!(limitRegex.test(query)) && !(deleteRegex.test(query))) { limitedQuery = `${limitedQuery} limit ${QuerySettings.getQueryLimit()};`; }
  }
  return limitedQuery;
}


export function buildExplanationQuery(answer, queryPattern) {
  let query = 'match ';
  let attributeQuery = null;
  Array.from(answer.map().entries()).forEach(([graqlVar, concept]) => {
    if (concept.isAttribute()) {
      const attributeRegex = queryPattern.match(/(?:has )(\S+)/);
      if (attributeRegex) { // if attribute is only an attribute of a concept
        attributeQuery = `has ${attributeRegex[1]} $${graqlVar};`;
      } else { // attribute plays a role player in a relation
        let attributeType = queryPattern.match(/\(([^)]+)\)/)[0].split(',').filter(y => y.includes(graqlVar));
        attributeType = attributeType[0].slice(1, attributeType[0].indexOf(':'));
        attributeQuery = `has ${attributeType} $${graqlVar};`;
      }
    } else if (concept.isEntity()) {
      query += `$${graqlVar} id ${concept.id}; `;
    } else if (concept.isRelation()) {
      query += `$${graqlVar} id ${concept.id}; `;
    }
  });
  return { query, attributeQuery };
}

export function computeAttributes(nodes, graknTx) {
  return Promise.all(nodes.map(async (node) => {
    const attributes = (await (await graknTx.query(`match $x id ${node.id}; $x has attribute $y; get $y;`)).collectConcepts());
    node.attributes = await Promise.all(attributes.map(async (concept) => {
      const attribute = {};
      if (concept.isType()) {
        await concept.label().then((label) => { attribute.type = label; });
      } else {
        await Promise.all([
          concept.type().then(type => type.label()).then((label) => { attribute.type = label; }),
          concept.value().then((value) => { attribute.value = value; }),
        ]);
      }
      return attribute;
    }));
    return node;
  }));
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
 * Given a Grakn Answer, this function returns the query that needs to be run in order
 * to obtain a visual explanation of the inferred concept
 * @param {Object} answer Grakn Answer which contains the explanation that needs to be loaded
 */
export function mapAnswerToExplanationQuery(answer) {
  const queryPattern = answer.explanation().queryPattern();
  let query = buildExplanationQuery(answer, queryPattern).query;
  if (queryPattern.includes('has')) {
    query = query.slice(0, -2);
    query += `, ${buildExplanationQuery(answer, queryPattern).attributeQuery} get;`;
  } else {
    query += `$r ${queryPattern.slice(1, -1).match(/\((.*?;)/)[0]} get $r; offset 0; limit 1;`;
  }
  return query;
}

/**
 * Checks if a ConceptMap inside the provided Answer contains at least one implicit concept
 * @param {Object} answer ConceptMap Answer to be inspected
 * @return {Boolean}
 */
async function answerContainsImplicitType(answer) {
  const concepts = Array.from(answer.map().values());
  return Promise.all(concepts.map(async concept => ((concept.isThing()) ? (await concept.type()).isImplicit() : concept.isImplicit())))
    .then(a => a.includes(true));
}

/**
 * Filters out Answers that contained inferred concepts in their ConceptMap
 * @param {Object[]} answers array of ConceptMap Answers to be inspected
 * @return {Object[]} filtered array of Answers
 */
export async function filterMaps(answers) { // Filter out ConceptMaps that contain implicit relations
  return Promise.all(answers.map(async x => ((await answerContainsImplicitType(x)) ? null : x)))
    .then(maps => maps.filter(map => map));
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
  const filteredResult = await filterMaps(resultAnswers);
  if (resultAnswers.length !== filteredResult.length) {
    const offsetDiff = resultAnswers.length - filteredResult.length;
    node.offset += QuerySettings.getNeighboursLimit();
    return filteredResult.concat(await getFilteredNeighbourAnswers(node, graknTx, offsetDiff));
  }
  return resultAnswers;
}
