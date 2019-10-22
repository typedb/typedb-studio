import { computeAttributes, computeRoles } from '@/components/SchemaDesign/SchemaUtils.js';
import MockConcepts from '../../../helpers/MockConcepts';
import { mockedEntityType, mockedAttributeType, getMockedAnswer, getMockedGraknTx } from '../../../helpers/mockedConcepts';


jest.mock('@/components/shared/PersistentStorage', () => ({
}));

describe('Schema Utils', () => {
  test('Compute Attributes', async () => {
    const entityType = { ...mockedEntityType };
    const attributeType = { ...mockedAttributeType };
    entityType.attributes = () => Promise.resolve({ collect: () => Promise.resolve([attributeType]) });


    const answer = getMockedAnswer([attributeType]);
    const graknTx = getMockedGraknTx([answer]);

    const nodes = await computeAttributes([entityType], graknTx);
    expect(nodes[0].attributes[0].type).toBe('name');
    expect(nodes[0].attributes[0].dataType).toBe('String');
  });

  test('Compute Roles', async () => {
    const nodes = await computeRoles([MockConcepts.getMockEntityType()]);
    expect(nodes[0].roles[0]).toBe('child');
  });
});
