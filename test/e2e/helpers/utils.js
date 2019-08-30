import interval from 'interval-promise';

export const waitUntil = async condition => new Promise((resolve, reject) => {
  let totalTime = 0;
  const intervalTime = 1;
  const maxTime = 100000;

  interval(async (iteration, stop) => {
    totalTime += intervalTime;
    const conditionResult = await condition();
    if (conditionResult) {
      stop();
      resolve(true);
    }
    if (totalTime === maxTime) reject(false);
  }, intervalTime);
});

export const waitUntillQueryCompletion = async (app) => {
  const hasLoadingStarted = await waitUntil(async () => app.client.isExisting('.bp3-spinner-animation')).catch(result => result);
  const hasLoadingFinished = await waitUntil(async () => !(await app.client.isExisting('.bp3-spinner-animation'))).catch(result => result);
  return hasLoadingStarted && hasLoadingFinished;
};
