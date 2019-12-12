const getMockedGraknTx = (answers, extraProps = {}) => ({
  query: () => Promise.resolve({
    collect: () => Promise.resolve(answers),
    collectConcepts: () => Promise.resolve(answers.map((answer, index) => answer.map().get(index))),
  }),
  ...extraProps,
});

const getMockedExplanation = answers => Promise.resolve({ getAnswers: () => answers });

const getMockedAnswer = (concepts, explanation) => {
  const answer = {};

  const map = new Map();
  concepts.forEach((concept, index) => { map.set(index, concept); });
  answer.map = () => map;
  answer.explanation = () => explanation;
  answer.queryPattern = () => '';

  return answer;
};

const mockedMetaType = {
  isRole: () => false,
  isType: () => true,
  isThing: () => false,
  isImplicit: () => Promise.resolve(false),
  label: () => Promise.resolve('entity'),
};

const mockedRole = {
  label: () => Promise.resolve('some-role'),
};

const mockedEntityType = {
  id: 'ent-type',
  baseType: 'ENTITY_TYPE',
  isRole: () => false,
  isType: () => true,
  isThing: () => false,
  label: () => Promise.resolve('some-entity-type'),
  attributes: () => Promise.resolve({ collect: () => Promise.resolve([]) }),
  isImplicit: () => Promise.resolve(false),
  sup: () => Promise.resolve({ label: () => Promise.resolve('entity') }),
  playing: () => Promise.resolve({ collect: () => Promise.resolve([]) }),
};

const mockedRelationType = {
  id: 'rel-type',
  baseType: 'RELATION_TYPE',
  isRole: () => false,
  isType: () => true,
  isRelationType: () => true,
  isThing: () => false,
  label: () => Promise.resolve('some-relation-type'),
  attributes: () => Promise.resolve({ collect: () => Promise.resolve([]) }),
  isImplicit: () => Promise.resolve(false),
  sup: () => Promise.resolve({ label: () => Promise.resolve('relation') }),
  playing: () => Promise.resolve({ collect: () => Promise.resolve([]) }),
};

const mockedAttributeType = {
  id: 'attr-type',
  baseType: 'ATTRIBUTE_TYPE',
  isRole: () => false,
  isType: () => true,
  isThing: () => false,
  label: () => Promise.resolve('some-attribute-type'),
  attributes: () => Promise.resolve({ collect: () => Promise.resolve([]) }),
  isImplicit: () => Promise.resolve(false),
  sup: () => Promise.resolve({ label: () => Promise.resolve('attribute') }),
  playing: () => Promise.resolve({ collect: () => Promise.resolve([]) }),
  dataType: () => Promise.resolve('String'),
};

const mockedEntityInstance = {
  id: 'ent-instance',
  baseType: 'ENTITY',
  isRole: () => false,
  isType: () => false,
  isThing: () => true,
  isEntity: () => true,
  isAttribute: () => false,
  isInferred: () => Promise.resolve(false),
  type: () => Promise.resolve(mockedEntityType),
  attributes: () => Promise.resolve({ collect: () => Promise.resolve([]) }),
};

const mockedRelationInstance = {
  id: 'rel-instance',
  baseType: 'RELATION',
  isRole: () => false,
  isType: () => false,
  isThing: () => true,
  isAttribute: () => false,
  isEntity: () => false,
  isRelation: () => true,
  isInferred: () => Promise.resolve(false),
  type: () => Promise.resolve(mockedRelationType),
  attributes: () => Promise.resolve({ collect: () => Promise.resolve([]) }),
};

const mockedAttributeInstance = {
  id: 'attr-instance',
  baseType: 'ATTRIBUTE',
  isRole: () => false,
  isType: () => false,
  isThing: () => true,
  isAttribute: () => true,
  isInferred: () => Promise.resolve(false),
  type: () => Promise.resolve(mockedAttributeType),
  value: () => Promise.resolve('some value'),
};

export {
  getMockedGraknTx,
  getMockedExplanation,
  getMockedAnswer,
  mockedMetaType,
  mockedRole,
  mockedEntityType,
  mockedRelationType,
  mockedAttributeType,
  mockedEntityInstance,
  mockedRelationInstance,
  mockedAttributeInstance,
};
