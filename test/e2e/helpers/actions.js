import assert from 'assert';
import { waitUntil } from './utils';

// eslint-disable-next-line import/prefer-default-export
export async function selectKeyspace(keyspace, app) {
  await app.client.click('.keyspaces');
  await assert.doesNotReject(async () => waitUntil(async () => app.client.isExisting('.top-bar-container .keyspaces-list')), true);
  await assert.doesNotReject(async () => waitUntil(async () => app.client.isExisting(`#${keyspace}`)));
  await app.client.click(`#${keyspace}`);
  const selectedKeyspaceName = await app.client.getText('.keyspaces');
  assert.equal(selectedKeyspaceName, keyspace);
}
