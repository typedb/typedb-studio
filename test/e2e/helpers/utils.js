import interval from 'interval-promise';

// eslint-disable-next-line import/prefer-default-export
export const waitUntil = criteria => new Promise(async (resolve, reject) => {
  const intervalTime = 1;
  const iterations = 3000;
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
