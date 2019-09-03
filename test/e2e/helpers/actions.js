import GraknClient from 'grakn-client';
import assert from 'assert';
import { execSync } from 'child_process';
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

export const waitForQueryCompletion = async (app) => {
  await assert.doesNotReject(async () => waitUntil(async () => app.client.isExisting('.bp3-spinner-animation')));
  await assert.doesNotReject(async () => waitUntil(async () => !(await app.client.isExisting('.bp3-spinner-animation'))));
};

export const waitForNotificationToDisapear = async (app) => {
  await assert.doesNotReject(async () => waitUntil(async () => app.client.isExisting('.toasted.info')));
  await assert.doesNotReject(async () => waitUntil(async () => !(await app.client.isExisting('.toasted.info'))));
};

export const loadKeyspace = (keyspaceName) => {
  execSync(`grakn console -k ${keyspaceName} -f $(pwd)/test/helpers/${keyspaceName}.gql`);
};

export const cleanKeyspace = async (keyspaceName) => {
  const client = new GraknClient('localhost:48555');
  await client.keyspaces().delete(keyspaceName);
};
