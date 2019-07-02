const Application = require('spectron').Application;
const assert = require('assert');
const electronPath = require('electron'); // Require Electron from the binaries included in node_modules.
const path = require('path');

const sleep = time => new Promise(r => setTimeout(r, time));
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

describe('Favourite queries', () => {
  test('initialize workbase', async () => {
    const visible = await app.browserWindow.isVisible();
    assert.equal(visible, true);
  });

  test('select keyspace', async () => {
    await app.client.waitUntil(async () => (await app.client.getAttribute('.keyspaces', 'class')) === 'btn keyspaces', 20000, 'timeout reached');

    app.client.click('.keyspaces');

    assert.equal(await app.client.getText('.keyspaces'), 'keyspace');

    app.client.click('#gene');

    await app.client.waitUntil(async () => (await app.client.getText('.keyspaces')) === 'gene', 20000, 'timeout reached');
  });

  test('add new favourite query', async () => {
    app.client.click('.CodeMirror');

    await sleep(1000);

    app.client.keys('match $x isa person; get;');

    app.client.click('.add-fav-query-btn');

    await app.client.waitUntil(async () => (await app.client.getAttribute('.save-query-btn', 'class')) === 'btn save-query-btn', 20000, 'timeout reached');

    app.client.click('.save-query-btn');

    await app.client.waitUntil(async () => (await app.client.getText('.fav-query-name-tooltip')) !== '', 20000, 'timeout reached');

    assert.equal(await app.client.getText('.fav-query-name-tooltip'), 'Please write a query name');

    app.client.click('.query-name-input');

    await sleep(1000);

    app.client.keys('get persons');

    app.client.click('.save-query-btn');

    await app.client.waitUntil(async () => (await app.client.getText('.toasted')) !== '', 20000, 'timeout reached');

    assert.equal(await app.client.getText('.toasted'), 'New query saved!\nCLOSE');

    await sleep(1000);

    app.client.click('.close-add-fav-query-container');
    app.client.click('.action');
  });

  test('add existing favourite query', async () => {
    app.client.click('.add-fav-query-btn');

    await app.client.waitUntil(async () => (await app.client.getAttribute('.query-name-input', 'class')) === 'input query-name-input', 20000, 'timeout reached');

    app.client.click('.query-name-input');

    await sleep(1000);

    app.client.keys('get persons');

    app.client.click('.save-query-btn');

    await app.client.waitUntil(async () => (await app.client.getText('.toasted')) !== '', 20000, 'timeout reached');

    assert.equal(await app.client.getText('.toasted'), 'Query name already saved. Please choose a different name.\nCLOSE');
  });


  test('run favourite query', async () => {
    app.client.click('.fav-queries-container-btn');

    await app.client.waitUntil(async () => (await app.client.getAttribute('.run-fav-query-btn', 'class')) === 'btn run-fav-query-btn', 20000, 'timeout reached');

    app.client.click('.run-fav-query-btn');

    app.client.click('.run-btn');

    await app.client.waitUntil(async () => (await app.client.getText('.no-of-entities')) !== 'entities: 0', 20000, 'timeout reached');

    assert.equal(await app.client.getText('.no-of-entities'), 'entities: 30');

    await app.client.click('.clear-graph-btn');
  });

  test('edit favourite query', async () => {
    await app.client.click('.fav-queries-container-btn');

    await app.client.click('.edit-fav-query-btn');

    await app.client.click('.CodeMirror-focused');

    await app.client.keys(['ArrowLeft', 'ArrowLeft', 'ArrowLeft', 'ArrowLeft', 'limit 1; ']);

    await app.client.click('.save-edited-fav-query');

    await app.client.click('.action');

    await app.client.click('.run-fav-query-btn');

    const noOfEntities = await app.client.getText('.no-of-entities');

    await assert.equal(noOfEntities, 'entities: 0');

    await assert.equal((await app.client.getText('.CodeMirror'))[0], ' match $x isa person; limit 1; get;');
    await assert.equal((await app.client.getText('.CodeMirror'))[1], 'match $x isa person; limit 1; get;');
  });

  test('delete favourite query', async () => {
    await app.client.click('.delete-fav-query-btn');

    await app.client.waitUntil(async () => (await app.client.getText('.toasted')).length === 1, 20000, 'timeout reached');

    await assert.equal(await app.client.getText('.toasted'), 'Query get persons has been deleted from saved queries.\nCLOSE');
  });
});
