import assert from 'assert';
import { startApp, stopApp } from './helpers/hooks';
import { selectKeyspace, loadKeyspace, cleanKeyspace, waitForQueryCompletion, waitForNodeToStabalize } from './helpers/actions';
import { getNodePosition, getNodeId } from './helpers/canvas';
import { waitUntil } from './helpers/utils';

jest.setTimeout(100000);

describe('Relations Panel', () => {
  let app;

  beforeAll(() => {
    loadKeyspace('gene');
  });

  beforeEach(async () => {
    app = await startApp();
    const isAppVisible = await app.browserWindow.isVisible();
    assert.equal(isAppVisible, true);
    await selectKeyspace('gene', app);
  });

  afterEach(async () => {
    await stopApp(app);
  });

  afterAll(async () => {
    await cleanKeyspace('gene');
  });


  test('click on a node', async () => {
    await app.client.click('.top-bar-container .CodeMirror');
    await app.client.keys('match $x isa person; $r($x) isa parentship; get $x; offset 0; limit 1;');
    await app.client.click('.run-btn');
    await waitForQueryCompletion(app);

    const targetNodeId = await getNodeId({ by: 'var', value: 'x' }, app);
    await waitForNodeToStabalize(targetNodeId, app);
    const targetNodeCoordinates = await getNodePosition(targetNodeId, app);
    await app.client.rightClick('#graph-div', targetNodeCoordinates.x, targetNodeCoordinates.y);
    await app.client.click('#graph-div', targetNodeCoordinates.x, targetNodeCoordinates.y);
    await assert.doesNotReject(async () => waitUntil(async () => app.client.isExisting('.role-btn-text') && app.client.isExisting('.relation-item')));
  });
});
