import Grakn from 'grakn-client';
import ServerSettings from '@/components/ServerSettings';

export const loadKeyspaces = async (context) => {
  try {
    const resp = await context.state.grakn.keyspaces().retrieve();
    context.commit('setIsGraknRunning', true);
    context.commit('setKeyspaces', resp);
  } catch (e) {
    context.commit('setIsGraknRunning', false);
  }
};

export const createKeyspace = async (context, name) => {
  const session = await context.state.grakn.session(name);
  await session.transaction().write().then(async () => { await context.dispatch('loadKeyspaces'); });
  await session.close();
};

export const deleteKeyspace = async (context, name) => context.state.grakn.keyspaces().delete(name)
  .then(async () => { await context.dispatch('loadKeyspaces'); });

export const login = (context, credentials) =>
  // TODO: Keyspace 'grakn' is hardcoded until we will implement an authenticate endpoint in gRPC
  context.dispatch('initGrakn', credentials).then(() => {
    context.commit('setCredentials', credentials);
    context.commit('userLogged', true);
  });

export const initGrakn = (context, credentials) => {
  const grakn = new Grakn(ServerSettings.getServerUri(), credentials);
  context.commit('setGrakn', grakn);
  context.dispatch('loadKeyspaces');
};

export const logout = async (context) => {
  context.commit('setCredentials', undefined);
  context.commit('setGrakn', undefined);
  context.commit('setKeyspaces', undefined);
  context.commit('userLogged', false);
  // Need to notify all the other states that they need to invalidate GraknClient
};
