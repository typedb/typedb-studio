import Grakn from 'grakn-client';
import ServerSettings from '@/components/ServerSettings';

export const loadKeyspaces = async (context, credentials) => {
  try {
    const grakn = new Grakn(ServerSettings.getServerUri(), credentials);
    const resp = await grakn.keyspaces().retrieve();
    context.commit('setIsGraknRunning', true);
    context.commit('setKeyspaces', resp);
    grakn.close();
  } catch (e) {
    context.commit('setIsGraknRunning', false);
  }
};

export const createKeyspace = async (context, name) => {
  const session = await global.grakn.session(name);
  await session.transaction().write().then(async (tx) => { await context.dispatch('loadKeyspaces'); tx.close(); });
  await session.close();
};

export const deleteKeyspace = async (context, name) => global.grakn.keyspaces().delete(name)
  .then(async () => { await context.dispatch('loadKeyspaces'); });

export const login = (context, credentials) =>
  // TODO: Keyspace 'grakn' is hardcoded until we will implement an authenticate endpoint in gRPC
  context.dispatch('initGrakn', credentials).then(() => {
    context.commit('setCredentials', credentials);
    context.commit('userLogged', true);
  });

export const initGrakn = (context, credentials) => {
  context.dispatch('loadKeyspaces', credentials);
  global.grakn = new Grakn(ServerSettings.getServerUri(credentials));
  global.graknTx = {};
};

export const logout = async (context) => {
  context.commit('setCredentials', undefined);
  context.commit('setKeyspaces', undefined);
  context.commit('userLogged', false);
  // Need to notify all the other states that they need to invalidate GraknClient
};
