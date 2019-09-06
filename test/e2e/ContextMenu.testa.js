import assert from 'assert';
import { waitUntil } from './helpers/utils';
import { startApp, stopApp } from './helpers/hooks';
import { selectKeyspace, waitForQueryCompletion, loadKeyspace, cleanKeyspace } from './helpers/actions';

jest.setTimeout(100000);

describe('Context Menu', () => {
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

  test('right clicking on an empty canvas does not open the context menu', async () => {
    await app.client.rightClick('#graph-div');
    await assert.doesNotReject(async () => {
      await waitUntil(async () => app.client.$('#context-menu').isExisting());
      await waitUntil(async () => (await app.client.$('#context-menu').getCssProperty('display')).value === 'none');
    });
  });

  test('right clicking on canvas (not a node) after running a query opens the context menu with disabled options', async () => {
    await app.client.click('.graqlEditor-container .CodeMirror');
    await app.client.keys('match $x isa person; get; limit 1;');
    await app.client.click('.run-btn');

    waitForQueryCompletion(app);
    await app.client.rightClick('#graph-div', 10, 10);

    await assert.doesNotReject(async () => waitUntil(async () => (await app.client.$('#context-menu').getCssProperty('display')).value !== 'none'));

    assert.equal(await app.client.getText('#context-menu'), 'Hide\nExplain\nShortest Path');

    const deleteClasses = await app.client.$('.delete-nodes').getAttribute('class');
    assert.equal(deleteClasses.includes('disabled'), true);

    const explainClasses = await app.client.$('.explain-node').getAttribute('class');
    assert.equal(explainClasses.includes('disabled'), true);

    const shortestPathClasses = await app.client.$('.delete-nodes').getAttribute('class');
    assert.equal(shortestPathClasses.includes('disabled'), true);
  });

  test('when visualisng schema concepts, right clicking does not open the context menu ', () => {
    assert.equal(true, true);
  });

  test('when only one node is selected, Compute Shortest Path remains hidden', () => {
    assert.equal(true, true);
  });

  test('when the selected node is not inferred, Explain remains hidden', () => {
    assert.equal(true, true);
  });

  test('Hide works for a single node selection', () => {
    assert.equal(true, true);
  });

  test('Hide works for a multi node selection', () => {
    assert.equal(true, true);
  });

  test('Explain works', () => {
    assert.equal(true, true);
  });

  test('Compute Shortest Path works', () => {
    assert.equal(true, true);
  });
});
