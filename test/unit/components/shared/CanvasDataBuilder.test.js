import CDB from '@/components/shared/CanvasDataBuilder';

import {
  getMockedEntity,
  getMockedAttribute,
  getMockedRelation,
  getMockedConceptMap,
  getMockedAttributeType,
  getMockedEntityType,
  getMockedRole,
  getMockedRelationType,
  getMockedMetaType,
} from '../../../helpers/mockedConcepts';


jest.mock('@/components/Visualiser/RightBar/SettingsTab/QuerySettings', () => ({
  getNeighboursLimit: () => 2,
  getRolePlayersStatus: () => true,
}));

jest.mock('@/components/shared/PersistentStorage', () => ({}));

const expectCommonPropsOnInstanceNode = (node) => {
  expect(node).toHaveProperty('id');
  expect(node).toHaveProperty('baseType');
  expect(node).toHaveProperty('var');
  expect(node).toHaveProperty('attrOffset');
  expect(node).toHaveProperty('type');
  expect(node).toHaveProperty('isInferred');
  if (node.isInferred) {
    expect(node).toHaveProperty('explanation');
    expect(node).toHaveProperty('queryPattern');
  }
  expect(node).toHaveProperty('attributes');
};

describe('building instances', () => {
  test('when graql answer contains an entity instance with an attribute', async () => {
    const attribute = getMockedAttribute({
      isRemote: false,
      customFuncs: {
        owners: () => Promise.resolve({ collect: () => Promise.resolve([getMockedEntity({ isRemote: true })]) }),
        type: () => getMockedAttributeType({ isRemote: false }),
      },
    });

    const answer = getMockedConceptMap([
      getMockedEntity({
        isRemote: false,
        customFuncs: { type: () => getMockedEntityType({ isRemote: false }) },
      }),
      attribute,
    ]);

    const { nodes, edges } = await CDB.buildInstances([answer]);

    expect(nodes).toHaveLength(2);
    nodes.forEach(node => expectCommonPropsOnInstanceNode(node));

    const entityNode = nodes.find(node => node.baseType === 'ENTITY');
    expect(entityNode.label).toEqual('entity-type: entity-id');
    expect(entityNode.offset).toEqual(0);

    const attributeNode = nodes.find(node => node.baseType === 'ATTRIBUTE');
    expect(attributeNode.value).toEqual('attribute-value');
    expect(attributeNode.label).toEqual('attribute-type: attribute-value');
    expect(attributeNode.offset).toEqual(0);

    expect(edges).toHaveLength(1);
    const edge = edges[0];
    expect(edge).toEqual({
      arrows: { to: { enabled: false } },
      from: 'entity-id',
      hiddenLabel: 'has',
      id: 'entity-id-attribute-id-has',
      label: '',
      options: { hideArrow: true, hideLabel: true },
      to: 'attribute-id',
    });
  });

  test('when graql answer contains a relation instance with a roleplayer', async () => {
    const entity = getMockedEntity({
      isRemote: false,
      customFuncs: { type: () => getMockedEntityType({ isRemote: false }) },
    });

    const relation = getMockedRelation({
      isRemote: false,
      customFuncs: {
        type: () => getMockedRelationType({ isRemote: false }),
        rolePlayersMap: () => Promise.resolve(new Map([[
          getMockedRole({ isRemote: true }),
          [entity],
        ]])),
      },
    });

    const answer = getMockedConceptMap([relation, entity]);

    const { nodes, edges } = await CDB.buildInstances([answer]);

    expect(nodes).toHaveLength(2);
    nodes.forEach(node => expectCommonPropsOnInstanceNode(node));

    const relationNode = nodes.find(node => node.baseType === 'RELATION');
    expect(relationNode.label).toEqual('');
    expect(relationNode.offset).toEqual(2);

    const entityNode = nodes.find(node => node.baseType === 'ENTITY');
    expect(entityNode.label).toEqual('entity-type: entity-id');
    expect(entityNode.offset).toEqual(0);

    expect(edges).toHaveLength(1);
    const edge = edges[0];
    expect(edge).toEqual({
      id: 'relation-id-entity-id-role',
      arrows: { to: { enabled: false } },
      from: 'relation-id',
      hiddenLabel: 'role',
      label: '',
      options: { hideArrow: true, hideLabel: true },
      to: 'entity-id',
    });
  });

  test('when graql answer contains an explanation', async () => {
    const entity = getMockedEntity({
      isRemote: false,
      customFuncs: {
        isInferred: () => true,
        type: () => getMockedEntityType({ isRemote: false }),
      },
    });
    const explConcept = getMockedAttribute({ isRemote: true });
    const explAnswer = getMockedConceptMap([explConcept]);
    const answer = getMockedConceptMap([entity], [explAnswer]);

    const { nodes } = await CDB.buildInstances([answer]);

    const explAnswers = await nodes[0].explanation().getAnswers();
    expect(explAnswers[0].map().get(0)).toEqual(explConcept);
  });

  test('when graql answer contains an implicit instance', async () => {
    const attribute = getMockedAttribute({
      isRemote: false,
      customFuncs: {
        type: () => getMockedAttributeType({
          isRemote: false,
          customFuncs: { isImplicit: () => true },
        }),
      },
    });
    const answer = getMockedConceptMap([attribute]);
    const { nodes, edges } = await CDB.buildInstances([answer]);
    expect(nodes).toHaveLength(0);
    expect(edges).toHaveLength(0);
  });
});

