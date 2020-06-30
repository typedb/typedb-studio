import assert from 'assert';
import { waitUntil } from './utils';

export const selectKeyspace = async (keyspace, app) => {
  await assert.doesNotReject(
    async () => waitUntil(async () => app.client.isExisting('.keyspaces')),
    undefined, 'keyspaces menu did not appear',
  );
  await app.client.click('.keyspaces');
  await assert.doesNotReject(
    async () => waitUntil(async () => app.client.isExisting('.top-bar-container .keyspaces-list')),
    undefined, 'list of keyspaces did not appear',
  );
  await assert.doesNotReject(
    async () => waitUntil(async () => app.client.isExisting(`#${keyspace}`)),
    undefined, `the target keyspace does not exists: ${keyspace}`,
  );
  await app.client.click(`#${keyspace}`);
  await assert.doesNotReject(
    async () => waitUntil(async () => (await app.client.getText('.keyspaces')) === keyspace),
    undefined, `the target keyspace was not selected: ${keyspace}`,
  );
};

export const clearEditor = async (selector, app) => {
  await app.client.click(selector);
  await app.client.click(selector);
  await app.client.click(selector);

  await app.client.keys(['Backspace']);
  const editorText = await app.client.getText(selector);
  assert.equal(editorText === '' || editorText === ' ', true);
};

export const waitForQueryCompletion = async (app) => {
  await assert.doesNotReject(
    async () => waitUntil(async () => app.client.isExisting('.bp3-spinner-animation')),
    undefined, 'query execution did not start',
  );
  await assert.doesNotReject(
    async () => waitUntil(async () => !(await app.client.isExisting('.bp3-spinner-animation'))),
    undefined, 'query execution did not finish',
  );
};

export const waitForNotificationToDisappear = async (app) => {
  await assert.doesNotReject(
    async () => waitUntil(async () => app.client.isExisting('.toasted.info')),
    undefined, 'notification did not appear',
  );
  await assert.doesNotReject(
    async () => waitUntil(async () => !(await app.client.isExisting('.toasted.info'))),
    undefined, 'notification did not disappear',
  );
};

export const waitForNotificationWithMsgToDisappear = async (expectedMsg, app) => {
  await assert.doesNotReject(
    async () => waitUntil(async () => app.client.isExisting('.toasted.info')),
    undefined, 'notification did not appear',
  );

  await assert.doesNotReject(
    async () => waitUntil(async () => (await app.client.getText('.toasted.info')) === expectedMsg),
    undefined, `notification message was not what was expected: ${expectedMsg}`,
  );

  await assert.doesNotReject(
    async () => waitUntil(async () => !(await app.client.isExisting('.toasted.info'))),
    undefined, 'notification did not disappear',
  );
};

export const doubleClickOnNode = async (coordinates, app) => {
  await app.client.click('#graph-div', coordinates.x, coordinates.y);
  await app.client.click('#graph-div', coordinates.x, coordinates.y);
};

export const clickOnNode = async (coordinates, app) => {
  await app.client.click('#graph-div', coordinates.x, coordinates.y);
};

export const confirmAction = async (app) => {
  await assert.doesNotReject(
    async () => waitUntil(async () => app.client.isExisting('.toasted.default')),
    undefined, 'confirmation box did not appear',
  );

  await assert.doesNotReject(
    async () => waitUntil(async () => app.client.isExisting('.toasted .confirm')),
    undefined, 'confirmation button did not appear',
  );

  await app.client.click('.toasted .confirm');
  await waitForNotificationToDisappear(app);
};


export const runQuery = async (query, app) => {
  await app.client.click('.top-bar-container .CodeMirror');
  await app.client.keys(query);
  await app.client.click('.run-btn');
  await waitForQueryCompletion(app);
};

export const changeSetting = async (settings, app) => {
  await app.client.click('.settings-tab');
  assert.equal(await app.client.isExisting('.settings-tab-content'), true);

  // eslint-disable-next-line no-prototype-builtins
  if (settings.hasOwnProperty('loadRoleplayers')) {
    const currentValue = await app.client.isSelected('.load-roleplayers-switch input');
    if (settings.loadRoleplayers !== currentValue) {
      await app.client.click('.load-roleplayers-switch');
      assert.equal(await app.client.isSelected('.load-roleplayers-switch input'), settings.loadRoleplayers);
    }
  }

  // eslint-disable-next-line no-prototype-builtins
  if (settings.hasOwnProperty('queryLimit')) {
    await app.client.setValue('.query-limit-input', '');
    await app.client.keys(`${settings.queryLimit}`);
  }

  // eslint-disable-next-line no-prototype-builtins
  if (settings.hasOwnProperty('neighboursLimit')) {
    await app.client.setValue('.neighbour-limit-input', '');
    await app.client.keys(`${settings.neighboursLimit}`);
  }
};
