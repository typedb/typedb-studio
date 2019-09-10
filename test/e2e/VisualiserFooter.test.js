import assert from 'assert';
import { startApp, stopApp } from './helpers/hooks';
import { selectKeyspace, runQuery } from './helpers/actions';
import { deleteKeyspace, loadKeyspace } from './helpers/utils';

jest.setTimeout(100000);

let app;

beforeAll(() => {
  loadKeyspace('gene');
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

describe('Canvas Data', () => {
  test('Footer shows correct number of entities', async () => {
    await selectKeyspace('gene', app);
    await runQuery('match $x isa person; get; offset 0; limit 1;', app);
    assert.equal(await app.client.getText('.no-of-entities'), 'entities: 1');
  });

  test('Footer shows correct number of attributes', async () => {
    await selectKeyspace('gene', app);
    await runQuery('match $x isa age; get; offset 0; limit 1;', app);
    assert.equal(await app.client.getText('.no-of-attributes'), 'attributes: 1');
  });

  test('Footer shows correct number of relations', async () => {
    await selectKeyspace('gene', app);
    await runQuery('match $x isa parentship; get; limit 1;', app);
    assert.equal(await app.client.getText('.no-of-relations'), 'relations: 1');
  });
});
