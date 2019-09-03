import interval from 'interval-promise';


// eslint-disable-next-line import/prefer-default-export
export const waitUntil = criteria => new Promise(async (resolve, reject) => {
  const intervalTime = 1;
  const iterations = 1000;
  let isFullfilled = false;
  let numOfIterations = 0;

  await interval(async (iteration, stop) => {
    numOfIterations += 1;
    if (await criteria()) {
      isFullfilled = true;
      stop();
    }
  }, intervalTime, { iterations });

  if (isFullfilled) return resolve();
  console.log(`rejected after ${numOfIterations} iterations`);
  return reject();
});
