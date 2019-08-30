import assert from 'assert';
import { waitUntil, waitUntillQueryCompletion } from './helpers/utils';
import { startApp, stopApp } from './helpers/hooks';
import { selectKeyspace } from './helpers/actions';

jest.setTimeout(30000);

describe('Favourite queries', () => {
  let app;

  beforeEach(async () => {
    app = await startApp();
    const isAppVisible = await app.browserWindow.isVisible();
    assert.equal(isAppVisible, true);
  });

  afterEach(async () => {
    await stopApp(app);
  });

  test('right clicking on an empty canvas does not open the context menu', async () => {
    await selectKeyspace('gene', app);
    await app.client.rightClick('#graph-div');
    const isContextMenuHidden = await waitUntil(async () => (await app.client.$('#context-menu').getCssProperty('display')).value === 'none').catch(result => result);
    assert.equal(isContextMenuHidden, true);
  });

  test('right clicking on canvas (not a node) after running a query opens the context menu with disabled options', async () => {
    await selectKeyspace('gene', app);

    await app.client.click('.graqlEditor-container .CodeMirror');
    await app.client.keys('match $x isa person; get; limit 1;');
    await app.client.click('.run-btn');

    const hasLoadingFinished = await waitUntillQueryCompletion(app);
    assert.equal(hasLoadingFinished, true);

    await app.client.rightClick('#graph-div', 10, 10);

    const isContextMenuDisplayed = await waitUntil(async () => (await app.client.$('#context-menu').getCssProperty('display')).value !== 'none').catch(result => result);
    assert.equal(isContextMenuDisplayed, true);

    assert.equal(await app.client.getText('#context-menu'), 'Hide\nExplain\nShortest Path');

    const deleteClasses = await app.client.$('.delete-nodes').getAttribute('class');
    assert.equal(deleteClasses.includes('disabled'), true);

    const explainClasses = await app.client.$('.explain-node').getAttribute('class');
    assert.equal(explainClasses.includes('disabled'), true);

    const shortestPathClasses = await app.client.$('.delete-nodes').getAttribute('class');
    assert.equal(shortestPathClasses.includes('disabled'), true);
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
