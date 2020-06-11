const methods = {
  concept: {
    static: {
      baseType: 'META_TYPE',
      id: 'META_TYPE',
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
      isSchemaConcept: () => false,
    },
    local: {
      label: () => 'thing',
      isImplicit: () => false,
    },
    remote: {
      label: () => Promise.resolve('thing'),
      isImplicit: () => Promise.resolve(false),
    },
  },
  type: {
    static: {
      isType: () => true,
    },
    local: {
      label: () => 'type',
    },
    remote: {
      label: () => Promise.resolve('type'),
      isAbstract: () => Promise.resolve(false),
      attributes: () => Promise.resolve({ collect: () => Promise.resolve([]) }),
      playing: () => Promise.resolve({ collect: () => Promise.resolve([]) }),
    },
  },
  role: {
    static: {
      baseType: 'ROLE',
      id: 'role-id',
      isRole: () => true,
    },
    local: {
      label: () => 'role',
    },
    remote: {
      label: () => Promise.resolve('role'),
    },
  },
  entityType: {
    static: {
      baseType: 'ENTITY_TYPE',
      id: 'entity-type-id',
      isEntityType: () => true,
    },
    local: {
      label: () => 'entity-type',
    },
    remote: {
      label: () => Promise.resolve('entity-type'),
    },
  },
  attributeType: {
    static: {
      baseType: 'ATTRIBUTE_TYPE',
      id: 'attribute-type-id',
      isAttributeType: () => true,
    },
    local: {
      label: () => 'attribute-type',
    },
    remote: {
      label: () => Promise.resolve('attribute-type'),
      valueType: () => Promise.resolve('String'),
    },
  },
  relationType: {
    static: {
      baseType: 'RELATION_TYPE',
      id: 'relation-type-id',
      isRelationType: () => true,
    },
    local: {
      label: () => 'relation-type',
    },
    remote: {
      label: () => Promise.resolve('relation-type'),
      roles: () => Promise.resolve({ collect: () => Promise.resolve([]) }),
    },
  },
  thing: {
    static: {
      isThing: () => true,
    },
    local: {
      isInferred: () => false,
    },
    remote: {
      isInferred: () => Promise.resolve(false),
      attributes: () => Promise.resolve({ collect: () => Promise.resolve([]) }),
      relations: () => Promise.resolve({ collect: () => Promise.resolve([]) }),
      roles: () => Promise.resolve({ collect: () => Promise.resolve([]) }),
    },
  },
  entity: {
    static: {
      baseType: 'ENTITY',
      id: 'entity-id',
      isEntity: () => true,
    },
    local: {},
    remote: {},
  },
  attribute: {
    static: {
      baseType: 'ATTRIBUTE',
      id: 'attribute-id',
      isEntity: () => true,
    },
    local: {
      valueType: () => 'String',
      value: () => 'attribute-value',
    },
    remote: {
      valueType: () => Promise.resolve('String'),
      value: () => Promise.resolve('attribute-value'),
      owners: () => Promise.resolve({ collect: Promise.resolve([]) }),
    },
  },
  relation: {
    static: {
      baseType: 'RELATION',
      id: 'relation-id',
      isEntity: () => true,
    },
    local: {
      valueType: () => 'String',
      value: () => 'relation-value',
    },
    remote: {
      rolePlayersMap: () => Promise.resolve(new Map()),
    },
  },
};

const getExtraProps = (mockerOptions, agentDefinedProps) => {
  const extraProps = { remote: {}, local: {} };

  if (agentDefinedProps && agentDefinedProps.remote) extraProps.remote = { ...extraProps.remote, ...agentDefinedProps.remote };
  if (mockerOptions && mockerOptions.extraProps && mockerOptions.extraProps.remote) extraProps.remote = { ...extraProps.remote, ...mockerOptions.extraProps.remote };

  if (agentDefinedProps && agentDefinedProps.local) extraProps.local = { ...extraProps, ...agentDefinedProps.local };
  if (mockerOptions && mockerOptions.extraProps && mockerOptions.extraProps.local) extraProps.local = { ...extraProps.local, ...mockerOptions.extraProps.local };

  return extraProps;
};

