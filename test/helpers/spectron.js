const interval = require('interval-promise');

function waitUntil(condition) {
  return new Promise((resolve) => {
    interval(async (iteration, stop) => {
      const conditionResult = await condition();
      if (conditionResult) {
        stop();
        resolve();
      }
    }, 100, { iterations: 1000 });
  });
}

export default {
  waitUntil,
};
