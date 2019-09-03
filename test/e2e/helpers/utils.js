import interval from 'interval-promise';
import assert from 'assert';


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
  await assert.doesNotReject(async () => waitUntil(async () => app.client.isExisting('.toasted.info')));
  await assert.doesNotReject(async () => waitUntil(async () => !(await app.client.isExisting('.bp3-spinner-animation'))));
};

export const waitUntilNotificationDisapears = async (app) => {
  await assert.doesNotReject(async () => waitUntil(async () => app.client.isExisting('.toasted.info')));
  await assert.doesNotReject(async () => waitUntil(async () => !(await app.client.isExisting('.toasted.info'))));
};
