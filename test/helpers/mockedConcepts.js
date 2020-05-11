const getConceptFuncs = () => ({
  isType: () => false,
  isEntityType: () => false,
  isAttributeType: () => false,
  isRelationType: () => false,
  isRule: () => false,
  isRole: () => false,
  isThing: () => false,
  isEntity: () => false,
  isAttribute: () => false,
  isRelation: () => false,
});

const getTypeFuncs = ({ isRemote }) => ({
  isType: () => true,
  isImplicit: isRemote ? () => Promise.resolve(false) : () => false,
  isAbstract: () => Promise.resolve(false),
  isThing: () => false,
  attributes: () => Promise.resolve({ collect: () => Promise.resolve([]) }),
  playing: () => Promise.resolve({ collect: () => Promise.resolve([]) }),
});

const getThingFuncs = ({ isRemote }) => ({
  isInferred: isRemote ? () => Promise.resolve(false) : () => false,
  isThing: () => true,
  isType: () => false,
});

/**
 * mock generators for TYPEs
 */

export const getMockedMetaType = ({ isRemote, customFuncs }) => {
  let mocked = {
    ...getConceptFuncs(),
    ...getTypeFuncs({ isRemote }),
    baseType: 'META_TYPE',
    id: 'meta-type-id',
    label: isRemote ? () => Promise.resolve('entity') : () => 'entity',
  };
  if (customFuncs) mocked = { ...mocked, ...customFuncs };
  return mocked;
};

export const getMockedEntityType = ({ isRemote, customFuncs }) => {
  let mocked = {
    ...getConceptFuncs(),
    ...getTypeFuncs({ isRemote }),
    baseType: 'ENTITY_TYPE',
    id: 'entity-type-id',
    isEntityType: () => true,
    label: isRemote ? () => Promise.resolve('entity-type') : () => 'entity-type',
    sup: () => Promise.resolve(getMockedEntityType({
      isRemote: true,
      customFuncs: { id: 'super-entity-type-id' },
    })),
  };
  if (customFuncs) mocked = { ...mocked, ...customFuncs };
  return mocked;
};

export const getMockedAttributeType = ({ isRemote, customFuncs }) => {
  let mocked = {
    ...getConceptFuncs(),
    ...getTypeFuncs({ isRemote }),
    baseType: 'ATTRIBUTE_TYPE',
    id: 'attribute-type-id',
    isAttributeType: () => true,
    label: isRemote ? () => Promise.resolve('attribute-type') : () => 'attribute-type',
    dataType: () => Promise.resolve('String'),
    sup: () => Promise.resolve(getMockedAttributeType({
      isRemote: true,
      customFuncs: { id: 'super-attribute-type-id' },
    })),
  };
  if (customFuncs) mocked = { ...mocked, ...customFuncs };
  return mocked;
};

export const getMockedRelationType = ({ isRemote, customFuncs }) => {
  let mocked = {
    ...getConceptFuncs(),
    ...getTypeFuncs({ isRemote }),
    baseType: 'RELATION_TYPE',
    id: 'relation-type-id',
    isRelationType: () => true,
    label: isRemote ? () => Promise.resolve('relation-type') : () => 'relation-type',
    sup: () => Promise.resolve(getMockedRelationType({
      isRemote: true,
      customFuncs: { id: 'super-relation-type-id' },
    })),
  };
  if (customFuncs) mocked = { ...mocked, ...customFuncs };
  return mocked;
};

/**
 * mock generators for ROLE
 */

export const getMockedRole = ({ isRemote, customFuncs }) => {
  let mocked = {
    ...getConceptFuncs(),
    isRole: () => true,
    label: isRemote ? () => Promise.resolve('role') : () => 'role',
  };
  if (customFuncs) mocked = { ...mocked, ...customFuncs };
  return mocked;
};

/**
 * mock generators for THINGs
 */

export const getMockedEntity = ({ isRemote, customFuncs }) => {
  let mocked = {
    ...getConceptFuncs(),
    ...getThingFuncs({ isRemote }),
    isEntity: () => true,
    type: isRemote ? () => Promise.resolve(getMockedEntityType({ isRemote: false })) : () => getMockedEntityType({ isRemote: false }),
    baseType: 'ENTITY',
    id: 'entity-id',
  };
  if (customFuncs) mocked = { ...mocked, ...customFuncs };
  return mocked;
};


export const getMockedAttribute = ({ isRemote, customFuncs }) => {
  let mocked = {
    ...getConceptFuncs(),
    ...getThingFuncs({ isRemote }),
    isAttribute: () => true,
    value: isRemote ? () => Promise.resolve('attribute-value') : () => 'attribute-value',
    type: isRemote ? () => Promise.resolve(getMockedAttributeType({ isRemote: false })) : () => getMockedAttributeType({ isRemote: false }),
    baseType: 'ATTRIBUTE',
    id: 'attribute-id',
  };
  if (customFuncs) mocked = { ...mocked, ...customFuncs };
  return mocked;
};

export const getMockedRelation = ({ isRemote, customFuncs }) => {
  let mocked = {
    ...getConceptFuncs(),
    ...getThingFuncs({ isRemote }),
    isRelation: () => true,
    type: isRemote ? () => Promise.resolve(getMockedRelationType({ isRemote: false })) : () => getMockedRelationType({ isRemote: false }),
    baseType: 'RELATION',
    id: 'relation-id',
  };
  if (customFuncs) mocked = { ...mocked, ...customFuncs };
  return mocked;
};

