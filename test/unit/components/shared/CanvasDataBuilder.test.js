import CDB from '@/components/shared/CanvasDataBuilder';
import {
  mockedMetaType,
  mockedRole,
  mockedEntityType,
  mockedRelationType,
  mockedAttributeType,
  mockedEntityInstance,
  mockedRelationInstance,
  mockedAttributeInstance,
  getMockedAnswer,
  getMockedExplanation,
  getMockedGraknTx,
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
  expect(node).toHaveProperty('explanation');
  expect(node).toHaveProperty('attrOffset');
  expect(node).toHaveProperty('type');
  expect(node).toHaveProperty('isInferred');
  expect(node).toHaveProperty('attributes');
  expect(node).toHaveProperty('txService');
};

describe('building instances', () => {
  test('when graql answer contains an entity instance with an attribute', async () => {
    const entityInstance = { ...mockedEntityInstance };
    const attributeInstance = { ...mockedAttributeInstance };
    attributeInstance.owners = () => Promise.resolve({ collect: () => Promise.resolve([entityInstance]) });
    const answers = [getMockedAnswer([entityInstance, attributeInstance], null)];
    const { nodes, edges } = await CDB.buildInstances(answers);

    expect(nodes).toHaveLength(2);
    nodes.forEach(node => expectCommonPropsOnInstanceNode(node));

    const entityNode = nodes.find(node => node.baseType === 'ENTITY');
    expect(entityNode.label).toEqual('some-entity-type: ent-instance');
    expect(entityNode.offset).toEqual(0);

    const attributeNode = nodes.find(node => node.baseType === 'ATTRIBUTE');
    expect(attributeNode.value).toEqual('some value');
    expect(attributeNode.label).toEqual('some-attribute-type: some value');
    expect(attributeNode.offset).toEqual(0);

    expect(edges).toHaveLength(1);
    const edge = edges[0];
    expect(edge).toEqual({
      arrows: { to: { enabled: false } },
      from: 'ent-instance',
      hiddenLabel: 'has',
      id: 'ent-instance-attr-instance-has',
      label: '',
      options: { hideArrow: true, hideLabel: true },
      to: 'attr-instance',
    });
  });

  test('when graql answer contains a relation instance with a roleplayer', async () => {
    const entityInstance = { ...mockedEntityInstance };
    const rolePlayersMap = new Map([[mockedRole, [entityInstance]]]);
    const relationInstance = {
      ...mockedRelationInstance,
      rolePlayersMap: () => Promise.resolve(rolePlayersMap),
    };
    const answers = [getMockedAnswer([relationInstance, entityInstance], null)];
    const { nodes, edges } = await CDB.buildInstances(answers);

    expect(nodes).toHaveLength(2);
    nodes.forEach(node => expectCommonPropsOnInstanceNode(node));

    const relationNode = nodes.find(node => node.baseType === 'RELATION');
    expect(relationNode.label).toEqual('');
    expect(relationNode.offset).toEqual(2);

    const entityNode = nodes.find(node => node.baseType === 'ENTITY');
    expect(entityNode.label).toEqual('some-entity-type: ent-instance');
    expect(entityNode.offset).toEqual(0);

    expect(edges).toHaveLength(1);
    const edge = edges[0];
    expect(edge).toEqual({
      id: 'rel-instance-ent-instance-some-role',
      arrows: { to: { enabled: false } },
      from: 'rel-instance',
      hiddenLabel: 'some-role',
      label: '',
      options: { hideArrow: true, hideLabel: true },
      to: 'ent-instance',
    });
  });

  test('when graql answer contains an explanation', async () => {
    const explConcept = mockedAttributeInstance;
    const explAnswer = getMockedAnswer([explConcept], null);
    const explanation = getMockedExplanation([explAnswer]);
    const answers = [getMockedAnswer([mockedEntityInstance], explanation)];
    const { nodes } = await CDB.buildInstances(answers);

    expect(nodes[0].explanation.answers()[0].map().get(0)).toEqual(explConcept);
  });

  test('when graql answer contains an implicit instance', async () => {
    const attributeInstance = {
      ...mockedAttributeInstance,
      type: () => Promise.resolve({ isImplicit: () => Promise.resolve(true) }),
    };
    const answers = [getMockedAnswer([attributeInstance], null)];
    const { nodes, edges } = await CDB.buildInstances(answers);
    expect(nodes).toHaveLength(0);
    expect(edges).toHaveLength(0);
  });
});

