import CDB from '@/components/shared/CanvasDataBuilder';

jest.mock('@/components/Visualiser/RightBar/SettingsTab/QuerySettings', () => ({
  getNeighboursLimit: () => 2,
}));

jest.mock('@/components/shared/PersistentStorage', () => ({}));

const getMockedExplanation = answers => ({ answers: () => answers });

const getMockedAnswer = (concepts, explanation) => {
  const answer = {};

  const map = new Map();
  concepts.forEach((concept, index) => { map.set(index, concept); });
  answer.map = () => map;
  answer.explanation = () => explanation;

  return answer;
};

const mockedMetaType = {
  isRole: () => false,
  isType: () => true,
  isThing: () => false,
  isImplicit: () => Promise.resolve(false),
  label: () => Promise.resolve('entity'),
};

const mockedEntityType = {
  id: 'ent-type',
  baseType: 'ENTITY_TYPE',
  isRole: () => false,
  isType: () => true,
  isThing: () => false,
  label: () => Promise.resolve('some entity type'),
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
  label: () => Promise.resolve('some relation type'),
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
  label: () => Promise.resolve('some attribute type'),
  attributes: () => Promise.resolve({ collect: () => Promise.resolve([]) }),
  isImplicit: () => Promise.resolve(false),
  sup: () => Promise.resolve({ label: () => Promise.resolve('attribute') }),
  playing: () => Promise.resolve({ collect: () => Promise.resolve([]) }),
};

const mockedEntityInstance = {
  id: 'ent-instance',
  baseType: 'ENTITY',
  isRole: () => false,
  isType: () => false,
  isThing: () => true,
  isAttribute: () => false,
  isInferred: () => Promise.resolve(false),
  type: () => Promise.resolve(mockedEntityType),
};

