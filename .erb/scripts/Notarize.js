const { notarize } = require('electron-notarize');
const { build } = require('../../package.json');

exports.default = async function notarizeMacos(context) {
  const { electronPlatformName, appOutDir } = context;
  if (electronPlatformName !== 'darwin') {
    return;
  }

  if (!process.env.CI) {
    console.warn('Skipping notarizing step. Packaging is not running in CI');
    return;
  }

  if (!('APPLEID' in process.env && 'APPLEID_PASSWORD' in process.env)) {
    console.warn('Skipping notarizing step. APPLEID and APPLEID_PASSWORD env variables must be set');
    return;
  }

  const appName = context.packager.appInfo.productFilename;

  await notarize({
    appBundleId: build.appId,
    appPath: `${appOutDir}/${appName}.app`,
    appleId: process.env.APPLEID,
    appleIdPassword: process.env.APPLEID_PASSWORD,
  });
};
