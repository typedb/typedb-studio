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

describe('Relations Panel', () => {
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

  test('click on a node', async () => {
    await app.client.click('.CodeMirror');

    await app.client.keys('match $x isa person; get; limit 1;');

    await sleep(1000);

    await app.client.click('.run-btn');

    await sleep(10000);

    await app.client.click('#graph-div');

    await sleep(6000);

    expect((await app.client.getText('.role-btn-text')).length).toBeGreaterThan(0);
    expect((await app.client.getText('.relation-item')).length).toBeGreaterThan(0);
    expect(((await app.client.getText('.role-label'))[0]).length).toBeGreaterThan(0);
  });
});
