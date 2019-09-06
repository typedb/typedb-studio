import assert from 'assert';
import { startApp, stopApp } from './helpers/hooks';
import { selectKeyspace, loadKeyspace, cleanKeyspace, waitForQueryCompletion } from './helpers/actions';

jest.setTimeout(100000);


describe('Canvas Data', () => {
  let app;

  beforeAll(() => {
    loadKeyspace('gene');
  });

  beforeEach(async () => {
    app = await startApp();
    const isAppVisible = await app.browserWindow.isVisible();
    assert.equal(isAppVisible, true);
    await selectKeyspace('gene', app);
  });

  afterEach(async () => {
    await stopApp(app);
  });

  afterAll(async () => {
    await cleanKeyspace('gene');
  });

  test('Footer shows correct number of entities', async () => {
    await app.client.click('.top-bar-container .CodeMirror');
    await app.client.keys('match $x isa person; get; offset 0; limit 1;');
    await app.client.click('.run-btn');
    await waitForQueryCompletion(app);
    assert.equal(await app.client.getText('.no-of-entities'), 'entities: 1');
  });

  test('Footer shows correct number of attributes', async () => {
    await app.client.click('.top-bar-container .CodeMirror');
    await app.client.keys('match $x isa age; get; offset 0; limit 1;');
    await app.client.click('.run-btn');
    await waitForQueryCompletion(app);
    assert.equal(await app.client.getText('.no-of-attributes'), 'attributes: 1');
  });

  test('Footer shows correct number of relations', async () => {
    await app.client.click('.top-bar-container .CodeMirror');
    await app.client.keys('match $x isa parentship; get; limit 1;');
    await app.client.click('.run-btn');
    await waitForQueryCompletion(app);
    assert.equal(await app.client.getText('.no-of-relations'), 'relations: 1');
  });
});
