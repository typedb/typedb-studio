const Application = require('spectron').Application;
const assert = require('assert');
const electronPath = require('electron'); // Require Electron from the binaries included in node_modules.
const path = require('path');

const sleep = time => new Promise(r => setTimeout(r, time));
jest.setTimeout(30000);


const app = new Application({
  path: electronPath,
  args: [path.join(__dirname, '../../dist/electron/main.js')],
});

beforeAll(async () => app.start());

afterAll(async () => {
  if (app && app.isRunning()) {
    return app.stop();
  }
  return undefined;
});

describe('Favourite queries', () => {
  test('initialize workbase', async () => {
    const visible = await app.browserWindow.isVisible();
    assert.equal(visible, true);
  });

  test('create new keyspace', async () => {
    app.client.click('.toggle-preferences');
    await sleep(1000);
    await app.client.setValue('.keyspace-input', 'test');
    app.client.click('.new-keyspace-btn');
    await sleep(10000);
    expect(await app.client.getText('.keyspace-label')).toEqual(expect.arrayContaining(['test']));
  });

  test('delete exisiting keyspace', async () => {
    app.client.click('.delete-keyspace-btn');
    await sleep(1000);
    app.client.click('.confirm');
    await sleep(10000);
    expect(await app.client.getText('.keyspace-label')).not.toEqual(expect.arrayContaining(['test']));
  });
});
