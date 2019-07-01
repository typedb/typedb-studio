const Application = require('spectron').Application;
const assert = require('assert');
const electronPath = require('electron'); // Require Electron from the binaries included in node_modules.
const path = require('path');

const sleep = time => new Promise(r => setTimeout(r, time));
jest.setTimeout(100000);

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

describe('Load neighbours', () => {
  test('initialize workbase', async () => {
    const visible = await app.browserWindow.isVisible();
    assert.equal(visible, true);
  });

  test('select keyspace', async () => {
    await sleep(15000);

    app.client.click('.keyspaces');

    assert.equal(await app.client.getText('.keyspaces'), 'keyspace');

    await sleep(3000);

    app.client.click('#gene');

    await sleep(4000);

    assert.equal(await app.client.getText('.keyspaces'), 'gene');

    await sleep(1000);
  });

  test('double click on type', async () => {
    app.client.click('.CodeMirror');

    await sleep(1000);

    app.client.keys('match $x sub entity; get; offset 1; limit 1;');

    await sleep(1000);

    app.client.click('.run-btn');

    await sleep(1000);

    app.client.leftClick('#graph-div');
    app.client.leftClick('#graph-div');

    await sleep(4000);

    const noOfEntities = Number((await app.client.getText('.no-of-entities')).match(/\d+/)[0]);
    expect(noOfEntities).toBeGreaterThan(0);

    const noOfAttributes = await app.client.getText('.no-of-attributes');
    assert.equal(noOfAttributes, 'attributes: 0');
    const noOfRelationships = await app.client.getText('.no-of-relations');
    assert.equal(noOfRelationships, 'relations: 0');

    app.client.click('.clear-graph-btn');
    app.client.click('.clear-editor');

    await sleep(1000);
  });

  test('double click on attribute', async () => {
    app.client.click('.CodeMirror');

    await sleep(1000);

    app.client.keys('match $x isa age; get; limit 1;');

    await sleep(1000);

    app.client.click('.run-btn');

    await sleep(1000);

    app.client.leftClick('#graph-div');
    app.client.leftClick('#graph-div');

    await sleep(10000);

    const noOfEntities = Number((await app.client.getText('.no-of-entities')).match(/\d+/)[0]);
    expect(noOfEntities).toBeGreaterThan(0);
    const noOfAttributes = Number((await app.client.getText('.no-of-attributes')).match(/\d+/)[0]);
    expect(noOfAttributes).toBeGreaterThan(0);
    const noOfRelationships = await app.client.getText('.no-of-relations');
    assert.equal(noOfRelationships, 'relations: 0');

    app.client.click('.clear-graph-btn');
    app.client.click('.clear-editor');

    await sleep(1000);
  });

  test('double click on relation', async () => {
    app.client.click('.CodeMirror');

    await sleep(2000);

    app.client.keys('match $x isa parentship; get; limit 1;');

    await sleep(2000);

    app.client.click('.run-btn');

    await sleep(10000);

    app.client.leftClick('#graph-div');
    app.client.leftClick('#graph-div');

    await sleep(10000);

    const noOfEntities = Number((await app.client.getText('.no-of-entities')).match(/\d+/)[0]);
    expect(noOfEntities).toBeGreaterThan(0);
    const noOfAttributes = await app.client.getText('.no-of-attributes');
    assert.equal(noOfAttributes, 'attributes: 0');
    const noOfRelationships = Number((await app.client.getText('.no-of-relations')).match(/\d+/)[0]);
    expect(noOfRelationships).toBeGreaterThan(0);

    app.client.click('.clear-graph-btn');
    app.client.click('.clear-editor');

    await sleep(1000);
  });

  test('double click on entity', async () => {
    app.client.click('.CodeMirror');

    await sleep(1000);

    app.client.keys('match $x isa person; get; offset 2; limit 1;');

    await sleep(1000);

    app.client.click('.run-btn');

    await sleep(20000);

    app.client.leftClick('#graph-div');
    app.client.leftClick('#graph-div');

    await sleep(40000);

    const noOfEntities = Number((await app.client.getText('.no-of-entities')).match(/\d+/)[0]);
    console.log(noOfEntities);
    expect(noOfEntities).toBeGreaterThan(0);
    const noOfAttributes = await app.client.getText('.no-of-attributes');
    assert.equal(noOfAttributes, 'attributes: 0');
    console.log((await app.client.getText('.no-of-relations')));

    const noOfRelationships = Number((await app.client.getText('.no-of-relations')).match(/\d+/)[0]);

    console.log(noOfRelationships);

    expect(noOfRelationships).toBeGreaterThan(0);
  });
});
