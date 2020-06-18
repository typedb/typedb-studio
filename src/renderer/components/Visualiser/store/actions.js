/* eslint-disable no-unused-vars */
import {
  RUN_CURRENT_QUERY,
  EXPLAIN_CONCEPT,
  UPDATE_NODES_LABEL,
  UPDATE_NODES_COLOUR,
  UPDATE_METATYPE_INSTANCES,
  INITIALISE_VISUALISER,
  CURRENT_KEYSPACE_CHANGED,
  CANVAS_RESET,
  DELETE_SELECTED_NODES,
  LOAD_NEIGHBOURS,
  LOAD_ATTRIBUTES,
} from '@/components/shared/StoresActions';
import logger from '@/../Logger';

import {
  addResetGraphListener,
  loadMetaTypeInstances,
  validateQuery,
  computeAttributes,
  getFilteredNeighbourAnswers,
} from '../VisualiserUtils';
import QuerySettings from '../RightBar/SettingsTab/QuerySettings';
import VisualiserGraphBuilder from '../VisualiserGraphBuilder';
import VisualiserCanvasEventsHandler from '../VisualiserCanvasEventsHandler';
import CDB from '../../shared/CanvasDataBuilder';
import { reopenTransaction } from '../../shared/SharedUtils';


const collect = (array, current) => array.concat(current);

