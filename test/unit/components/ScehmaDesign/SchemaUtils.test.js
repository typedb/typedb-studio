import { computeAttributes, computeRoles } from '@/components/SchemaDesign/SchemaUtils.js';
import { getMockedEntityType, getMockedAttributeType, getMockedTransaction, getMockedRole } from '../../../helpers/mockedConcepts';


jest.mock('@/components/shared/PersistentStorage', () => ({
}));

describe('Schema Utils', () => {
  test('Compute Attributes', async () => {
    const attributeType = getMockedAttributeType();
    const entityType = getMockedEntityType({
      extraProps: {
        remote: {
          attributes: () => Promise.resolve({ collect: () => Promise.resolve([attributeType.asRemote()]) }),
        },
      },
    });

    const graknTx = getMockedTransaction([], {
      getSchemaConcept: () => Promise.resolve(entityType.asRemote()),
    });

    const nodes = await computeAttributes([entityType], graknTx);
    expect(nodes[0].attributes[0].type).toBe('attribute-type');
    expect(nodes[0].attributes[0].dataType).toBe('string');
  });

  test('Compute Roles', async () => {
    const entityType = getMockedEntityType({
      extraProps: {
        remote: {
          playing: () => Promise.resolve({ collect: () => Promise.resolve([getMockedRole({ isRemote: true })]) }),
        },
      },
    });

    const graknTx = getMockedTransaction([], {
      getSchemaConcept: () => Promise.resolve(entityType.asRemote()),
    });

    const nodes = await computeRoles([entityType], graknTx);

    expect(nodes[0].roles[0]).toBe('role');
  });
});
