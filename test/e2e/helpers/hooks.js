const Application = require('spectron').Application;
const electronPath = require('electron');
const path = require('path');

export const startApp = async () => {
  const app = await new Application({
    path: electronPath,
    args: [path.join(__dirname, '../../../dist/electron/main.js')],
  }).start();

  return app;
};

export const stopApp = async (app) => {
  if (app && app.isRunning()) {
    await app.stop().catch((error) => { throw error; });
  }
};
