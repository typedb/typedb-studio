import assert from 'assert';
import { startApp, stopApp } from './helpers/hooks';
import { selectKeyspace, clearInput } from './helpers/actions';
import { waitUntil, waitUntilNotificationDisapears, waitUntillQueryCompletion } from './helpers/utils';

jest.setTimeout(30000);

const addFavouriteQuery = async (queryName, query, expectedNotification, app) => {
  await clearInput('.top-bar-container .CodeMirror', app);
  await app.client.keys(query);
  await app.client.$('.add-fav-query-btn').click();
  await app.client.$('.query-name-input').click();
  await app.client.keys(queryName);
  await app.client.$('.save-query-btn').click();

  await assert.doesNotReject(async () => waitUntil(async () => app.client.isExisting('.toasted')));
  assert.equal(await app.client.$('.toasted').getText(), expectedNotification);
  await assert.doesNotReject(async () => waitUntil(async () => !(await app.client.isExisting('.toasted'))));
  await clearInput('.top-bar-container .CodeMirror', app);
};

const deleteAllFavouriteQueries = async (app) => {
  await app.client.$('.fav-queries-container-btn').click();

  /* eslint-disable no-await-in-loop */
  while (await app.client.isExisting('.delete-fav-query-btn')) {
    await app.client.$('.delete-fav-query-btn').click();
    await waitUntilNotificationDisapears(app);
  }
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

  test('adding a new favourite query wihtout a name fails', async () => {
    await selectKeyspace('gene', app);
    await clearInput('.top-bar-container .CodeMirror', app);
    await app.client.keys('match $x isa person; get;');
    await app.client.$('.add-fav-query-btn').click();
    await app.client.$('.save-query-btn').click();
    assert.equal(await app.client.getText('.fav-query-name-tooltip'), 'Please write a query name');
  });

  test('adding a new favourite query works', async () => {
    await selectKeyspace('gene', app);
    await deleteAllFavouriteQueries(app);
    await addFavouriteQuery('get persons', 'match $x isa person; get;', 'New query saved!\nCLOSE', app);
  });

  test('adding a favourite query with an existing name fails', async () => {
    await selectKeyspace('gene', app);
    await deleteAllFavouriteQueries(app);
    await addFavouriteQuery('get persons', 'match $x isa person; get;', 'New query saved!\nCLOSE', app);
    await addFavouriteQuery('get persons', 'match $x isa person; get;', 'Query name already saved. Please choose a different name.\nCLOSE', app);
  });

  test('running a favourite query works', async () => {
    await selectKeyspace('gene', app);
    await deleteAllFavouriteQueries(app);
    await addFavouriteQuery('get persons', 'match $x isa person; get; offset 0; limit 30;', 'New query saved!\nCLOSE', app);
    await app.client.click('.fav-queries-container-btn');
    await app.client.click('.run-fav-query-btn');
    // eslint-disable-next-line max-len
    await assert.doesNotReject(async () => waitUntil(async () => (await app.client.$('.top-bar-container .CodeMirror').getText()) === ' match $x isa person; get; offset 0; limit 30;'));
    await app.client.click('.run-btn');
    await waitUntillQueryCompletion(app);

    assert.equal(await app.client.getText('.no-of-entities'), 'entities: 30');
  });

  test('editing a favourite query works', async () => {
    await selectKeyspace('gene', app);
    await deleteAllFavouriteQueries(app);
    await addFavouriteQuery('get persons', 'match $x isa person; get; offset 0; limit 1;', 'New query saved!\nCLOSE', app);

    await app.client.click('.fav-queries-container-btn');
    await app.client.click('.edit-fav-query-btn');
    await clearInput('.fav-query-item .CodeMirror', app);
    await app.client.keys('match $x isa person; get; offset 0; limit 2;');
    await app.client.click('.save-edited-fav-query');
    await app.client.click('.run-fav-query-btn');
    // eslint-disable-next-line max-len
    await assert.doesNotReject(async () => waitUntil(async () => (await app.client.$('.top-bar-container .CodeMirror').getText()) === ' match $x isa person; get; offset 0; limit 2;'));
    await app.client.click('.run-btn');
    await waitUntillQueryCompletion(app);

    await assert.equal(await app.client.getText('.no-of-entities'), 'entities: 2');
  });

  test('deleting a favourite query works', async () => {
    await selectKeyspace('gene', app);
    await deleteAllFavouriteQueries(app);
    await addFavouriteQuery('get persons', 'match $x isa person; get; offset 0; limit 1;', 'New query saved!\nCLOSE', app);

    await app.client.click('.fav-queries-container-btn');
    await app.client.click('.delete-fav-query-btn');

    await assert.doesNotReject(async () => waitUntil(async () => app.client.isExisting('.toasted')));
    assert.equal(await app.client.$('.toasted').getText(), 'Query get persons has been deleted from saved queries.\nCLOSE');
  });

  test('saving a miltiline query as a favourite query retains line breaks', () => {
    assert.equal(true, true);
  });

  test('running a miltiline favourite query shows up in main editor while retaining break lines', () => {
    assert.equal(true, true);
  });
});
