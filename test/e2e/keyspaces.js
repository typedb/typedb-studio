import assert from 'assert';
import { waitUntil } from './helpers/utils';
import { startApp, stopApp } from './helpers/hooks';

jest.setTimeout(30000);

const openPreferencesPanel = async (app) => {
  await app.client.click('.toggle-preferences');
  await assert.doesNotReject(async () => waitUntil(async () => app.client.isExisting('.preferences-container')));
};

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

  test('creating a new keyspace works', async () => {
    openPreferencesPanel(app);
    // app.client.click('.toggle-preferences');

    // await sleep(1000);

    // await app.client.setValue('.keyspace-input', 'keyspace');
    // app.client.click('.new-keyspace-btn');

    // await sleep(10000);

    // const noOfKeyspace = (await app.client.getText('.keyspace-label')).length;
    // assert.equal(noOfKeyspace, 3);
  });

  // test('delete exisiting keyspace', async () => {
  //   app.client.click('.delete-keyspace-btn');

  //   await sleep(1000);

  //   app.client.click('.confirm');

  //   await sleep(10000);

  //   const noOfKeyspace = (await app.client.getText('.keyspace-label')).length;
  //   assert.equal(noOfKeyspace, 2);
  // });
});