describe('building types', () => {
  test('when graql answer contains a subtype of a meta type', async () => {
    const entityType = getMockedEntityType({
      isRemote: false,
      customFuncs: { sup: () => Promise.resolve(getMockedEntityType({
        isRemote: true,
        customFuncs: { id: 'super-entity-type-id' } })),
      },
    });
    const answer = getMockedConceptMap([entityType]);
    const { nodes, edges } = await CDB.buildTypes([answer]);

    expect(nodes).toHaveLength(1);

    const node = nodes[0];

    expect(node).toHaveProperty('id');
    expect(node).toHaveProperty('baseType');
    expect(node).toHaveProperty('var');
    expect(node).toHaveProperty('attrOffset');
    expect(node).toHaveProperty('offset');
    expect(node).toHaveProperty('attributes');
    expect(node).toHaveProperty('playing');
    expect(node).not.toHaveProperty('explanation');

    expect(node.label).toEqual('entity-type');

    expect(edges).toHaveLength(0);
  });

  test('when graql answer contains a type with an attribute where the type is a subtype of a meta type', async () => {
    const attributeType = getMockedAttributeType({ isRemote: false });
    const entityType = getMockedEntityType({
      isRemote: false,
      customFuncs: {
        attributes: () => Promise.resolve({ collect: () => Promise.resolve([attributeType]) }),
      },
    });
    const answer = getMockedConceptMap([entityType, attributeType]);

    const { nodes, edges } = await CDB.buildTypes([answer]);

    expect(nodes).toHaveLength(2);

    expect(edges).toEqual([{
      id: 'entity-type-id-attribute-type-id-has',
      arrows: { to: { enabled: true } },
      from: 'entity-type-id',
      label: 'has',
      options: { hideArrow: false, hideLabel: false },
      to: 'attribute-type-id',
    }]);
  });

  test('when graql answer contains a type that owns the same attributes as its supertype that is not a meta type', async () => {
    const attributeType = getMockedAttributeType({ isRemote: false });
    const superEntityType = getMockedEntityType({
      isRemote: false,
      customFuncs: {
        id: 'some-super-entity-type-id',
        attributes: () => Promise.resolve({ collect: () => Promise.resolve([attributeType]) }),
      },
    });

    const subEntityType = getMockedEntityType({
      isRemote: false,
      customFuncs: {
        id: 'sub-entity-type-id',
        attributes: () => Promise.resolve({ collect: () => Promise.resolve([attributeType]) }),
        sup: () => Promise.resolve(superEntityType),
      },
    });

    const answer = getMockedConceptMap([attributeType, superEntityType, subEntityType]);

    const { nodes, edges } = await CDB.buildTypes([answer]);

    expect(nodes).toHaveLength(3);

    expect(edges).toEqual([
      {
        arrows: { to: { enabled: true } },
        from: 'some-super-entity-type-id',
        id: 'some-super-entity-type-id-attribute-type-id-has',
        label: 'has',
        options: { hideArrow: false, hideLabel: false },
        to: 'attribute-type-id',
      },
      {
        arrows: { to: { enabled: true } },
        from: 'sub-entity-type-id',
        id: 'sub-entity-type-id-some-super-entity-type-id-sub',
        label: 'sub',
        options: { hideArrow: false, hideLabel: false },
        to: 'some-super-entity-type-id',
      },
    ]);
  });

  test('when graql answer contains a type that extends its non-meta supertype by owning more attributes', async () => {
    const attributeType = getMockedAttributeType({ isRemote: false });
    const superEntityType = getMockedEntityType({
      isRemote: false,
      customFuncs: {
        id: 'some-super-entity-type-id',
        attributes: () => Promise.resolve({ collect: () => Promise.resolve([attributeType]) }),
      },
    });

    const anotherAttributeType = getMockedAttributeType({
      isRemote: false,
      customFuncs: { id: 'another-attribute-type-id' },
    });
    const subEntityType = getMockedEntityType({
      isRemote: false,
      customFuncs: {
        id: 'sub-entity-type-id',
        sup: () => Promise.resolve(superEntityType),
        attributes: () => Promise.resolve({ collect: () => Promise.resolve([attributeType, anotherAttributeType]) }),
      },
    });

    const answer = getMockedConceptMap([subEntityType, superEntityType, attributeType, anotherAttributeType]);

    const { nodes, edges } = await CDB.buildTypes([answer]);

    expect(nodes).toHaveLength(4);
    expect(edges).toEqual([
      {
        arrows: { to: { enabled: true } },
        from: 'sub-entity-type-id',
        id: 'sub-entity-type-id-some-super-entity-type-id-sub',
        label: 'sub',
        options: { hideArrow: false, hideLabel: false },
        to: 'some-super-entity-type-id',
      },
      {
        arrows: { to: { enabled: true } },
        from: 'sub-entity-type-id',
        id: 'sub-entity-type-id-another-attribute-type-id-has',
        label: 'has',
        options: { hideArrow: false, hideLabel: false },
        to: 'another-attribute-type-id',
      },
      {
        arrows: { to: { enabled: true } },
        from: 'some-super-entity-type-id',
        id: 'some-super-entity-type-id-attribute-type-id-has',
        label: 'has',
        options: { hideArrow: false, hideLabel: false },
        to: 'attribute-type-id',
      },
    ]);
  });

  test('when graql answer contains a relation type with a roleplayer', async () => {
    const role = getMockedRole({ isRemote: true });

    const relationType = getMockedRelationType({
      isRemote: false,
      customFuncs: {
        roles: () => Promise.resolve({ collect: () => Promise.resolve([role]) }),
      },
    });

    const entityType = getMockedEntityType({
      isRemote: false,
      customFuncs: {
        playing: () => Promise.resolve({ collect: () => Promise.resolve([role]) }),
      },
    });

    role.relations = () => Promise.resolve({ collect: () => Promise.resolve([relationType]) });
    role.players = () => Promise.resolve({ collect: () => Promise.resolve([entityType]) });

    const answer = getMockedConceptMap([entityType, relationType]);

    const { nodes, edges } = await CDB.buildTypes([answer]);

    expect(nodes).toHaveLength(2);

    expect(edges).toEqual([
      {
        arrows: { to: { enabled: true } },
        from: 'relation-type-id',
        id: 'relation-type-id-entity-type-id-role',
        label: 'role',
        options: { hideArrow: false, hideLabel: false },
        to: 'entity-type-id',
      },
      {
        arrows: { to: { enabled: true } },
        from: 'relation-type-id',
        id: 'relation-type-id-entity-type-id-role',
        label: 'role',
        options: { hideArrow: false, hideLabel: false },
        to: 'entity-type-id',
      },
    ]);
  });

  test('when graql answer contains a meta type', async () => {
    const metaType = getMockedMetaType({ isRemote: false });
    const answer = getMockedConceptMap([metaType]);

    const { nodes, edges } = await CDB.buildTypes([answer]);
    expect(nodes).toHaveLength(0);
    expect(edges).toHaveLength(0);
  });

  test('when graql answer contains an implicit type', async () => {
    const relationType = getMockedRelationType({
      isRemote: false,
      customFuncs: {
        isImplicit: () => true,
      },
    });

    const answer = getMockedConceptMap([relationType]);

    const { nodes, edges } = await CDB.buildTypes([answer]);
    expect(nodes).toHaveLength(0);
    expect(edges).toHaveLength(0);
  });
});

