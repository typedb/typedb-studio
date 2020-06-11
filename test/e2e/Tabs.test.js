import assert from 'assert';
import { startApp, stopApp } from './helpers/hooks';
import { selectKeyspace } from './helpers/actions';
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
    assert.equal(await app.client.isExisting('#tab-2'), true);
    const newTabClass = await app.client.getAttribute('#tab-2', 'class');
    assert.equal(newTabClass.indexOf('current-tab') > -1, true);
  });

  test('only tab is not closable', async () => {
    const closeButtons = (await app.client.elements('.close-tab-btn')).value;
    assert.equal(closeButtons.length, 0);
  });

  test('closing current tab sets first tab as active', async () => {
    await app.client.click('.new-tab-btn');
    await app.client.click('#tab-2 .close-tab-btn');
    const firstTabClass = await app.client.getAttribute('#tab-1', 'class');
    assert.equal(firstTabClass.indexOf('current-tab') > -1, true);
  });

  test('closing a tab retains the state of other tabs', async () => {
    await app.client.click('.new-tab-btn');
    await selectKeyspace('gene', app);
    await app.client.click('#tab-1 .close-tab-btn');
    assert.equal(await app.client.getText('.keyspaces'), 'gene');
  });

  test('rename a tab', async () => {
    const newTitle = 'new tab title';
    await app.client.doubleClick('#tab-1 .tab-title');
    await app.client.keys(newTitle);
    await app.client.click('#tab-1 .save-tab-rename-btn');
    const updatedTabTitle = await app.client.getText('#tab-1 .tab-title');
    const truncatedNewTtitle = 'new tab ti...';
    assert.equal(updatedTabTitle, truncatedNewTtitle);
  });

  test('cancel tab rename', async () => {
    const originalTabTitle = await app.client.getText('#tab-1 .tab-title');
    await app.client.doubleClick('#tab-1 .tab-title');
    await app.client.keys('new tab title');
    await app.client.click('#tab-1 .cancel-tab-rename-btn');
    const tabTitleAfterCancel = await app.client.getText('#tab-1 .tab-title');
    assert.equal(originalTabTitle, tabTitleAfterCancel);
  });
});
