import assert from 'assert';
import { waitUntil, loadKeyspace, deleteKeyspace } from './helpers/utils';
import { startApp, stopApp } from './helpers/hooks';
import { selectKeyspace, waitForQueryCompletion } from './helpers/actions';

jest.setTimeout(100000);

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

describe('Context Menu', () => {
  test('right clicking on an empty canvas does not open the context menu', async () => {
    await selectKeyspace('gene', app);
    await assert.doesNotReject(async () => waitUntil(async () => app.client.isExisting('#graph-div')), 'canvas is not present');
    await app.client.rightClick('#graph-div');
    await assert.doesNotReject(
      async () => waitUntil(async () => app.client.isExisting('#context-menu')),
      'context menu did not appear',
    );
    await assert.doesNotReject(
      async () => waitUntil(async () => (await app.client.getCssProperty('#context-menu', 'display')).value === 'none'),
      'context menu was displayed',
    );
  });

  test('right clicking on canvas (not a node) after running a query opens the context menu with disabled options', async () => {
    await selectKeyspace('gene', app);
    await app.client.click('.top-bar-container .CodeMirror');
    await app.client.keys('match $x isa person; get; limit 1;');
    await app.client.click('.run-btn');
    waitForQueryCompletion(app);
    await app.client.rightClick('#graph-div', 10, 10);
    await assert.doesNotReject(
      async () => waitUntil(async () => (await app.client.getCssProperty('#context-menu', 'display')).value !== 'none'),
      'context menu was not displayed',
    );
    assert.equal(await app.client.getText('#context-menu'), 'Hide\nExplain\nShortest Path');
    const deleteClasses = await app.client.getAttribute('.delete-nodes', 'class');
    assert.equal(deleteClasses.includes('disabled'), true);
    const explainClasses = await app.client.getAttribute('.explain-node', 'class');
    assert.equal(explainClasses.includes('disabled'), true);
    const shortestPathClasses = await app.client.getAttribute('.delete-nodes', 'class');
    assert.equal(shortestPathClasses.includes('disabled'), true);
  });

  // test('when visualisng schema concepts, right clicking does not open the context menu ', () => {
  //   assert.equal(true, true);
  // });

  // test('when only one node is selected, Compute Shortest Path remains hidden', () => {
  //   assert.equal(true, true);
  // });

  // test('when the selected node is not inferred, Explain remains hidden', () => {
  //   assert.equal(true, true);
  // });

  // test('Hide works for a single node selection', () => {
  //   assert.equal(true, true);
  // });

  // test('Hide works for a multi node selection', () => {
  //   assert.equal(true, true);
  // });

  // test('Explain works', () => {
  //   assert.equal(true, true);
  // });

  // test('Compute Shortest Path works', () => {
  //   assert.equal(true, true);
  // });
});
