import assert from 'assert';
import { startApp, stopApp } from './helpers/hooks';
import { selectKeyspace, clearEditor, waitForNotificationToDisappear, waitForNotificationWithMsgToDisappear } from './helpers/actions';
import { waitUntil, loadKeyspace, deleteKeyspace } from './helpers/utils';

jest.setTimeout(100000);

const addFavouriteQuery = async (queryName, query, expectedNotification, app) => {
  await clearEditor('.top-bar-container .CodeMirror', app);
  await app.client.click('.top-bar-container .CodeMirror');
  await app.client.keys(query);
  await app.client.click('.add-fav-query-btn');
  await app.client.click('.query-name-input');
  await app.client.keys(queryName);
  await app.client.click('.save-query-btn');
  await waitForNotificationWithMsgToDisappear(expectedNotification, app);
  await clearEditor('.top-bar-container .CodeMirror', app);
};

const deleteAllFavouriteQueries = async (app) => {
  await app.client.click('.fav-queries-container-btn');

  const existsNoFavQueries = await app.client.isExisting('.tooltip-container');
  if (existsNoFavQueries) {
    await app.client.click('#graph-div');
    return;
  }

  /* eslint-disable no-await-in-loop */
  while (await app.client.isExisting('.delete-fav-query-btn')) {
    await app.client.click('.delete-fav-query-btn');
    await waitForNotificationToDisappear(app);
  }
};

let app;

beforeAll(async () => {
  await loadKeyspace('gene');
});

beforeEach(async () => {
  app = await startApp();
  const isAppVisible = await app.browserWindow.isVisible();
  assert.equal(isAppVisible, true);
  await selectKeyspace('gene', app);
  await deleteAllFavouriteQueries(app);
});

afterEach(async () => {
  await stopApp(app);
});

afterAll(async () => {
  await deleteKeyspace('gene');
});

describe('Favourite queries', () => {
  test('adding a new favourite query wihtout a name fails', async () => {
    await app.client.click('.top-bar-container .CodeMirror');
    await app.client.keys('match $x isa person; get;');
    await app.client.click('.add-fav-query-btn');
    await app.client.click('.save-query-btn');
    await assert.doesNotReject(
      async () => waitUntil(async () => app.client.isExisting('.fav-query-name-tooltip')),
      undefined, 'tooltip did not appear',
    );
    assert.equal(await app.client.getText('.fav-query-name-tooltip'), 'Please write a query name');
  });

  test('adding a new favourite query works', async () => {
    await addFavouriteQuery('get persons', 'match $x isa person; get;', 'New query saved!\nCLOSE', app);
  });

  test('adding a favourite query with an existing name fails', async () => {
    await addFavouriteQuery('get persons', 'match $x isa person; get;', 'New query saved!\nCLOSE', app);
    await addFavouriteQuery('get persons', 'match $x isa person; get;', 'Query name already saved. Please choose a different name.\nCLOSE', app);
  });

  test('running a favourite query works', async () => {
    await addFavouriteQuery('get persons', 'match $x isa person; get; offset 0; limit 30;', 'New query saved!\nCLOSE', app);
    await app.client.click('.fav-queries-container-btn');
    await app.client.click('.run-fav-query-btn');
    assert.equal((await app.client.getText('.top-bar-container .CodeMirror'))[0].trim(), 'match $x isa person; get; offset 0; limit 30;');
  });

  test('editing a favourite query works', async () => {
    await addFavouriteQuery('get persons', 'match $x isa person; get; offset 0; limit 1;', 'New query saved!\nCLOSE', app);
    await app.client.click('.fav-queries-container-btn');
    await app.client.click('.edit-fav-query-btn');
    await clearEditor('.fav-query-item .CodeMirror', app);
    await app.client.keys('match $x isa person; get; offset 0; limit 2;');
    await app.client.click('.save-edited-fav-query');
    await app.client.click('.run-fav-query-btn');
    assert.equal((await app.client.getText('.top-bar-container .CodeMirror'))[0].trim(), 'match $x isa person; get; offset 0; limit 2;');
  });

  test('deleting a favourite query works', async () => {
    await selectKeyspace('gene', app);
    await deleteAllFavouriteQueries(app);
    await addFavouriteQuery('get persons', 'match $x isa person; get; offset 0; limit 1;', 'New query saved!\nCLOSE', app);
    await app.client.click('.fav-queries-container-btn');
    await app.client.click('.delete-fav-query-btn');
    await waitForNotificationWithMsgToDisappear('Query get persons has been deleted from saved queries.\nCLOSE', app);
  });

  // test('saving a miltiline query as a favourite query retains line breaks', () => {
  //   assert.equal(true, true);
  // });

  // test('running a miltiline favourite query shows up in main editor while retaining break lines', () => {
  //   assert.equal(true, true);
  // });
});
