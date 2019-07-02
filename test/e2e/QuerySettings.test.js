const Application = require('spectron').Application;
const assert = require('assert');
const electronPath = require('electron'); // Require Electron from the binaries included in node_modules.
const path = require('path');

const sleep = time => new Promise(r => setTimeout(r, time));
jest.setTimeout(15000);

const app = new Application({
  path: electronPath,
  args: [path.join(__dirname, '../../dist/electron/main.js')],
});

beforeAll(async () => app.start());

afterAll(async () => {
  await sleep(1000);

  app.client.click('.neighbour-limit-input');

  await sleep(1000);

  app.client.keys(['Backspace', 'Backspace', '20']);

  await sleep(1000);

  app.client.click('.query-limit-input');

  await sleep(1000);

  app.client.keys(['Backspace', 'Backspace', '30']);

  await sleep(1000);

  app.client.click('.load-roleplayers-switch');

  await sleep(1000);

  if (app && app.isRunning()) {
    return app.stop();
  }
  return undefined;
});

describe('Query Settings', () => {
  test('initialize workbase', async () => {
    const visible = await app.browserWindow.isVisible();
    assert.equal(visible, true);
  });

  test('select keyspace', async () => {
    app.client.click('.keyspaces');

    assert.equal(await app.client.getText('.keyspaces'), 'keyspace');

    app.client.click('#gene');

    await app.client.waitUntil(async () => (await app.client.getText('.keyspaces')) === 'gene', 20000, 'timeout reached');
  });

  test('set query limit', async () => {
    app.client.click('.settings-tab');

    await sleep(1000);

    app.client.click('.query-limit-input');

    await sleep(1000);

    app.client.keys(['Backspace', 'Backspace', '1']);

    await sleep(1000);

    app.client.click('.CodeMirror');

    await sleep(1000);

    app.client.keys('match $x isa person; get;');

    await sleep(1000);

    app.client.click('.run-btn');

    await app.client.waitUntil(async () => (await app.client.getText('.no-of-entities')) === 'entities: 1', 20000, 'timeout reached');

    app.client.click('.clear-graph-btn');
    app.client.click('.clear-editor');
  });

  test('set neighbours limit', async () => {
    await app.client.waitUntil(async () => (await app.client.getText('.no-of-entities')) === 'entities: 0', 20000, 'timeout reached');

    app.client.click('.neighbour-limit-input');

    await sleep(1000);

    app.client.keys(['Backspace', 'Backspace', '1']);

    await sleep(1000);

    app.client.click('.CodeMirror');

    await sleep(1000);

    app.client.keys('match $x isa parentship; get;');

    await sleep(1000);

    app.client.click('.run-btn');

    await app.client.waitUntil(async () => (await app.client.getText('.no-of-relations')) === 'relations: 1', 20000, 'timeout reached');

    assert.equal((await app.client.getText('.no-of-entities')), 'entities: 1');

    app.client.click('.clear-graph-btn');
    app.client.click('.clear-editor');
  });

  test('dont auto load neighbours', async () => {
    await app.client.waitUntil(async () => (await app.client.getText('.no-of-relations')) === 'relations: 0', 20000, 'timeout reached');

    app.client.click('.load-roleplayers-switch');

    await sleep(1000);

    app.client.click('.CodeMirror');

    await sleep(1000);

    app.client.keys('match $x isa parentship; get;');

    await sleep(1000);

    app.client.click('.run-btn');

    await app.client.waitUntil(async () => (await app.client.getText('.no-of-relations')) === 'relations: 1', 20000, 'timeout reached');

    assert.equal((await app.client.getText('.no-of-entities')), 'entities: 0');
  });
});