/**
 * mock generator: Answer
 */

export const getMockedConceptMap = (concepts, explanationAnswers) => {
  const map = new Map();
  concepts.forEach((concept, index) => { map.set(index, concept); });
  const mock = {
    map: () => map,
    hasExplanation: () => !!explanationAnswers,
    explanation: () => ({ getAnswers: () => Promise.resolve(explanationAnswers || []) }),
    queryPattern: () => '',
  };
  return mock;
};

/**
 * mock generator for Session and Transaction
*/

export const getMockedTransaction = (answers, customFuncs) => {
  const mock = {
    query: () => Promise.resolve(answers),
    commit: () => Promise.resolve(),
    close: () => Promise.resolve(),
    isOpen: () => Promise.resolve(true),
    ...customFuncs,
  };
  return mock;
};

// const concept = {
//   props: {
//     id: 'id',
//   },
//   funcs: {
//     delete: () => Promise.resolve(),
//     isDeleted: () => Promise.resolve(true),
//   },
// };

// const conceptType = {
//   funcs: {

//   },
// };

// const getMockedGraknTx = (answers, extraProps = {}) => ({
//   query: () => Promise.resolve({
//     collect: () => Promise.resolve(answers),
//     collectConcepts: () => Promise.resolve(answers.map((answer, index) => answer.map().get(index))),
//   }),
//   ...extraProps,
// });

// const getMockedExplanation = answers => Promise.resolve({ getAnswers: () => answers });

export const getMockedAnswer = (concepts, explanation) => {
  const answer = {};

  const map = new Map();
  concepts.forEach((concept, index) => { map.set(index, concept); });
  answer.map = () => map;
  answer.explanation = () => explanation;
  answer.queryPattern = () => '';

  return answer;
};

// const mockedMetaType = {
//   isRole: () => false,
//   isType: () => true,
//   isThing: () => false,
//   isImplicit: () => Promise.resolve(false),
//   label: () => Promise.resolve('entity'),
// };

// const mockedRole = {
//   label: () => Promise.resolve('some-role'),
//   isImplicit: () => Promise.resolve(false),
// };

// const mockedEntityType = {
//   id: 'ent-type',
//   baseType: 'ENTITY_TYPE',
//   isRole: () => false,
//   isType: () => true,
//   isThing: () => false,
//   label: () => Promise.resolve('some-entity-type'),
//   attributes: () => Promise.resolve({ collect: () => Promise.resolve([]) }),
//   isImplicit: () => Promise.resolve(false),
//   sup: () => Promise.resolve({ label: () => Promise.resolve('entity') }),
//   playing: () => Promise.resolve({ collect: () => Promise.resolve([mockedRole]) }),
// };

// const mocked = { ...mockedEntityType };
// mocked.instances = () => Promise;

// const mockedRelationType = {
//   id: 'rel-type',
//   baseType: 'RELATION_TYPE',
//   isRole: () => false,
//   isType: () => true,
//   isRelationType: () => true,
//   isThing: () => false,
//   label: () => Promise.resolve('some-relation-type'),
//   attributes: () => Promise.resolve({ collect: () => Promise.resolve([]) }),
//   isImplicit: () => Promise.resolve(false),
//   sup: () => Promise.resolve({ label: () => Promise.resolve('relation') }),
//   playing: () => Promise.resolve({ collect: () => Promise.resolve([]) }),
// };

// const mockedAttributeType = {
//   id: 'attr-type',
//   baseType: 'ATTRIBUTE_TYPE',
//   isRole: () => false,
//   isType: () => true,
//   isThing: () => false,
//   label: () => Promise.resolve('some-attribute-type'),
//   attributes: () => Promise.resolve({ collect: () => Promise.resolve([]) }),
//   isImplicit: () => Promise.resolve(false),
//   sup: () => Promise.resolve({ label: () => Promise.resolve('attribute') }),
//   playing: () => Promise.resolve({ collect: () => Promise.resolve([]) }),
//   dataType: () => Promise.resolve('String'),
// };

// const mockedEntity = getMockedEntity({
//   isRemote: false,
//   customFuncs: {
//     attributes: () => ({
//       collect: () => [getMockedEntity({ isRemote: true })],
//     }),
//   },
// });
// debugger;

// const mockedRelationInstance = {
//   id: 'rel-instance',
//   baseType: 'RELATION',
//   isRole: () => false,
//   isType: () => false,
//   isThing: () => true,
//   isAttribute: () => false,
//   isEntity: () => false,
//   isRelation: () => true,
//   isInferred: () => Promise.resolve(false),
//   type: () => Promise.resolve(mockedRelationType),
//   attributes: () => Promise.resolve({ collect: () => Promise.resolve([]) }),
// };

// const mockedAttributeInstance = {
//   id: 'attr-instance',
//   baseType: 'ATTRIBUTE',
//   isRole: () => false,
//   isType: () => false,
//   isThing: () => true,
//   isAttribute: () => true,
//   isInferred: () => Promise.resolve(false),
//   type: () => Promise.resolve(mockedAttributeType),
//   value: () => Promise.resolve('some value'),
// };

// export {
//   getMockedGraknTx,
//   getMockedExplanation,
//   getMockedAnswer,
//   mockedMetaType,
//   mockedRole,
//   mockedEntityType,
//   mockedRelationType,
//   mockedAttributeType,
//   mockedEntityInstance,
//   mockedRelationInstance,
//   mockedAttributeInstance,
// };
