import assert from 'assert';
import { startApp, stopApp } from './helpers/hooks';
import { selectKeyspace, loadKeyspace, cleanKeyspace, waitForQueryCompletion, clearInput } from './helpers/actions';
import { waitUntil } from './helpers/utils';

jest.setTimeout(100000);

describe('Query Settings', () => {
  let app;

  beforeAll(() => {
    loadKeyspace('gene');
  });

  beforeEach(async () => {
    app = await startApp();
    const isAppVisible = await app.browserWindow.isVisible();
    assert.equal(isAppVisible, true);
    await selectKeyspace('gene', app);

    // open settings
    await app.client.click('.settings-tab');
    assert.equal(await app.client.isExisting('.settings-tab-content'), true);

    // ensure Loading Roleplayers setting is enabled
    const isLoadingRoleplayersEnabled = await app.client.isSelected('.load-roleplayers-switch input');
    if (!isLoadingRoleplayersEnabled) {
      await app.client.click('.load-roleplayers-switch');
      assert.equal(await app.client.isSelected('.load-roleplayers-switch input'), true);
    }

    // set the Query Limit setting to 1
    await clearInput('.neighbour-limit-input', app);
    await app.client.keys('1');
  });

  afterEach(async () => {
    await stopApp(app);
  });

  afterAll(async () => {
    await cleanKeyspace('gene');
  });


  test('Query Limit setting is applied', async () => {
    await clearInput('.query-limit-input', app);
    await app.client.keys('1');
    await app.client.click('.top-bar-container .CodeMirror');
    await app.client.keys('match $x isa person; get;');
    await app.client.click('.run-btn');
    await assert.doesNotReject(async () => waitUntil(async () => (await app.client.getText('.top-bar-container .CodeMirror')) === 'match $x isa person; get; offset 0; limit 1;'));
  });

  test('Neighbours Limit setting is applied', async () => {
    await clearInput('.neighbour-limit-input', app);
    await app.client.keys('2');
    await app.client.click('.top-bar-container .CodeMirror');
    await app.client.keys('match $x isa parentship; get;');
    await app.client.click('.run-btn');
    await waitForQueryCompletion(app);
    assert.equal(await app.client.getText('.no-of-entities'), 'entities: 2');
  });

  test('Load Neighbours enabled is applied', async () => {
    await app.client.click('.top-bar-container .CodeMirror');
    await app.client.keys('match $x isa parentship; get;');
    await app.client.click('.run-btn');
    await waitForQueryCompletion(app);

    assert.equal(await app.client.getText('.no-of-entities'), 'entities: 1');
  });

  test('Load Neighbours disabled is applied', async () => {
    await app.client.click('.load-roleplayers-switch');
    assert.equal(await app.client.isSelected('.load-roleplayers-switch input'), false);

    await app.client.click('.top-bar-container .CodeMirror');
    await app.client.keys('match $x isa parentship; get;');
    await app.client.click('.run-btn');
    await waitForQueryCompletion(app);

    assert.equal(await app.client.getText('.no-of-entities'), 'entities: 0');
  });
});
