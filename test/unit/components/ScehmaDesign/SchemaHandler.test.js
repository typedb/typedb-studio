

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

jest.setTimeout(30000);

const grakn = new Grakn(ServerSettings.getServerUri(), null);
let graknSession;

beforeAll(() => {
  graknSession = grakn.session('testkeyspace2');
});

afterAll(() => {
  graknSession.close();
});

describe('Actions', () => {
  test('define entity type', async () => {
    let graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    const schemaHandler = new SchemaHandler(graknTx);
    await schemaHandler.defineEntityType({ entityLabel: 'person' });
    await graknTx.commit();

    graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    expect(await graknTx.getSchemaConcept('person')).toBeDefined();
    await graknTx.close();
  });

  test('define attribute type', async () => {
    let graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    const schemaHandler = new SchemaHandler(graknTx);

    await schemaHandler.defineAttributeType({ attributeLabel: 'name', dataType: 'string' });
    await graknTx.commit();

    graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    expect(await graknTx.getSchemaConcept('name')).toBeDefined();
    await graknTx.close();
  });

  test('define relation type & role type', async () => {
    let graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    const schemaHandler = new SchemaHandler(graknTx);

    await schemaHandler.defineRelationType({ relationLabel: 'parentship' });
    await schemaHandler.defineRole({ roleLabel: 'parent' });
    await schemaHandler.addRelatesRole({ schemaLabel: 'parentship', roleLabel: 'parent' });

    await graknTx.commit();

    graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    expect(await graknTx.getSchemaConcept('parent')).toBeDefined();

    const parentshipRoles = await (await (await graknTx.getSchemaConcept('parentship')).roles()).collect();
    expect(parentshipRoles).toHaveLength(1);
    await graknTx.close();
  });

  test('delete type', async () => {
    let graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    let schemaHandler = new SchemaHandler(graknTx);

    await schemaHandler.defineEntityType({ entityLabel: 'delete-type' });
    await graknTx.commit();

    graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    expect(await graknTx.getSchemaConcept('delete-type')).toBeDefined();

    schemaHandler = new SchemaHandler(graknTx);

    await schemaHandler.deleteType({ label: 'delete-type' });
    await graknTx.commit();

    graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    expect(await graknTx.getSchemaConcept('delete-type')).toBe(null);
    await graknTx.close();
  });

  test('add attribute', async () => {
    let graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    const schemaHandler = new SchemaHandler(graknTx);

    await schemaHandler.addAttribute({ schemaLabel: 'person', attributeLabel: 'name' });
    await graknTx.commit();

    graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    const personAttributes = await (await (await graknTx.getSchemaConcept('person')).attributes()).collect();
    expect(personAttributes).toHaveLength(1);
    await graknTx.close();
  });

  test('delete attribute', async () => {
    let graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    let schemaHandler = new SchemaHandler(graknTx);

    await schemaHandler.addAttribute({ schemaLabel: 'person', attributeLabel: 'name' });
    await graknTx.commit();

    graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    let personAttributes = await (await (await graknTx.getSchemaConcept('person')).attributes()).collect();
    expect(personAttributes).toHaveLength(1);

    schemaHandler = new SchemaHandler(graknTx);
    await schemaHandler.deleteAttribute({ label: 'person', attributeLabel: 'name' });
    await graknTx.commit();

    graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    personAttributes = await (await (await graknTx.getSchemaConcept('person')).attributes()).collect();
    expect(personAttributes).toHaveLength(0);
    await graknTx.close();
  });

  test('add plays role', async () => {
    let graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    const schemaHandler = new SchemaHandler(graknTx);

    await schemaHandler.addPlaysRole({ schemaLabel: 'person', roleLabel: 'parent' });
    await graknTx.commit();

    graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    const personRoles = await (await (await graknTx.getSchemaConcept('person')).playing()).collect();
    expect(personRoles).toHaveLength(1);
    await graknTx.close();
  });

  test('delete plays role', async () => {
    let graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    const schemaHandler = new SchemaHandler(graknTx);

    await schemaHandler.deletePlaysRole({ label: 'person', roleLabel: 'parent' });
    await graknTx.commit();

    graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    const personRoles = await (await (await graknTx.getSchemaConcept('person')).playing()).collect();
    expect(personRoles).toHaveLength(0);
    await graknTx.close();
  });

  test.skip('delete relates role', async () => {
    let graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    let schemaHandler = new SchemaHandler(graknTx);

    await schemaHandler.defineRelationType({ relationLabel: 'parentship' });
    await schemaHandler.defineRole({ roleLabel: 'parent' });
    await schemaHandler.addRelatesRole({ schemaLabel: 'parentship', roleLabel: 'parent' });

    await graknTx.commit();

    graknTx = await graknSession.transaction(Grakn.txType.WRITE);

    const personRoles = await (await (await graknTx.getSchemaConcept('parentship')).roles()).collect();
    expect(personRoles).toHaveLength(1);

    graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    schemaHandler = new SchemaHandler(graknTx);

    await schemaHandler.deleteRelatesRole({ label: 'parentship', roleLabel: 'parent' });
    await graknTx.commit();

    graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    const parentshipRoles = await (await (await graknTx.getSchemaConcept('parentship')).roles()).collect();
    expect(parentshipRoles).toHaveLength(0);
    await graknTx.close();
  });
});

