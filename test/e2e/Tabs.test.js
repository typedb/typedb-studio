import assert from 'assert';
import { startApp, stopApp } from './helpers/hooks';
// import { selectKeyspace, runQuery, clickOnNode } from './helpers/actions';
// import { getNodePosition } from './helpers/canvas';
import { loadKeyspace, deleteKeyspace } from './helpers/utils';

jest.setTimeout(100000);

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

describe('Tabs', () => {
  test('add a new tab', async () => {
    await app.client.click('.new-tab-btn');
    const tabs = await app.client.elements('.tab');
    assert.equal(tabs.length, 2);
    // await selectKeyspace('gene', app);
    // await runQuery('match $x isa person; $r($x) isa parentship; get $x; offset 0; limit 1;', app);
    // const targetNodeCoordinates = await getNodePosition({ by: 'var', value: 'x' }, app);
    // await clickOnNode(targetNodeCoordinates, app);
    // await assert.doesNotReject(
    //   async () => waitUntil(async () => app.client.isExisting('.role-btn-text') && app.client.isExisting('.relation-item')),
    //   undefined, 'relation panel items did not appear',
    // );
  });
});
