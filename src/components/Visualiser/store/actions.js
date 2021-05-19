/*
 * Copyright (C) 2021 Vaticle
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

/* eslint-disable no-unused-vars */
import {
  RUN_CURRENT_QUERY,
  EXPLAIN_CONCEPT,
  UPDATE_NODES_LABEL,
  UPDATE_NODES_COLOUR,
  UPDATE_METATYPE_INSTANCES,
  INITIALISE_VISUALISER,
  CURRENT_DATABASE_CHANGED,
  CANVAS_RESET,
  DELETE_SELECTED_NODES,
  LOAD_NEIGHBOURS,
  LOAD_ATTRIBUTES,
  REOPEN_GLOBAL_TYPEDB_TX,
} from '@/components/shared/StoresActions';
import logger from '@/logger';

import {
  addResetGraphListener,
  loadMetaTypeInstances,
  validateQuery,
  computeAttributes,
  getNeighbourAnswers,
  getConcept
} from '../VisualiserUtils';
import QuerySettings from '../RightBar/SettingsTab/QuerySettings';
import VisualiserGraphBuilder from '../VisualiserGraphBuilder';
import VisualiserCanvasEventsHandler from '../VisualiserCanvasEventsHandler';
import CDB from '../../shared/CanvasDataBuilder';
import { getTransactionOptions, reopenTransaction } from '../../shared/SharedUtils';
import { SessionType } from "typedb-client/api/TypeDBSession";
import { TransactionType } from "typedb-client/api/TypeDBTransaction";

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

  async [CURRENT_DATABASE_CHANGED]({ state, dispatch, commit, rootState }, database) {
    if (database !== state.currentDatabase) {
      dispatch(CANVAS_RESET);
      commit('setCurrentQuery', '');
      commit('currentDatabase', database);

      if (global.typeDBSession) await global.typeDBSession.close();
      global.typeDBSession = await global.typedb.session(database, SessionType.DATA);
      // eslint-disable-next-line no-prototype-builtins
      if (!global.typeDBTx) global.typeDBTx = {};
      if (global.typeDBTx[rootState.activeTab]) global.typeDBTx[rootState.activeTab].close();
      global.typeDBTx[rootState.activeTab] = await global.typeDBSession.transaction(TransactionType.READ, getTransactionOptions());
      dispatch(UPDATE_METATYPE_INSTANCES);
    }
  },

  async [UPDATE_METATYPE_INSTANCES]({ dispatch, commit, rootState }) {
    try {
      const typeDBTx = global.typeDBTx[rootState.activeTab];
      const metaTypeInstances = await loadMetaTypeInstances(typeDBTx);
      commit('metaTypeInstances', metaTypeInstances);
    } catch (e) {
      await reopenTransaction(rootState, commit);
      console.log(e);
      logger.error(e.stack);
      throw e;
    }
  },

  async [UPDATE_NODES_LABEL]({ state, dispatch, rootState }, type) {
    const nodesToUpdate = state.visFacade.getAllNodes().filter(x => x.typeLabel === type);
    const updatedNodes = await CDB.updateNodesLabel(nodesToUpdate);
    state.visFacade.updateNode(updatedNodes);
  },

  [UPDATE_NODES_COLOUR]({ state }, type) {
    const nodes = state.visFacade.getAllNodes().filter(x => x.typeLabel === type);
    const updatedNodes = nodes.map(node => Object.assign(node, state.visStyle.computeNodeStyle(node)));
    state.visFacade.updateNode(updatedNodes);
  },

  async [LOAD_NEIGHBOURS]({ state, commit, dispatch, rootState }, { visNode, neighboursLimit }) {
    try {
      commit('loadingQuery', true);
      const typeDBTx = global.typeDBTx[rootState.activeTab];

      const currentData = {
        nodes: state.visFacade.getAllNodes(),
        edges: state.visFacade.getAllEdges(),
      };

      const neighbourAnswers = await getNeighbourAnswers(visNode, currentData.edges, typeDBTx);
      const targetConcept = await getConcept(visNode, typeDBTx);
      const data = await CDB.buildNeighbours(targetConcept, neighbourAnswers);

      currentData.nodes.push(...data.nodes);
      currentData.edges.push(...data.edges);

      const shouldLoadRPs = QuerySettings.getRolePlayersStatus();
      if (shouldLoadRPs) {
        const rpData = await CDB.buildRPInstances(neighbourAnswers, currentData, true, typeDBTx);
        data.nodes.push(...rpData.nodes);
        data.edges.push(...rpData.edges);
      }

      state.visFacade.addToCanvas(data);
      if (data.nodes.length) state.visFacade.fitGraphToWindow();
      commit('updateCanvasData');
      const styledNodes = data.nodes.map(node => Object.assign(node, state.visStyle.computeNodeStyle(node)));
      state.visFacade.updateNode(styledNodes);
      const nodesWithAttribtues = await computeAttributes(data.nodes, typeDBTx);
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
      const typeDBTx = global.typeDBTx[rootState.activeTab];
      const result = await typeDBTx.query().match(query).collect();
      if (!result.length) {
        commit('loadingQuery', false);
        return null;
      }

      const queryTypes = {
        MATCH: 'match',
        PATH: 'compute path',
      };

      // eslint-disable-next-line no-prototype-builtins
      // const queryType = (result[0].hasOwnProperty('map') ? queryTypes.GET : queryTypes.PATH);
      const queryType = queryTypes.MATCH;

      let nodes = [];
      const edges = [];
      if (queryType === queryTypes.MATCH) {
        const shouldLoadRPs = QuerySettings.getRolePlayersStatus();
        const shouldLimit = true;

        const instancesData = await CDB.buildInstances(result);
        nodes.push(...instancesData.nodes);
        edges.push(...instancesData.edges);

        const typesData = await CDB.buildTypes(result);
        nodes.push(...typesData.nodes);
        edges.push(...typesData.edges);

        if (shouldLoadRPs) {
          const rpData = await CDB.buildRPInstances(result, { nodes, edges }, shouldLimit, typeDBTx);
          nodes.push(...rpData.nodes);
          edges.push(...rpData.edges);
        }
      } else if (queryType === queryTypes.PATH) {
        // TBD - handle multiple paths
        const path = result[0];
        const pathNodes = await Promise.all(path.list().map(id => typeDBTx.getConcept(id)));
        const pathData = await VisualiserGraphBuilder.buildFromConceptList(path, pathNodes);
        nodes.push(...pathData.nodes);
        edges.push(...pathData.edges);
      }

      state.visFacade.addToCanvas({ nodes, edges });
      state.visFacade.fitGraphToWindow();
      commit('updateCanvasData');

      nodes = await computeAttributes(nodes, typeDBTx);

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
      const typeDBTx = global.typeDBTx[rootState.activeTab];
      commit('loadingQuery', true);
      if (!visNode.iid && !visNode.typeLabel) throw "Node does not have a Type Label or an IID";
      state.visFacade.updateNode({ id: visNode.id, attrOffset: visNode.attrOffset + neighboursLimit });
      let query;
      let result;
      if (visNode.iid) {
        query = `match $x type ${visNode.typeLabel}, owns $y; get $y; limit 1;`;
        const ownedAttrs = await typeDBTx.query().match(query).collect();
        if (ownedAttrs.length) { // If no attributes are owned, this query errors because it's unsatisfiable.
          query = `match $x iid ${visNode.iid}, has attribute $y; get $y; offset ${visNode.attrOffset}; limit ${neighboursLimit};`;
          result = await typeDBTx.query().match(query).collect();
        } else {
          result = [];
        }
      } else {
        query = `match $x type ${visNode.typeLabel}, owns $y; get $y; offset ${visNode.attrOffset}; limit ${neighboursLimit};`;
        result = await typeDBTx.query().match(query).collect();
      }

      const data = await CDB.buildInstances(result);
      const shouldLoadRPs = QuerySettings.getRolePlayersStatus();
      const shouldLimit = true;

      if (shouldLoadRPs) {
        const rpData = await CDB.buildRPInstances(result, shouldLimit, typeDBTx);
        data.nodes.push(...rpData.nodes);
        data.edges.push(...rpData.edges);
      }
      state.visFacade.addToCanvas(data);
      data.nodes = await computeAttributes(data.nodes, typeDBTx);
      state.visFacade.updateNode(data.nodes);
      commit('loadingQuery', false);

      if (data) { // when attributes are found, construct edges and add to graph
        const edges = await Promise.all(data.nodes.map(async attr => {
          let ownerConcept = await getConcept(visNode, typeDBTx);
          let attrConcept = await getConcept(attr, typeDBTx);
          return CDB.getEdge(ownerConcept, attrConcept, CDB.edgeTypes.instance.HAS)
        }));

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
  // eslint-disable-next-line consistent-return
  async [EXPLAIN_CONCEPT]({ state, getters, commit, rootState }) {
    try {
      const node = getters.selectedNode;
      const typeDBTx = global.typeDBTx[rootState.activeTab];

      if (!node.explainable) {
          return;
      }
      if (!node.explanations) {
        node.explanations = typeDBTx.query().explain(node.explainable).iterator();
        state.visFacade.updateNode(node);
      }
      const explanationNext = await node.explanations.next();
      if (explanationNext.done) {
          node.explanations = null;
          node.explanationExhausted = true;
          state.visFacade.updateNode(node);
      } else {
        const explanation = explanationNext.value;
        const answers = [explanation.condition()];

        const data = await CDB.buildInstances(answers);
        const rpData = await CDB.buildRPInstances(answers, data, false, typeDBTx);
        data.nodes.push(...rpData.nodes);
        data.edges.push(...rpData.edges);

        // this is to avoid overriding the explanation object of nodes that are already visualised
        data.nodes = data.nodes.filter(node => !state.visFacade.getNode().some(currNode => currNode.id === node.id));

        state.visFacade.addToCanvas(data);
        commit('updateCanvasData');
        const nodesWithAttributes = await computeAttributes(data.nodes, typeDBTx);

        state.visFacade.updateNode(nodesWithAttributes);
        const styledEdges = data.edges.map(edge => ({ ...edge, label: edge.hiddenLabel, ...state.visStyle.computeExplanationEdgeStyle() }));
        state.visFacade.updateEdge(styledEdges);
        commit('loadingQuery', false);
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

  async [REOPEN_GLOBAL_TYPEDB_TX]({ rootState, commit }) {
    if (global.typeDBSession && global.typeDBTx) {
      if (global.typeDBTx[rootState.activeTab]) {
        global.typeDBTx[rootState.activeTab].close();
      }
      global.typeDBTx[rootState.activeTab] = await global.typeDBSession.transaction(TransactionType.READ, getTransactionOptions());
      rootState[rootState.activeTab].visFacade.resetCanvas();
      commit('selectedNodes', null);
      commit('updateCanvasData');
    }
  },
};
