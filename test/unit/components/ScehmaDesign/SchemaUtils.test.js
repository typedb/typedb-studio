import { computeAttributes, computeRoles } from '@/components/SchemaDesign/SchemaUtils.js';
import { getMockedEntityType, getMockedAttributeType, getMockedTransaction, getMockedRole } from '../../../helpers/mockedConcepts';


jest.mock('@/components/shared/PersistentStorage', () => ({
}));

describe('Schema Utils', () => {
  test('Compute Attributes', async () => {
    const attributeType = getMockedAttributeType({ isRemote: false });
    const entityType = getMockedEntityType({
      isRemote: false,
      customFuncs: {
        attributes: () => Promise.resolve({ collect: () => Promise.resolve([attributeType]) }),
      },
    });

    const graknTx = getMockedTransaction([], {
      getSchemaConcept: () => Promise.resolve(entityType),
    });

    const nodes = await computeAttributes([entityType], graknTx);
    expect(nodes[0].attributes[0].type).toBe('attribute-type');
    expect(nodes[0].attributes[0].dataType).toBe('String');
  });

  test('Compute Roles', async () => {
    const entityType = getMockedEntityType({
      isRemote: false,
      customFuncs: {
        playing: () => Promise.resolve({ collect: () => Promise.resolve([getMockedRole({ isRemote: true })]) }),
      },
    });

    const graknTx = getMockedTransaction([], {
      getSchemaConcept: () => Promise.resolve(entityType),
    });

    const nodes = await computeRoles([entityType], graknTx);

    expect(nodes[0].roles[0]).toBe('role');
  });
});