const getMockedConcept = (commons, extraProps, isRemote) => {
  let staticProps = {};
  commons.forEach((common) => { staticProps = { ...staticProps, ...methods[common].static }; });

  let remoteProps = {};
  commons.forEach((common) => { remoteProps = { ...remoteProps, ...methods[common].remote }; });
  if (extraProps.remote) remoteProps = { ...remoteProps, ...extraProps.remote };

  let localProps = {};
  commons.forEach((common) => { localProps = { ...localProps, ...methods[common].local }; });
  if (extraProps.remote) localProps = { ...localProps, ...extraProps.local };

  const remoteConcept = { ...staticProps, ...remoteProps };
  const localConcept = { ...staticProps, ...localProps, asRemote: () => remoteConcept };

  const mockedConcept = isRemote ? { ...remoteConcept } : { ...localConcept };

  return mockedConcept;
};

export const getMockedMetaType = (options) => {
  const extraProps = getExtraProps(options);

  return getMockedConcept(
    ['concept'],
    extraProps,
    options && options.isRemote,
  );
};

export const getMockedEntityType = (options) => {
  const extraProps = getExtraProps(options, {
    remote: { sup: () => Promise.resolve(getMockedMetaType({ isRemote: true })) },
  });

  return getMockedConcept(
    ['concept', 'type', 'entityType'],
    extraProps,
    options && options.isRemote,
  );
};

export const getMockedAttributeType = (options) => {
  const extraProps = getExtraProps(options, {
    remote: { sup: () => Promise.resolve(getMockedMetaType({ isRemote: true })) },
  });

  return getMockedConcept(
    ['concept', 'type', 'attributeType'],
    extraProps,
    options && options.isRemote,
  );
};

export const getMockedRelationType = (options) => {
  const extraProps = getExtraProps(options, {
    remote: { sup: () => Promise.resolve(getMockedMetaType({ isRemote: true })) },
  });

  return getMockedConcept(
    ['concept', 'type', 'relationType'],
    extraProps,
    options && options.isRemote,
  );
};

export const getMockedRole = (options) => {
  const extraProps = getExtraProps(options);

  return getMockedConcept(
    ['concept', 'type', 'role'],
    extraProps,
    options && options.isRemote,
  );
};

export const getMockedEntity = (options) => {
  const extraProps = getExtraProps(options, {
    local: { type: () => getMockedEntityType() },
  });

  return getMockedConcept(
    ['concept', 'thing', 'entity'],
    extraProps,
    options && options.isRemote,
  );
};


export const getMockedAttribute = (options) => {
  const extraProps = getExtraProps(options, {
    local: { type: () => getMockedAttributeType() },
    remote: { type: () => Promise.resolve(getMockedAttributeType().asRemote()) },
  });

  return getMockedConcept(
    ['concept', 'thing', 'attribute'],
    extraProps,
    options && options.isRemote,
  );
};

export const getMockedRelation = (options) => {
  const extraProps = getExtraProps(options, {
    local: { type: () => getMockedRelationType() },
  });

  return getMockedConcept(
    ['concept', 'thing', 'relation'],
    extraProps,
    options && options.isRemote,
  );
};

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

export const getMockedTransaction = (answers, customFuncs) => {
  let mocked = {
    query: () => ({
      collect: () => Promise.resolve(answers),
      collectConcepts: () => Promise.resolve(answers.map((answer, index) => answer.map().get(index))),
    }),
    commit: () => Promise.resolve(),
    close: () => Promise.resolve(),
    isOpen: () => Promise.resolve(true),
  };
  if (customFuncs) mocked = { ...mocked, ...customFuncs };
  return mocked;
};