const mockedRelationInstance = {
  id: 'rel-instance',
  baseType: 'RELATION',
  isRole: () => false,
  isType: () => false,
  isThing: () => true,
  isAttribute: () => false,
  isInferred: () => Promise.resolve(false),
  type: () => Promise.resolve(mockedRelationType),
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

describe('buildInstances', () => {
  test('when graql answer contains an entity instance', async () => {
    const answers = [getMockedAnswer([mockedEntityInstance], null)];
    const { nodes, edges } = await CDB.buildInstances(answers);

    expect(nodes).toHaveLength(1);

    const node = nodes[0];

    expectCommonPropsOnInstanceNode(node);

    expect(node.label).toEqual('some entity type: ent-instance');
    expect(node.offset).toEqual(0);

    expect(edges).toHaveLength(0);
  });

  test('when graql answer contains a relation instance', async () => {
    const answers = [getMockedAnswer([mockedRelationInstance], null)];
    const { nodes, edges } = await CDB.buildInstances(answers);

    expect(nodes).toHaveLength(1);

    const node = nodes[0];

    expectCommonPropsOnInstanceNode(node);

    expect(node.label).toEqual('');
    expect(node.offset).toEqual(2);

    expect(edges).toHaveLength(0);
  });

  test('when graql answer contains an attribute', async () => {
    const attributeInstance = {
      ...mockedAttributeInstance,
      owners: () => Promise.resolve({ collect: () => Promise.resolve([mockedEntityInstance]) }),
    };
    const answers = [getMockedAnswer([attributeInstance], null)];
    const { nodes, edges } = await CDB.buildInstances(answers);

    expect(nodes).toHaveLength(1);
    const node = nodes[0];

    expectCommonPropsOnInstanceNode(node);

    expect(node.value).toEqual('some value');
    expect(node.label).toEqual('some attribute type: some value');
    expect(node.offset).toEqual(0);

    expect(edges).toEqual([
      { arrows: { to: { enable: false } }, from: 'ent-instance', hiddenLabel: 'has', label: '', options: { hideArrow: true, hideLabel: true }, to: 'attr-instance' },
    ]);
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

    expect(node.label).toEqual('some entity type');

    expect(edges).toHaveLength(0);
  });

  test('when graql answer contains a type that owns attributes and is a subtype of a meta type', async () => {
    const entityType = {
      ...mockedEntityType,
      sup: () => Promise.resolve({ label: () => Promise.resolve('entity') }),
      attributes: () => Promise.resolve({ collect: () => Promise.resolve([mockedAttributeType]) }),
    };
    const answers = [getMockedAnswer([entityType], null)];
    const { nodes, edges } = await CDB.buildTypes(answers);

    expect(nodes).toHaveLength(1);
    expect(edges).toEqual([{ arrows: { to: { enabled: true } }, from: 'ent-type', label: 'has', options: { hideArrow: false, hideLabel: false }, to: 'attr-type' }]);
  });

  test('when graql answer contains a type that owns the same attributes as its supertype that is not a meta type', async () => {
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

    const answers = [getMockedAnswer([subType], null)];
    const { nodes, edges } = await CDB.buildTypes(answers);

    expect(nodes).toHaveLength(1);
    expect(edges).toEqual([{ arrows: { to: { enabled: true } }, from: 'subtype', label: 'sub', options: { hideArrow: false, hideLabel: false }, to: 'supertype' }]);
  });

  test('when graql answer contains a type that extends its non-meta supertype by owning more attributes', async () => {
    const superEntityType = {
      ...mockedEntityType,
      id: 'supertype',
      label: () => Promise.resolve('supertype entity'),
      attributes: () => Promise.resolve({ collect: () => Promise.resolve([mockedAttributeType]) }),
    };

    const extraAttributeType = { ...mockedAttributeType, id: 'the extra attribute' };
    const subEntityType = {
      ...mockedEntityType,
      attributes: () => Promise.resolve({ collect: () => Promise.resolve([mockedAttributeType, extraAttributeType]) }),
      sup: () => Promise.resolve(superEntityType),
    };

    const answers = [getMockedAnswer([subEntityType], null)];
    const { nodes, edges } = await CDB.buildTypes(answers);

    expect(nodes).toHaveLength(1);
    expect(edges).toEqual([
      { arrows: { to: { enabled: true } }, from: 'ent-type', label: 'sub', options: { hideArrow: false, hideLabel: false }, to: 'supertype' },
      { arrows: { to: { enabled: true } }, from: 'ent-type', label: 'has', options: { hideArrow: false, hideLabel: false }, to: 'the extra attribute' },
    ]);
  });

  test('when graql answer contains a type that plays a role in a relation', async () => {
    const roleplayerType = {
      ...mockedEntityType,
      playing: () => Promise.resolve({
        collect: () => Promise.resolve([
          { label: () => Promise.resolve('role a'),
            relations: () => Promise.resolve({
              collect: () => Promise.resolve([{ ...mockedRelationType, id: 'relation' }]),
            }) },
        ]),
      }),
      id: 'roleplayer',
    };

    const answers = [getMockedAnswer([roleplayerType], null)];
    const { edges } = await CDB.buildTypes(answers);

    expect(edges).toEqual([{ arrows: { to: { enabled: true } }, from: 'relation', label: 'role a', options: { hideArrow: false, hideLabel: false }, to: 'roleplayer' }]);
  });

  test('when graql answer contains a relation type', async () => {
    const relationType = {
      ...mockedRelationType,
      roles: () => Promise.resolve({ collect: () => Promise.resolve([
        { label: () => Promise.resolve('role a'), players: () => Promise.resolve({ collect: () => Promise.resolve([mockedEntityType]) }) },
        { label: () => Promise.resolve('role b'), players: () => Promise.resolve({ collect: () => Promise.resolve([mockedAttributeType]) }) },
      ]) }),
    };
    const answers = [getMockedAnswer([relationType], null)];

    const { nodes, edges } = await CDB.buildTypes(answers);

    expect(nodes).toHaveLength(1);

    expect(edges).toEqual([
      { arrows: { to: { enabled: true } }, from: 'rel-type', label: 'role a', options: { hideArrow: false, hideLabel: false }, to: 'ent-type' },
      { arrows: { to: { enabled: true } }, from: 'rel-type', label: 'role b', options: { hideArrow: false, hideLabel: false }, to: 'attr-type' },
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