describe('building neighbours', () => {
  test('when the target node is a type', async () => {
    const entityType = getMockedEntityType({ isRemote: false });
    const entity = getMockedEntity({
      isRemote: true,
      customFuncs: {
        type: () => entityType,
      },
    });

    const answer = getMockedConceptMap([entity]);

    const neighboursData = await CDB.buildNeighbours(entityType, [answer]);

    expect(neighboursData.nodes).toHaveLength(1);

    const node = neighboursData.nodes[0];
    expect(node).toMatchObject({
      baseType: 'ENTITY',
      id: 'entity-id',
      offset: 0,
      var: 0,
      label: 'entity-type: entity-id',
      type: 'entity-type',
    });

    expect(neighboursData.edges).toHaveLength(1);

    const edge = neighboursData.edges[0];
    expect(edge).toEqual({
      arrows: { to: { enabled: false } },
      from: 'entity-id',
      hiddenLabel: 'isa',
      id: 'entity-id-entity-type-id-isa',
      label: '',
      options: { hideArrow: true, hideLabel: true },
      to: 'entity-type-id',
    });
  });

  test('when the target node is an entity instance', async () => {
    const entity = getMockedEntity({ isRemote: true });
    const relation = getMockedRelation({
      isRemote: false,
      customFuncs: {
        rolePlayersMap: () => Promise.resolve(new Map([[getMockedRole({ isRemote: true }), [entity]]])),
      },
    });
    const answer = getMockedConceptMap([relation]);

    const neighboursData = await CDB.buildNeighbours(entity, [answer]);

    expect(neighboursData.nodes).toHaveLength(1);
    const node = neighboursData.nodes[0];
    expect(node).toMatchObject({ baseType: 'RELATION', id: 'relation-id', offset: 2, var: 0, type: 'relation-type', label: '' });

    expect(neighboursData.edges).toHaveLength(1);
    const edge = neighboursData.edges[0];
    expect(edge).toEqual({
      arrows: { to: { enabled: false } },
      from: 'relation-id',
      hiddenLabel: 'role',
      id: 'relation-id-entity-id-role',
      label: '',
      options: { hideArrow: true, hideLabel: true },
      to: 'entity-id',
    });
  });

  test('when the target node is an attribute instance', async () => {
    const attribute = getMockedAttribute({ isRemote: false });
    const entity = getMockedEntity({ isRemote: false });
    const answer = getMockedConceptMap([entity]);

    const neighboursData = await CDB.buildNeighbours(attribute, [answer]);

    expect(neighboursData.nodes).toHaveLength(1);
    const node = neighboursData.nodes[0];
    expect(node).toMatchObject({ baseType: 'ENTITY', id: 'entity-id', offset: 0, var: 0, type: 'entity-type', label: 'entity-type: entity-id' });

    expect(neighboursData.edges).toHaveLength(1);
    const edge = neighboursData.edges[0];
    expect(edge).toEqual({
      arrows: { to: { enabled: false } },
      from: 'entity-id',
      hiddenLabel: 'has',
      id: 'entity-id-attribute-id-has',
      label: '',
      options: { hideArrow: true, hideLabel: true },
      to: 'attribute-id',
    });
  });

  test('when the target node is an relation instance', async () => {
    const entity = getMockedEntity({ isRemote: false });
    const relation = getMockedRelation({
      isRemote: false,
      customFuncs: {
        rolePlayersMap: () => Promise.resolve(new Map([[getMockedRole({ isRemote: true }), [entity]]])),
      },
    });

    const answer = getMockedConceptMap([entity]);

    const neighboursData = await CDB.buildNeighbours(relation, [answer]);

    expect(neighboursData.nodes).toHaveLength(1);
    const node = neighboursData.nodes[0];
    expect(node).toMatchObject({ baseType: 'ENTITY', id: 'entity-id', offset: 0, var: 0, type: 'entity-type', label: 'entity-type: entity-id' });

    expect(neighboursData.edges).toHaveLength(1);
    const edge = neighboursData.edges[0];
    expect(edge).toEqual({
      arrows: { to: { enabled: false } },
      from: 'relation-id',
      hiddenLabel: 'role',
      id: 'relation-id-entity-id-role',
      label: '',
      options: { hideArrow: true, hideLabel: true },
      to: 'entity-id',
    });
  });
});
