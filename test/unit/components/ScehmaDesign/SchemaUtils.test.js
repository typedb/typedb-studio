import { computeAttributes, computeRoles } from '@/components/SchemaDesign/SchemaUtils.js';
import MockConcepts from '../../../helpers/MockConcepts';


jest.mock('@/components/shared/PersistentStorage', () => ({
}));

describe('Schema Utils', () => {
  test('Compute Attributes', async () => {
    const nodes = await computeAttributes([MockConcepts.getMockEntityType()]);
    expect(nodes[0].attributes[0].type).toBe('name');
    expect(nodes[0].attributes[0].dataType).toBe('String');
  });

  test('Compute Roles', async () => {
    const nodes = await computeRoles([MockConcepts.getMockEntityType()]);
    expect(nodes[0].roles[0]).toBe('child');
  });
});
