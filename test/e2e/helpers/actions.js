import assert from 'assert';
import { waitUntil } from './utils';

// eslint-disable-next-line import/prefer-default-export
export async function selectKeyspace(keyspace, app) {
  await app.client.click('.keyspaces');

  const isKeyspacesListOpen = await waitUntil(async () => app.client.isExisting('.top-bar-container .keyspaces-list')).catch(result => result);
  assert.equal(isKeyspacesListOpen, true);

  const isKeyspaceListed = await waitUntil(async () => app.client.isExisting(`#${keyspace}`)).catch(result => result);
  assert.equal(isKeyspaceListed, true);

  await app.client.click(`#${keyspace}`);

  const selectedKeyspaceName = await app.client.getText('.keyspaces');
  assert.equal(selectedKeyspaceName, keyspace);
}
