import assert from 'assert';
import { startApp, stopApp } from './helpers/hooks';
import { selectKeyspace } from './helpers/actions';
import { waitUntil, deleteKeyspace, loadKeyspace } from './helpers/utils';

jest.setTimeout(100000);

let app;

beforeAll(async () => {
  await loadKeyspace('gene');
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

describe('Types Panel', () => {
  test('selecting a type autofills Graql Editor', async () => {
    await selectKeyspace('gene', app);
    await app.client.click('.types-container-btn');
    await assert.doesNotReject(async () => waitUntil(async () => app.client.isExisting('.select-type-btn')));
    await app.client.click('.select-type-btn=person');
    await assert.doesNotReject(async () => waitUntil(async () => (await app.client.getText('.top-bar-container .CodeMirror')) === 'match $x isa person; get;'));
  });
});