export default {
  [INITIALISE_VISUALISER]({ state, commit, dispatch }, { container, visFacade }) {
    addResetGraphListener(dispatch, CANVAS_RESET);
    commit('setVisFacade', visFacade.initVisualiser(container, state.visStyle));
    VisualiserCanvasEventsHandler.registerHandlers({ state, commit, dispatch });
  },

  [CANVAS_RESET]({ state, commit }) {
    state.visFacade.resetCanvas();
    commit('selectedNodes', null);
    commit('updateCanvasData');
  },

  async [CURRENT_KEYSPACE_CHANGED]({ state, dispatch, commit, rootState }, keyspace) {
    if (keyspace !== state.currentKeyspace) {
      dispatch(CANVAS_RESET);
      commit('setCurrentQuery', '');
      commit('currentKeyspace', keyspace);

      if (global.graknSession) await global.graknSession.close();
      global.graknSession = await global.grakn.session(keyspace);
      // eslint-disable-next-line no-prototype-builtins
      if (!global.graknTx) global.graknTx = {};
      if (global.graknTx[rootState.activeTab]) global.graknTx[rootState.activeTab].close();
      global.graknTx[rootState.activeTab] = await global.graknSession.transaction().write();
      dispatch(UPDATE_METATYPE_INSTANCES);
    }
  },

  async [UPDATE_METATYPE_INSTANCES]({ dispatch, commit, rootState }) {
    try {
      const graknTx = global.graknTx[rootState.activeTab];
      const metaTypeInstances = await loadMetaTypeInstances(graknTx);
      commit('metaTypeInstances', metaTypeInstances);
    } catch (e) {
      await reopenTransaction(rootState, commit);
      console.log(e);
      logger.error(e.stack);
      throw e;
    }
  },

  async [UPDATE_NODES_LABEL]({ state, dispatch, rootState }, type) {
    const nodesToUpdate = state.visFacade.getAllNodes().filter(x => x.type === type);
    const updatedNodes = await CDB.updateNodesLabel(nodesToUpdate);
    state.visFacade.updateNode(updatedNodes);
  },

  [UPDATE_NODES_COLOUR]({ state }, type) {
    const nodes = state.visFacade.getAllNodes().filter(x => x.type === type);
    const updatedNodes = nodes.map(node => Object.assign(node, state.visStyle.computeNodeStyle(node)));
    state.visFacade.updateNode(updatedNodes);
  },

  async [LOAD_NEIGHBOURS]({ state, commit, dispatch, rootState }, { visNode, neighboursLimit }) {
    try {
      commit('loadingQuery', true);
      const graknTx = global.graknTx[rootState.activeTab];
      const filteredResult = await getFilteredNeighbourAnswers(visNode, graknTx, neighboursLimit);
      const targetConcept = await graknTx.getConcept(visNode.id);
      const data = await CDB.buildNeighbours(targetConcept, filteredResult, graknTx);
      visNode.offset += neighboursLimit;
      state.visFacade.updateNode(visNode);
      state.visFacade.addToCanvas(data);
      if (data.nodes.length) state.visFacade.fitGraphToWindow();
      commit('updateCanvasData');
      const styledNodes = data.nodes.map(node => Object.assign(node, state.visStyle.computeNodeStyle(node)));
      state.visFacade.updateNode(styledNodes);
      const nodesWithAttribtues = await computeAttributes(data.nodes, graknTx);
      state.visFacade.updateNode(nodesWithAttribtues);
      commit('loadingQuery', false);
    } catch (e) {
      await reopenTransaction(rootState, commit);
      commit('loadingQuery', false);
      console.log(e);
      logger.error(e.stack);
      throw e;
    }
  },

  async [RUN_CURRENT_QUERY]({ state, commit, rootState }) {
    try {
      commit('setGlobalErrorMsg', '');
      const query = state.currentQuery;
      validateQuery(query);
      commit('loadingQuery', true);
      const graknTx = global.graknTx[rootState.activeTab];
      const result = await (await graknTx.query(query, { explain: true })).collect();
      if (!result.length) {
        commit('loadingQuery', false);
        return null;
      }

      const queryTypes = {
        GET: 'get',
        PATH: 'compute path',
      };

      // eslint-disable-next-line no-prototype-builtins
      const queryType = (result[0].hasOwnProperty('map') ? queryTypes.GET : queryTypes.PATH);

      let nodes = [];
      const edges = [];
      if (queryType === queryTypes.GET) {
        const shouldLoadRPs = QuerySettings.getRolePlayersStatus();
        const shouldLimit = true;

        const instancesData = await CDB.buildInstances(result, query);
        nodes.push(...instancesData.nodes);
        edges.push(...instancesData.edges);

        const typesData = await CDB.buildTypes(result);
        nodes.push(...typesData.nodes);
        edges.push(...typesData.edges);

        if (shouldLoadRPs) {
          const rpData = await CDB.buildRPInstances(result, { nodes, edges }, shouldLimit, graknTx);
          nodes.push(...rpData.nodes);
          edges.push(...rpData.edges);
        }
      } else if (queryType === queryTypes.PATH) {
        // TBD - handle multiple paths
        const path = result[0];
        const pathNodes = await Promise.all(path.list().map(id => graknTx.getConcept(id)));
        const pathData = await VisualiserGraphBuilder.buildFromConceptList(path, pathNodes);
        nodes.push(...pathData.nodes);
        edges.push(...pathData.edges);
      }

      state.visFacade.addToCanvas({ nodes, edges });
      state.visFacade.fitGraphToWindow();
      commit('updateCanvasData');

      nodes = await computeAttributes(nodes, graknTx);

      state.visFacade.updateNode(nodes);

      commit('loadingQuery', false);

      return { nodes, edges };
    } catch (e) {
      await reopenTransaction(rootState, commit);
      commit('loadingQuery', false);
      console.log(e);
      logger.error(e.stack);
      throw e;
    }
  },
  async [LOAD_ATTRIBUTES]({ state, commit, rootState }, { visNode, neighboursLimit }) {
    try {
      const graknTx = global.graknTx[rootState.activeTab];
      commit('loadingQuery', true);
      const query = `match $x id ${visNode.id}, has attribute $y; get $y; offset ${visNode.attrOffset}; limit ${neighboursLimit};`;
      state.visFacade.updateNode({ id: visNode.id, attrOffset: visNode.attrOffset + neighboursLimit });

      const result = await (await graknTx.query(query)).collect();

      const shouldLoadRPs = QuerySettings.getRolePlayersStatus();
      const shouldLimit = true;
      const data = await CDB.buildInstances(result);

      if (shouldLoadRPs) {
        const rpData = await CDB.buildRPInstances(result, shouldLimit, graknTx);
        data.nodes.push(...rpData.nodes);
        data.edges.push(...rpData.edges);
      }
      state.visFacade.addToCanvas(data);
      data.nodes = await computeAttributes(data.nodes, graknTx);
      state.visFacade.updateNode(data.nodes);
      commit('loadingQuery', false);

      if (data) { // when attributes are found, construct edges and add to graph
        const edges = data.nodes.map(attr => CDB.getEdge(visNode, attr, CDB.edgeTypes.instance.HAS));

        state.visFacade.addToCanvas({ nodes: data.nodes, edges });
        commit('updateCanvasData');
      }
    } catch (e) {
      await reopenTransaction(rootState, commit);
      commit('loadingQuery', false);
      console.log(e);
      logger.error(e.stack);
      throw e;
    }
  },
  async [EXPLAIN_CONCEPT]({ state, getters, commit, rootState }) {
    try {
      const graknTx = global.graknTx[rootState.activeTab];

      const node = getters.selectedNode;
      const queryPattern = node.queryPattern;
      const queryPatternVariales = Array.from(new Set(queryPattern.match(/\$[^\s|)|;|,]*/g))).map(x => x.substring(1));

      const explanation = await node.explanation();


      let isRelUnassigned = false;
      const rule = await explanation.getRule();
      const when = rule && await rule.getWhen();
      const relRegex = /(\$[^\s]*|;|{)(\s*?\(.*?\))/g;
      let relMatches = relRegex.exec(when);
      while (relMatches) {
        if (!relMatches[1].includes('$')) {
          isRelUnassigned = true;
          break;
        }
        relMatches = relRegex.exec(when);
      }

      if (isRelUnassigned) {
        commit(
          'setGlobalErrorMsg',
          'The rule `when` definition for this inferred concept contains at least one unassigned relation. At the moment explanation cannot be provided for such a rule.',
        );
      } else {
        const explanationAnswers = explanation.getAnswers();
        const explanationPromises = [];
        const explanationResult = [];

        explanationAnswers.forEach((answer) => {
          const answerVariabes = Array.from(answer.map().keys());

          const isJointExplanation = answerVariabes.every(variable => queryPatternVariales.includes(variable));
          if (answer.hasExplanation() && isJointExplanation) {
            explanationPromises.push(answer.explanation());
          } else if (!isJointExplanation) {
            explanationResult.push(answer);
          }
        });

        (await Promise.all(explanationPromises)).map(expl => expl.getAnswers()).reduce(collect, []).forEach((expl) => {
          explanationResult.push(expl);
        });

        if (explanationResult.length > 0) {
          const data = await CDB.buildInstances(explanationResult);
          const rpData = await CDB.buildRPInstances(explanationResult, data, false, graknTx);
          data.nodes.push(...rpData.nodes);
          data.edges.push(...rpData.edges);

          state.visFacade.addToCanvas(data);
          commit('updateCanvasData');
          const nodesWithAttributes = await computeAttributes(data.nodes, graknTx);

          state.visFacade.updateNode(nodesWithAttributes);
          const styledEdges = data.edges.map(edge => ({ ...edge, label: edge.hiddenLabel, ...state.visStyle.computeExplanationEdgeStyle() }));
          state.visFacade.updateEdge(styledEdges);
          commit('loadingQuery', false);
        } else {
          commit('setGlobalErrorMsg', 'The transaction has been refreshed since the loading of this node and, as a result, the explaination is incomplete.');
        }
      }
    } catch (e) {
      await reopenTransaction(rootState, commit);
      commit('loadingQuery', false);
      console.log(e);
      logger.error(e.stack);
      throw e;
    }
  },

  async [DELETE_SELECTED_NODES]({ state, commit }) {
    state.selectedNodes.forEach((node) => {
      state.visFacade.deleteNode(node);
    });
    commit('selectedNodes', null);
  },
};
