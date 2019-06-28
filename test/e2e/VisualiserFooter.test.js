const Application = require('spectron').Application;
const assert = require('assert');
const electronPath = require('electron'); // Require Electron from the binaries included in node_modules.
const path = require('path');

jest.setTimeout(30000);

const app = new Application({
  path: electronPath,
  args: [path.join(__dirname, '../../dist/electron/main.js')],
});

beforeAll(async () => app.start());

afterAll(async () => {
  if (app && app.isRunning()) {
    return app.stop();
  }
  return undefined;
});

describe('Canvas Data', () => {
  test('initialize workbase', async () => {
    const visible = await app.browserWindow.isVisible();
    assert.equal(visible, true);
  });

  test('select keyspace', async () => {
    app.client.click('.keyspaces');

    assert.equal(await app.client.getText('.keyspaces'), 'keyspace');

    app.client.click('#gene');

    assert.equal(await app.client.getText('.keyspaces'), 'gene');
  });

  test('entity', async () => {
    await app.client.click('.CodeMirror');

    await app.client.keys('match $x isa person; get; limit 1;');

    await app.client.click('.run-btn');

    await app.client.waitUntil(async () => (await app.client.getText('.no-of-entities')) !== 'entities: 0', 25000, 'wait for canvas data to be updated');

    const noOfEntities = await app.client.getText('.no-of-entities');
    assert.equal(noOfEntities, 'entities: 1');

    await app.client.click('.clear-editor');
  });
  test('attribute', async () => {
    await app.client.click('.CodeMirror');

    await app.client.keys('match $x isa age; get; limit 1;');

    await app.client.click('.run-btn');

    await app.client.waitUntil(async () => (await app.client.getText('.no-of-attributes')) !== 'attributes: 0', 25000, 'wait for canvas data to be updated');

    const noOfAttributes = await app.client.getText('.no-of-attributes');
    assert.equal(noOfAttributes, 'attributes: 1');

    await app.client.click('.clear-editor');
  });
  test('relation', async () => {
    await app.client.click('.CodeMirror');

    await app.client.keys('match $x isa parentship; get; limit 1;');

    await app.client.click('.run-btn');

    await app.client.waitUntil(async () => (await app.client.getText('.no-of-relations')) !== 'relations: 0', 25000, 'wait for canvas data to be updated');

    const noOfRelations = await app.client.getText('.no-of-relations');
    assert.equal(noOfRelations, 'relations: 1');

    await app.client.click('.clear-editor');
  });
});
