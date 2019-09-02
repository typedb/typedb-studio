/* eslint-disable no-await-in-loop */
import interval from 'interval-promise';

export const waitUntil = criteria => new Promise(async (resolve, reject) => {
  const intervalTime = 1;
  const iterations = 10000;
  let isFullfilled = false;

  await interval(async (iteration, stop) => {
    if (await criteria()) {
      isFullfilled = true;
      stop();
    }
  }, intervalTime, { iterations });

  if (isFullfilled) return resolve();
  return reject();
});

export const waitUntillQueryCompletion = async (app) => {
  await waitUntil(async () => app.client.isExisting('.bp3-spinner-animation'));
  await waitUntil(async () => !(await app.client.isExisting('.bp3-spinner-animation')));
};