describe('building types', () => {
  test('when graql answer contains an subtype of a meta type', async () => {
    const entityType = {
      ...mockedEntityType,
      sup: () => Promise.resolve({ label: () => Promise.resolve('entity') }),
    };
    const answers = [getMockedAnswer([entityType], null)];
    const { nodes, edges } = await CDB.buildTypes(answers);

    expect(nodes).toHaveLength(1);

    const node = nodes[0];

    expect(node).toHaveProperty('id');
    expect(node).toHaveProperty('baseType');
    expect(node).toHaveProperty('var');
    expect(node).toHaveProperty('attrOffset');
    expect(node).toHaveProperty('offset');
    expect(node).toHaveProperty('attributes');
    expect(node).toHaveProperty('playing');
    expect(node).toHaveProperty('txService');

    expect(node).not.toHaveProperty('explanation');

    expect(node.label).toEqual('some-entity-type');

    expect(edges).toHaveLength(0);
  });

  test('when graql answer contains a type with an attribute where the type is a subtype of a meta type', async () => {
    const attributeType = { ...mockedAttributeType };
    const entityType = {
      ...mockedEntityType,
      sup: () => Promise.resolve({ label: () => Promise.resolve('entity') }),
      attributes: () => Promise.resolve({ collect: () => Promise.resolve([attributeType]) }),
    };
    const answers = [getMockedAnswer([entityType, attributeType], null)];
    const { nodes, edges } = await CDB.buildTypes(answers);

    expect(nodes).toHaveLength(2);

    expect(edges).toEqual([{
      id: 'ent-type-attr-type-has',
      arrows: { to: { enabled: true } },
      from: 'ent-type',
      label: 'has',
      options: { hideArrow: false, hideLabel: false },
      to: 'attr-type',
    }]);
  });

  test('when graql answer contains a type that owns the same attributes as its supertype that is not a meta type', async () => {
    const attributeType = { ...mockedAttributeType };

    const superType = {
      ...mockedEntityType,
      id: 'supertype',
      label: () => Promise.resolve('some other entity'),
      attributes: () => Promise.resolve({ collect: () => Promise.resolve([mockedAttributeType]) }),
    };

    const subType = {
      ...mockedEntityType,
      id: 'subtype',
      attributes: () => Promise.resolve({ collect: () => Promise.resolve([mockedAttributeType]) }),
      sup: () => Promise.resolve(superType),
    };

    const answers = [getMockedAnswer([subType, attributeType, superType], null)];
    const { nodes, edges } = await CDB.buildTypes(answers);

    expect(nodes).toHaveLength(3);

    expect(edges).toEqual([
      {
        arrows: { to: { enabled: true } },
        from: 'subtype',
        id: 'subtype-supertype-sub',
        label: 'sub',
        options: { hideArrow: false, hideLabel: false },
        to: 'supertype',
      },
      {
        arrows: { to: { enabled: true } },
        from: 'supertype',
        id: 'supertype-attr-type-has',
        label: 'has',
        options: { hideArrow: false, hideLabel: false },
        to: 'attr-type',
      },
    ]);
  });

  test('when graql answer contains a type that extends its non-meta supertype by owning more attributes', async () => {
    const attributeType = { ...mockedAttributeType };
    const superEntityType = {
      ...mockedEntityType,
      id: 'supertype',
      label: () => Promise.resolve('supertype entity'),
      attributes: () => Promise.resolve({ collect: () => Promise.resolve([attributeType]) }),
    };

    const extraAttributeType = { ...mockedAttributeType, id: 'the-extra-attribute' };
    const subEntityType = {
      ...mockedEntityType,
      attributes: () => Promise.resolve({ collect: () => Promise.resolve([attributeType, extraAttributeType]) }),
      sup: () => Promise.resolve(superEntityType),
    };

    const answers = [getMockedAnswer([subEntityType, superEntityType, attributeType, extraAttributeType], null)];
    const { nodes, edges } = await CDB.buildTypes(answers);

    expect(nodes).toHaveLength(4);
    expect(edges).toEqual([
      {
        arrows: { to: { enabled: true } },
        from: 'ent-type',
        id: 'ent-type-supertype-sub',
        label: 'sub',
        options: { hideArrow: false, hideLabel: false },
        to: 'supertype',
      },
      {
        arrows: { to: { enabled: true } },
        from: 'ent-type',
        id: 'ent-type-the-extra-attribute-has',
        label: 'has',
        options: { hideArrow: false, hideLabel: false },
        to: 'the-extra-attribute',
      },
      {
        arrows: { to: { enabled: true } },
        from: 'supertype',
        id: 'supertype-attr-type-has',
        label: 'has',
        options: { hideArrow: false, hideLabel: false },
        to: 'attr-type',
      },
    ]);
  });

  test('when graql answer contains a relation type with a roleplayer', async () => {
    const role = { ...mockedRole };

    const relationType = {
      ...mockedRelationType,
      roles: () => Promise.resolve({ collect: () => Promise.resolve([role]) }),
    };

    role.relations = () => Promise.resolve({
      collect: () => Promise.resolve([{ ...relationType }]),
    });

    const entityType = {
      ...mockedEntityType,
      playing: () => Promise.resolve({
        collect: () => Promise.resolve([role]),
      }),
    };

    role.players = () => Promise.resolve({ collect: () => Promise.resolve([entityType]) });

    const answers = [getMockedAnswer([entityType, relationType], null)];
    const { nodes, edges } = await CDB.buildTypes(answers);

    expect(nodes).toHaveLength(2);

    expect(edges).toEqual([
      {
        arrows: { to: { enabled: true } },
        from: 'rel-type',
        id: 'rel-type-ent-type-some-role',
        label: 'some-role',
        options: { hideArrow: false, hideLabel: false },
        to: 'ent-type',
      },
      {
        arrows: { to: { enabled: true } },
        from: 'rel-type',
        id: 'rel-type-ent-type-some-role',
        label: 'some-role',
        options: { hideArrow: false, hideLabel: false },
        to: 'ent-type',
      },
    ]);
  });

  test('when graql answer contains a meta type', async () => {
    const answers = [getMockedAnswer([mockedMetaType], null)];
    const { nodes, edges } = await CDB.buildTypes(answers);
    expect(nodes).toHaveLength(0);
    expect(edges).toHaveLength(0);
  });

  test('when graql answer contains an implicit type', async () => {
    const metaType = {
      ...mockedMetaType,
      label: () => Promise.resolve('some implicit entity'),
      isImplicit: () => Promise.resolve(true),
    };
    const answers = [getMockedAnswer([metaType], null)];
    const { nodes, edges } = await CDB.buildTypes(answers);
    expect(nodes).toHaveLength(0);
    expect(edges).toHaveLength(0);
  });
});

