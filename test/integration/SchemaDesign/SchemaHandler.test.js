

import SchemaHandler from '@/components/SchemaDesign/SchemaHandler';
import ServerSettings from '@/components/ServerSettings';

import Grakn from 'grakn-client';

jest.mock('@/components/shared/PersistentStorage', () => ({
//   get: jest.fn(),
}));

jest.mock('@/components/ServerSettings', () => ({
  getServerHost: () => '127.0.0.1',
  getServerUri: () => '127.0.0.1:48555',
}));

jest.setTimeout(60000);

const grakn = new Grakn(ServerSettings.getServerUri(), null);
let graknSession;

beforeAll(async () => {
  graknSession = await grakn.session('testkeyspace2');
});

afterAll(() => {
  graknSession.close();
});

describe('Actions', () => {
  test('define entity type', async () => {
    let graknTx = await graknSession.transaction().write();
    const schemaHandler = new SchemaHandler(graknTx);
    await schemaHandler.defineEntityType({ entityLabel: 'person' });
    await graknTx.commit();

    graknTx = await graknSession.transaction().write();
    expect(await graknTx.getSchemaConcept('person')).toBeDefined();
    await graknTx.close();
  });

  test('define attribute type', async () => {
    let graknTx = await graknSession.transaction().write();
    const schemaHandler = new SchemaHandler(graknTx);

    await schemaHandler.defineAttributeType({ attributeLabel: 'name', dataType: 'string' });
    await graknTx.commit();

    graknTx = await graknSession.transaction().write();
    expect(await graknTx.getSchemaConcept('name')).toBeDefined();
    await graknTx.close();
  });

  test('define relation type & role type', async () => {
    let graknTx = await graknSession.transaction().write();
    try {
      const schemaHandler = new SchemaHandler(graknTx);

      await schemaHandler.defineRelationType({ relationLabel: 'parentship' });
      await schemaHandler.defineRole({ roleLabel: 'parent' });
      await schemaHandler.addRelatesRole({ schemaLabel: 'parentship', roleLabel: 'parent' });

      await graknTx.commit();

      graknTx = await graknSession.transaction().write();
      expect(await graknTx.getSchemaConcept('parent')).toBeDefined();

      const parentshipRoles = await (await (await graknTx.getSchemaConcept('parentship')).roles()).collect();
      expect(parentshipRoles).toHaveLength(1);
    } finally {
      await graknTx.close();
    }
  });

  test('delete type', async () => {
    let graknTx = await graknSession.transaction().write();
    let schemaHandler = new SchemaHandler(graknTx);

    await schemaHandler.defineEntityType({ entityLabel: 'delete-type' });
    await graknTx.commit();

    graknTx = await graknSession.transaction().write();
    expect(await graknTx.getSchemaConcept('delete-type')).toBeDefined();

    schemaHandler = new SchemaHandler(graknTx);

    await schemaHandler.deleteType({ label: 'delete-type' });
    await graknTx.commit();

    graknTx = await graknSession.transaction().write();
    expect(await graknTx.getSchemaConcept('delete-type')).toBe(null);
    await graknTx.close();
  });

  test('add attribute', async () => {
    let graknTx = await graknSession.transaction().write();
    const schemaHandler = new SchemaHandler(graknTx);

    await schemaHandler.addAttribute({ schemaLabel: 'person', attributeLabel: 'name' });
    await graknTx.commit();

    graknTx = await graknSession.transaction().write();
    const personAttributes = await (await (await graknTx.getSchemaConcept('person')).attributes()).collect();
    expect(personAttributes).toHaveLength(1);
    await graknTx.close();
  });

  test('delete attribute', async () => {
    let graknTx = await graknSession.transaction().write();
    let schemaHandler = new SchemaHandler(graknTx);

    await schemaHandler.addAttribute({ schemaLabel: 'person', attributeLabel: 'name' });
    await graknTx.commit();

    graknTx = await graknSession.transaction().write();
    let personAttributes = await (await (await graknTx.getSchemaConcept('person')).attributes()).collect();
    expect(personAttributes).toHaveLength(1);

    schemaHandler = new SchemaHandler(graknTx);
    await schemaHandler.deleteAttribute({ label: 'person', attributeLabel: 'name' });
    await graknTx.commit();

    graknTx = await graknSession.transaction().write();
    personAttributes = await (await (await graknTx.getSchemaConcept('person')).attributes()).collect();
    expect(personAttributes).toHaveLength(0);
    await graknTx.close();
  });

  test('add plays role', async () => {
    let graknTx = await graknSession.transaction().write();
    const schemaHandler = new SchemaHandler(graknTx);

    await schemaHandler.addPlaysRole({ schemaLabel: 'person', roleLabel: 'parent' });
    await graknTx.commit();

    graknTx = await graknSession.transaction().write();
    const personRoles = await (await (await graknTx.getSchemaConcept('person')).playing()).collect();
    expect(personRoles).toHaveLength(1);
    await graknTx.close();
  });

  test('delete plays role', async () => {
    let graknTx = await graknSession.transaction().write();
    const schemaHandler = new SchemaHandler(graknTx);

    await schemaHandler.deletePlaysRole({ label: 'person', roleLabel: 'parent' });
    await graknTx.commit();

    graknTx = await graknSession.transaction().write();
    const personRoles = await (await (await graknTx.getSchemaConcept('person')).playing()).collect();
    expect(personRoles).toHaveLength(0);
    await graknTx.close();
  });

  test('delete relates role', async () => {
    let graknTx = await graknSession.transaction().write();
    try {
      let schemaHandler = new SchemaHandler(graknTx);

      await schemaHandler.defineRelationType({ relationLabel: 'parentship' });
      await schemaHandler.defineRole({ roleLabel: 'parent' });
      await schemaHandler.addRelatesRole({ schemaLabel: 'parentship', roleLabel: 'parent' });
      await schemaHandler.defineRole({ roleLabel: 'child' });
      await schemaHandler.addRelatesRole({ schemaLabel: 'parentship', roleLabel: 'child' });

      await graknTx.commit();

      graknTx = await graknSession.transaction().write();

      const personRoles = await (await (await graknTx.getSchemaConcept('parentship')).roles()).collect();
      expect(personRoles).toHaveLength(2);

      schemaHandler = new SchemaHandler(graknTx);

      await schemaHandler.deleteRelatesRole({ label: 'parentship', roleLabel: 'parent' });
      await graknTx.commit();

      graknTx = await graknSession.transaction().write();
      const parentshipRoles = await (await (await graknTx.getSchemaConcept('parentship')).roles()).collect();
      expect(parentshipRoles).toHaveLength(1);
    } finally {
      await graknTx.close();
    }
  });
});

