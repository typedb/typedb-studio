import { computeAttributes, computeRoles } from '@/components/SchemaDesign/SchemaUtils.js';
import { mockedEntityType, mockedAttributeType, getMockedGraknTx } from '../../../helpers/mockedConcepts';


jest.mock('@/components/shared/PersistentStorage', () => ({
}));

describe('Schema Utils', () => {
  test('Compute Attributes', async () => {
    const entityType = { ...mockedEntityType };
    const attributeType = { ...mockedAttributeType };
    entityType.attributes = () => Promise.resolve({ collect: () => Promise.resolve([attributeType]) });


    const graknTx = getMockedGraknTx([], {
      getSchemaConcept: () => Promise.resolve(entityType),
    });

    const nodes = await computeAttributes([entityType], graknTx);
    expect(nodes[0].attributes[0].type).toBe('some-attribute-type');
    expect(nodes[0].attributes[0].dataType).toBe('String');
  });

  test('Compute Roles', async () => {
    const entityType = { ...mockedEntityType };

    const graknTx = getMockedGraknTx([], {
      getSchemaConcept: () => Promise.resolve(entityType),
    });

    const nodes = await computeRoles([entityType], graknTx);
    expect(nodes[0].roles[0]).toBe('some-role');
  });
});
