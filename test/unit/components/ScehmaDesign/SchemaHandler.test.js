

import SchemaHandler from '@/components/SchemaDesign/SchemaHandler';
import ServerSettings from '@/components/ServerSettings';

import Grakn from 'grakn';

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


describe('Actions', () => {
  test('define entity type', async () => {
    let graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    const schemaHandler = new SchemaHandler(graknTx);
    await schemaHandler.defineEntityType({ entityLabel: 'person' });
    await graknTx.commit();

    graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    expect(await graknTx.getSchemaConcept('person')).toBeDefined();
  });

  test('define attribute type', async () => {
    let graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    const schemaHandler = new SchemaHandler(graknTx);

    await schemaHandler.defineAttributeType({ attributeLabel: 'name', dataType: 'string' });
    await graknTx.commit();

    graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    expect(await graknTx.getSchemaConcept('name')).toBeDefined();
  });

  test('define relationship type & role type', async () => {
    let graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    const schemaHandler = new SchemaHandler(graknTx);

    await schemaHandler.defineRelationshipType({ relationshipLabel: 'parentship' });
    await schemaHandler.defineRole({ roleLabel: 'parent' });
    await schemaHandler.addRelatesRole({ schemaLabel: 'parentship', roleLabel: 'parent' });

    await graknTx.commit();

    graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    expect(await graknTx.getSchemaConcept('parent')).toBeDefined();

    const parentshipRoles = await (await (await graknTx.getSchemaConcept('parentship')).roles()).collect();
    expect(parentshipRoles).toHaveLength(1);
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
  });

  test('add attribute', async () => {
    let graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    const schemaHandler = new SchemaHandler(graknTx);

    await schemaHandler.addAttribute({ schemaLabel: 'person', attributeLabel: 'name' });
    await graknTx.commit();

    graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    const personAttributes = await (await (await graknTx.getSchemaConcept('person')).attributes()).collect();
    expect(personAttributes).toHaveLength(1);
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
  });

  test('add plays role', async () => {
    let graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    const schemaHandler = new SchemaHandler(graknTx);

    await schemaHandler.addPlaysRole({ schemaLabel: 'person', roleLabel: 'parent' });
    await graknTx.commit();

    graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    const personRoles = await (await (await graknTx.getSchemaConcept('person')).playing()).collect();
    expect(personRoles).toHaveLength(1);
  });

  test('delete plays role', async () => {
    let graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    const schemaHandler = new SchemaHandler(graknTx);

    await schemaHandler.deletePlaysRole({ label: 'person', roleLabel: 'parent' });
    await graknTx.commit();

    graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    const personRoles = await (await (await graknTx.getSchemaConcept('person')).playing()).collect();
    expect(personRoles).toHaveLength(0);
  });

  test('delete relates role', async () => {
    let graknTx = await graknSession.transaction(Grakn.txType.WRITE);
    let schemaHandler = new SchemaHandler(graknTx);

    await schemaHandler.defineRelationshipType({ relationshipLabel: 'parentship' });
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
  });
});

