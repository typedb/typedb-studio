import assert from 'assert';
import { waitUntil } from './utils';

export const selectKeyspace = async (keyspace, app) => {
  await app.client.click('.keyspaces');
  await assert.doesNotReject(async () => waitUntil(async () => app.client.isExisting('.top-bar-container .keyspaces-list')), true);
  await assert.doesNotReject(async () => waitUntil(async () => app.client.isExisting(`#${keyspace}`)));
  await app.client.click(`#${keyspace}`);
  await assert.doesNotReject(async () => waitUntil(async () => (await app.client.$('.keyspaces').getText()) === keyspace));
};

export const clearInput = async (selector, app) => {
  await app.client.click(selector);
  await app.client.click(selector);
  await app.client.click(selector);

  await app.client.keys(['Backspace']);
  await assert.doesNotReject(async () => waitUntil(async () => (await app.client.getText(selector)) === ''));
};
