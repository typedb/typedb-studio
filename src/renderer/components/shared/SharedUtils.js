export const META_LABELS = new Set(['entity', 'relation', 'attribute', 'role', 'thing']);

export const baseTypes = {
  TYPE: 'META_TYPE',
  ENTITY_TYPE: 'ENTITY_TYPE',
  RELATION_TYPE: 'RELATION_TYPE',
  ATTRIBUTE_TYPE: 'ATTRIBUTE_TYPE',
  RULE: 'RULE',
  ROLE: 'ROLE',
  ENTITY_INSTANCE: 'ENTITY',
  RELATION_INSTANCE: 'RELATION',
  ATTRIBUTE_INSTANCE: 'ATTRIBUTE',
};

export const reopenTransaction = async (state, commit) => {
  const graknTx = global.graknTx[state.activeTab];
  try {
    await graknTx.querya('match $x sub entity; get; limit 1;');
  } catch (e) {
    global.graknTx[state.activeTab] = await global.graknSession.transaction().write();
    commit('setGlobalErrorMsg', 'The transaction was refreshed and, as a result, the explanation of currently displayed inferred nodes may be incomplete.');
  }
};
