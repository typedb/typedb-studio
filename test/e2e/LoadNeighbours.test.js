import assert from 'assert';
import { startApp, stopApp } from './helpers/hooks';
import { selectKeyspace, waitForQueryCompletion, runQuery, doubleClickOnNode } from './helpers/actions';
import { getNodePosition } from './helpers/canvas';
import { loadKeyspace, deleteKeyspace } from './helpers/utils';

jest.setTimeout(1000000);

let app;

beforeAll(async () => {
  await loadKeyspace('gene');
});


beforeEach(async () => {
  app = await startApp();
  const isAppVisible = await app.browserWindow.isVisible();
  assert.equal(isAppVisible, true);
});

afterEach(async () => {
  await stopApp(app);
});

afterAll(async () => {
  await deleteKeyspace('gene');
});

describe('Load Neighbours', () => {
  test('double clicking on an entity loads the relations it plays a role in', async () => {
    await selectKeyspace('gene', app);
    await runQuery('match $x isa person; get; offset 0; limit 1;', app);
    const targetNodeCoordinates = await getNodePosition({ by: 'var', value: 'x' }, app);
    await doubleClickOnNode(targetNodeCoordinates, app);
    await waitForQueryCompletion(app);
    const numOfEntities = Number((await app.client.getText('.no-of-entities')).match(/\d+/)[0]);
    assert.equal(numOfEntities > 1, true);
    const noOfRelationships = Number((await app.client.getText('.no-of-relations')).match(/\d+/)[0]);
    assert.equal(noOfRelationships > 0, true);
  });

  test('double clicking on an attribute instance loads its owners', async () => {
    await selectKeyspace('gene', app);
    await runQuery('match $x isa age; get; offset 0; limit 1;', app);
    const targetNodeCoordinates = await getNodePosition({ by: 'var', value: 'x' }, app);
    await doubleClickOnNode(targetNodeCoordinates, app);
    await waitForQueryCompletion(app);
    const noOfEntities = Number((await app.client.getText('.no-of-entities')).match(/\d+/)[0]);
    assert.equal(noOfEntities > 0, true);
    const noOfRelationships = await app.client.getText('.no-of-relations');
    assert.equal(noOfRelationships, 'relations: 0');
  });

  // test('double clicking on a relation instance loads its roleplayers', async () => {
  //   // TODO:  turn off load role players here
  //   await app.client.click('.top-bar-container .CodeMirror');
  //   await app.client.keys('match $x isa parentship; get; offset 0; limit 1;');
  //   await app.client.click('.run-btn');
  //   await waitForQueryCompletion(app);

  //   const targetNodeId = await getNodeId({ by: 'var', value: 'x' }, app);
  //   await waitForNodeToStabalize(targetNodeId, app);
  //   const targetNodeCoordinates = await getNodePosition(targetNodeId, app);
  //   await app.client.rightClick('#graph-div', targetNodeCoordinates.x, targetNodeCoordinates.y);
  //   await doubleClick('#graph-div', targetNodeCoordinates, app);
  //   await waitForQueryCompletion(app);

  //   const noOfEntities = Number((await app.client.getText('.no-of-entities')).match(/\d+/)[0]);
  //   expect(noOfEntities).toBeGreaterThan(0);

  //   const noOfRelationships = Number((await app.client.getText('.no-of-relations')).match(/\d+/)[0]);
  //   expect(noOfRelationships).toBeGreaterThan(0);
  // });

  test('double clicking on a type loads its instances', async () => {
    await selectKeyspace('gene', app);
    await runQuery('match $x sub person; get; offset 0; limit 1;', app);
    const targetNodeCoordinates = await getNodePosition({ by: 'var', value: 'x' }, app);
    await doubleClickOnNode(targetNodeCoordinates, app);
    await waitForQueryCompletion(app);
    const noOfEntities = Number((await app.client.getText('.no-of-entities')).match(/\d+/)[0]);
    assert.equal(noOfEntities > 0, true);
  });
});
