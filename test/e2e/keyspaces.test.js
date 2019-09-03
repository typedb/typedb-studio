import assert from 'assert';
import { waitUntil, waitUntilNotificationDisapears } from './helpers/utils';
import { startApp, stopApp } from './helpers/hooks';

jest.setTimeout(100000);


const openPreferencesPanel = async (app) => {
  await assert.doesNotReject(async () => waitUntil(async () => app.client.isExisting('.toggle-preferences')));
  await app.client.click('.toggle-preferences');
  assert.equal(await app.client.isExisting('.preferences-container'), true);
};

const closePreferencesPanel = async (app) => {
  await app.client.click('.close-container');
};

const deleteAllKeyspaces = async (app) => {
  /* eslint-disable no-await-in-loop */
  while (await app.client.isExisting('.delete-keyspace-btn')) {
    await app.client.$('.delete-keyspace-btn').click();
    await waitUntil(async () => app.client.isExisting('.toasted.default'));
    await waitUntil(async () => app.client.isExisting('.toasted .confirm'));
    await app.client.click('.toasted .confirm');
    await waitUntilNotificationDisapears(app);
  }
};

const addKeyspace = async (keyspace, app) => {
  await app.client.click('.keyspace-input');
  await app.client.keys(keyspace);
  await app.client.click('.new-keyspace-btn');
  await waitUntilNotificationDisapears(app);
  assert.equal(await app.client.getText('.keyspace-label'), keyspace);
};

describe('Keyspaces', () => {
  let app;

  beforeEach(async () => {
    app = await startApp();
    const isAppVisible = await app.browserWindow.isVisible();
    assert.equal(isAppVisible, true);

    await openPreferencesPanel(app);
    await deleteAllKeyspaces(app);
    await closePreferencesPanel(app);
  });

  afterEach(async () => {
    await stopApp(app);
  });

  test('creating a new keyspace works', async () => {
    await openPreferencesPanel(app);
    await addKeyspace('test_keyspace', app);
    await closePreferencesPanel(app);
  });

  test('deleting a keyspace works', async () => {
    await openPreferencesPanel(app);
    await addKeyspace('test_keyspace', app);
    await app.client.click('.delete-keyspace-btn');
    await waitUntil(async () => app.client.isExisting('.toasted.default'));
    await waitUntil(async () => app.client.isExisting('.toasted .confirm'));
    await app.client.click('.toasted .confirm');
    await waitUntilNotificationDisapears(app);
    assert.equal(await app.client.isExisting('.keyspace-item'), false);
    await closePreferencesPanel(app);
  });
});
