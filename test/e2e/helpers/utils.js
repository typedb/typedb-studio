/* eslint-disable no-console */
import interval from 'interval-promise';
import { execSync } from 'child_process';
const GraknClient = require('grakn-client');


// eslint-disable-next-line import/prefer-default-export
export const waitUntil = criteria => new Promise(async (resolve, reject) => {
  const intervalTime = 1;
  const iterations = 15000;
  let isFullfilled = false;

  await interval(async (iteration, stop) => {
    if (await criteria()) {
      isFullfilled = true;
      stop();
    }
  }, intervalTime, { iterations });

  if (isFullfilled) {
    return resolve();
  }
  return reject();
});


export const loadKeyspace = (keyspaceName) => {
  if (!process.env.GRAKN_PATH) throw new Error('The path to the Grakn distribution is not provided as the environmental variable; graknPath');
  execSync(`${process.env.GRAKN_PATH}grakn console -k ${keyspaceName} -f $(pwd)/test/helpers/${keyspaceName}.gql`);
};

export const deleteKeyspace = async (keyspaceName) => {
  const client = new GraknClient('localhost:48555');
  const keyspaces = await client.keyspaces().retrieve();
  if (keyspaces.includes(keyspaceName)) {
    await client.keyspaces().delete(keyspaceName);
  }
};
