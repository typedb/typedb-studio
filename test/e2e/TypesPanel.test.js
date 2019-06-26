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
  if (app && app.isRunning()) {
    return app.stop();
  }
  return undefined;
});


describe('Types Panel', () => {
  test('initialize workbase', async () => {
    const visible = await app.browserWindow.isVisible();
    assert.equal(visible, true);
  });

  // test('select keyspace', async () => {
  //   app.client.click('.keyspaces');
  //   await app.client.waitUntilWindowLoaded();

  //   const keyspaceList = app.client.selectByAttribute('class', 'keyspaces-list');
  //   assert.ok(keyspaceList);

  //   assert.equal(await app.client.getText('.keyspaces'), 'keyspace');

  //   app.client.click('#gene');

  //   assert.equal(await app.client.getText('.keyspaces'), 'gene');
  // });

  // test('load person entities', async () => {
  //   app.client.click('#types-panel');
  //   await app.client.waitUntilWindowLoaded();
  //   const typesPanel = app.client.selectByAttribute('class', 'types-panel');

  //   assert.ok(typesPanel);
  //   await sleep(1000);

  //   app.client.click('#list-entities');
  //   await app.client.waitUntilWindowLoaded();
  //   const entitiesTab = await app.client.getHTML('#entities-tab');

  //   assert.ok(entitiesTab);
  //   await sleep(3000);

  //   let noOfNodes = await app.client.getText('#nodes');
  //   assert.equal(noOfNodes, 'nodes: 0');

  //   app.client.click('#person-btn');
  //   await sleep(1000);

  //   noOfNodes = await app.client.getText('#nodes');
  //   assert.equal(noOfNodes, 'nodes: 30');

  //   app.client.rightClick('#graph-div');
  //   await sleep(1000);
  //   app.client.click('#clear-graph');
  //   await sleep(1000);
  //   app.client.click('.confirm');
  //   await sleep(1000);

  //   noOfNodes = await app.client.getText('#nodes');
  //   assert.equal(noOfNodes, 'nodes: 0');
  // });

  // test('load age attributes', async () => {
  //   app.client.click('#types-panel');
  //   await app.client.waitUntilWindowLoaded();
  //   const typesPanel = app.client.selectByAttribute('class', 'types-panel');

  //   assert.ok(typesPanel);
  //   await sleep(1000);

  //   app.client.click('#list-attributes');
  //   await app.client.waitUntilWindowLoaded();
  //   const entitiesTab = await app.client.getHTML('#attributes-tab');

  //   assert.ok(entitiesTab);
  //   await sleep(3000);

  //   let noOfNodes = await app.client.getText('#nodes');
  //   assert.equal(noOfNodes, 'nodes: 0');

  //   app.client.click('#age-btn');
  //   await sleep(1000);

  //   noOfNodes = await app.client.getText('#nodes');
  //   assert.equal(noOfNodes, 'nodes: 18');

  //   app.client.rightClick('#graph-div');
  //   await sleep(1000);
  //   app.client.click('#clear-graph');
  //   await sleep(1000);
  //   app.client.click('.confirm');
  //   await sleep(1000);

  //   noOfNodes = await app.client.getText('#nodes');
  //   assert.equal(noOfNodes, 'nodes: 0');
  // });

  // test('load parentiship relations', async () => {
  //   app.client.click('#types-panel');
  //   await app.client.waitUntilWindowLoaded();
  //   const typesPanel = app.client.selectByAttribute('class', 'types-panel');

  //   assert.ok(typesPanel);
  //   await sleep(1000);

  //   app.client.click('#list-relations');
  //   await app.client.waitUntilWindowLoaded();
  //   const entitiesTab = await app.client.getHTML('#relations-tab');

  //   assert.ok(entitiesTab);
  //   await sleep(4000);

  //   let noOfNodes = await app.client.getText('#nodes');
  //   assert.equal(noOfNodes, 'nodes: 0');
  //   let noOfEdges = await app.client.getText('#edges');
  //   assert.equal(noOfEdges, 'edges: 0');

  //   await app.client.click('#marriage-btn');
  //   await sleep(6000);

  //   noOfNodes = await app.client.getText('#nodes');
  //   assert.equal(noOfNodes, 'nodes: 74');
  //   noOfEdges = await app.client.getText('#edges');
  //   assert.equal(noOfEdges, 'edges: 60');

  //   app.client.rightClick('#graph-div');
  //   await sleep(1000);
  //   app.client.click('#clear-graph');
  //   await sleep(1000);
  //   app.client.click('.confirm');
  //   await sleep(1000);

  //   noOfNodes = await app.client.getText('#nodes');
  //   assert.equal(noOfNodes, 'nodes: 0');
  //   noOfEdges = await app.client.getText('#edges');
  //   assert.equal(noOfEdges, 'edges: 0');
  // });
});