describe('building neighbours', () => {
  test('when the target node is a type', async () => {
    const entityType = { ...mockedEntityType };
    const entityInstance = { ...mockedEntityInstance };
    const answer = getMockedAnswer([entityInstance]);
    const graknTx = getMockedGraknTx([]);
    const neighboursData = await CDB.buildNeighbours(entityType, [answer], graknTx);

    expect(neighboursData.nodes).toHaveLength(1);

    const node = neighboursData.nodes[0];
    expect(node).toMatchObject({
      baseType: 'ENTITY',
      id: 'ent-instance',
      offset: 0,
      var: 0,
      label: 'some-entity-type: ent-instance',
      type: 'some-entity-type',
    });

    expect(neighboursData.edges).toHaveLength(1);

    const edge = neighboursData.edges[0];
    expect(edge).toEqual({
      arrows: { to: { enabled: false } },
      from: 'ent-instance',
      hiddenLabel: 'isa',
      id: 'ent-instance-ent-type-isa',
      label: '',
      options: { hideArrow: true, hideLabel: true },
      to: 'ent-type',
    });
  });

  test('when the target node is a entity instance', async () => {
    const entityInstance = { ...mockedEntityInstance };
    const relationInstance = { ...mockedRelationInstance };
    const rolePlayersMap = new Map();
    rolePlayersMap.set(mockedRole, [entityInstance]);
    relationInstance.rolePlayersMap = () => Promise.resolve(rolePlayersMap);
    const answer = getMockedAnswer([relationInstance]);
    const graknTx = getMockedGraknTx([]);
    const neighboursData = await CDB.buildNeighbours(entityInstance, [answer], graknTx);

    expect(neighboursData.nodes).toHaveLength(1);
    const node = neighboursData.nodes[0];
    expect(node).toMatchObject({ baseType: 'RELATION', id: 'rel-instance', offset: 2, var: 0, type: 'some-relation-type', label: '' });

    expect(neighboursData.edges).toHaveLength(1);
    const edge = neighboursData.edges[0];
    expect(edge).toEqual({
      arrows: { to: { enabled: false } },
      from: 'rel-instance',
      hiddenLabel: 'some-role',
      id: 'rel-instance-ent-instance-some-role',
      label: '',
      options: { hideArrow: true, hideLabel: true },
      to: 'ent-instance',
    });
  });

  test('when the target node is an attribute instance', async () => {
    const attributeInstance = { ...mockedAttributeInstance };
    const entityInstance = { ...mockedEntityInstance };
    const answer = getMockedAnswer([entityInstance]);
    const graknTx = getMockedGraknTx([]);
    const neighboursData = await CDB.buildNeighbours(attributeInstance, [answer], graknTx);

    expect(neighboursData.nodes).toHaveLength(1);
    const node = neighboursData.nodes[0];
    expect(node).toMatchObject({ baseType: 'ENTITY', id: 'ent-instance', offset: 0, var: 0, type: 'some-entity-type', label: 'some-entity-type: ent-instance' });

    expect(neighboursData.edges).toHaveLength(1);
    const edge = neighboursData.edges[0];
    expect(edge).toEqual({
      arrows: { to: { enabled: false } },
      from: 'ent-instance',
      hiddenLabel: 'has',
      id: 'ent-instance-attr-instance-has',
      label: '',
      options: { hideArrow: true, hideLabel: true },
      to: 'attr-instance',
    });
  });

  test('when the target node is an relation instance', async () => {
    const entityInstance = { ...mockedEntityInstance };
    const role = { ...mockedRole };
    const rolePlayersMap = new Map([[role, [entityInstance]]]);
    const relationInstance = {
      ...mockedRelationInstance,
      rolePlayersMap: () => Promise.resolve(rolePlayersMap),
    };
    const answer = getMockedAnswer([entityInstance]);
    const graknTx = getMockedGraknTx([], {
      getConcept: () => Promise.resolve(relationInstance),
    });
    const neighboursData = await CDB.buildNeighbours(relationInstance, [answer], graknTx);

    expect(neighboursData.nodes).toHaveLength(1);
    const node = neighboursData.nodes[0];
    expect(node).toMatchObject({ baseType: 'ENTITY', id: 'ent-instance', offset: 0, var: 0, type: 'some-entity-type', label: 'some-entity-type: ent-instance' });

    expect(neighboursData.edges).toHaveLength(1);
    const edge = neighboursData.edges[0];
    expect(edge).toEqual({
      arrows: { to: { enabled: false } },
      from: 'rel-instance',
      hiddenLabel: 'some-role',
      id: 'rel-instance-ent-instance-some-role',
      label: '',
      options: { hideArrow: true, hideLabel: true },
      to: 'ent-instance',
    });
  });
});
