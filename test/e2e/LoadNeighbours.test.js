import assert from 'assert';
import { startApp, stopApp } from './helpers/hooks';
import { selectKeyspace, waitForQueryCompletion, runQuery, doubleClickOnNode, changeSetting } from './helpers/actions';
import { getNodePosition } from './helpers/canvas';
import { loadKeyspace, deleteKeyspace } from './helpers/utils';

jest.setTimeout(1000000);

let app;

beforeAll(() => {
  loadKeyspace('gene');
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
    await changeSetting({ neighboursLimit: 1 }, app);
    await runQuery('match $x isa person; get; offset 0; limit 1;', app);
    const targetNodeCoordinates = await getNodePosition({ by: 'var', value: 'x' }, app);
    await doubleClickOnNode(targetNodeCoordinates, app);
    await waitForQueryCompletion(app);

    const noOfRelations = Number((await app.client.getText('.no-of-relations')).match(/\d+/)[0]);
    assert.equal(noOfRelations, 1);
  });

  test('double clicking on an attribute instance loads its owners', async () => {
    await selectKeyspace('gene', app);
    await changeSetting({ neighboursLimit: 1 }, app);
    await runQuery('match $x isa age; get; offset 0; limit 1;', app);
    const targetNodeCoordinates = await getNodePosition({ by: 'var', value: 'x' }, app);
    await doubleClickOnNode(targetNodeCoordinates, app);
    await waitForQueryCompletion(app);

    const noOfEntities = Number((await app.client.getText('.no-of-entities')).match(/\d+/)[0]);
    const noOfRelations = Number((await app.client.getText('.no-of-relations')).match(/\d+/)[0]);
    const noOfAttributes = Number((await app.client.getText('.no-of-attributes')).match(/\d+/)[0]);
    assert.equal(noOfEntities + noOfRelations + noOfAttributes, 2);
  });

  test('double clicking on a relation instance loads its roleplayers', async () => {
    await selectKeyspace('gene', app);
    await changeSetting({ loadRoleplayers: false, neighboursLimit: 1 }, app);
    await runQuery('match $x isa parentship; get; offset 0; limit 1;', app);
    const targetNodeCoordinates = await getNodePosition({ by: 'var', value: 'x' }, app);
    await doubleClickOnNode(targetNodeCoordinates, app);
    await waitForQueryCompletion(app);

    const noOfEntities = Number((await app.client.getText('.no-of-entities')).match(/\d+/)[0]);
    const noOfRelations = Number((await app.client.getText('.no-of-relations')).match(/\d+/)[0]);
    const noOfAttributes = Number((await app.client.getText('.no-of-attributes')).match(/\d+/)[0]);
    assert.equal(noOfEntities + noOfRelations + noOfAttributes, 2);
  });

  test('double clicking on a type loads its instances', async () => {
    await selectKeyspace('gene', app);
    await changeSetting({ neighboursLimit: 1 }, app);
    await runQuery('match $x sub person; get; offset 0; limit 1;', app);
    const targetNodeCoordinates = await getNodePosition({ by: 'var', value: 'x' }, app);
    await doubleClickOnNode(targetNodeCoordinates, app);
    await waitForQueryCompletion(app);

    const noOfEntities = Number((await app.client.getText('.no-of-entities')).match(/\d+/)[0]);
    assert.equal(noOfEntities, 1);
  });
});
