/*
 * Copyright (C) 2021 Grakn Labs
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
import { reopenTransaction } from '../../shared/SharedUtils';
import { SessionType } from "grakn-client/api/GraknSession";
import { TransactionType } from "grakn-client/api/GraknTransaction";
import { GraknOptions } from "grakn-client/api/GraknOptions";


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

  async [CURRENT_DATABASE_CHANGED]({ state, dispatch, commit, rootState }, database) {
    if (database !== state.currentDatabase) {
      dispatch(CANVAS_RESET);
      commit('setCurrentQuery', '');
      commit('currentDatabase', database);

      if (global.graknSession) await global.graknSession.close();
      global.graknSession = await global.grakn.session(database, SessionType.DATA);
      // eslint-disable-next-line no-prototype-builtins
      if (!global.graknTx) global.graknTx = {};
      if (global.graknTx[rootState.activeTab]) global.graknTx[rootState.activeTab].close();
      global.graknTx[rootState.activeTab] = await global.graknSession.transaction(TransactionType.READ);
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
      const graknTx = global.graknTx[rootState.activeTab];

      const currentData = {
        nodes: state.visFacade.getAllNodes(),
        edges: state.visFacade.getAllEdges(),
      };

      const neighbourAnswers = await getNeighbourAnswers(visNode, currentData.edges, graknTx);
      const targetConcept = await getConcept(visNode, graknTx);
      const data = await CDB.buildNeighbours(targetConcept, neighbourAnswers);

      currentData.nodes.push(...data.nodes);
      currentData.edges.push(...data.edges);

      const shouldLoadRPs = QuerySettings.getRolePlayersStatus();
      if (shouldLoadRPs) {
        const rpData = await CDB.buildRPInstances(neighbourAnswers, currentData, true, graknTx);
        data.nodes.push(...rpData.nodes);
        data.edges.push(...rpData.edges);
      }

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
      const options = GraknOptions.core({ explain: false });
      const result = await graknTx.query().match(query, options).collect();
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
      if (!visNode.iid && !visNode.typeLabel) throw "Node does not have a Type Label or an IID";
      state.visFacade.updateNode({ id: visNode.id, attrOffset: visNode.attrOffset + neighboursLimit });
      let query;
      let result;
      if (visNode.iid) {
        query = `match $x type ${visNode.typeLabel}, owns $y; get $y; limit 1;`;
        const ownedAttrs = await graknTx.query().match(query).collect();
        if (ownedAttrs.length) { // If no attributes are owned, this query errors because it's unsatisfiable.
          query = `match $x iid ${visNode.iid}, has attribute $y; get $y; offset ${visNode.attrOffset}; limit ${neighboursLimit};`;
          result = await graknTx.query().match(query).collect();
        } else {
          result = [];
        }
      } else {
        query = `match $x type ${visNode.typeLabel}, owns $y; get $y; offset ${visNode.attrOffset}; limit ${neighboursLimit};`;
        result = await graknTx.query().match(query).collect();
      }

      const data = await CDB.buildInstances(result);
      const shouldLoadRPs = QuerySettings.getRolePlayersStatus();
      const shouldLimit = true;

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
        const edges = await Promise.all(data.nodes.map(async attr => {
          let ownerConcept = await getConcept(visNode, graknTx);
          let attrConcept = await getConcept(attr, graknTx);
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
      const graknTx = global.graknTx[rootState.activeTab];

      const isRelUnassigned = (when) => {
        let isRelUnassigned = false;
        const relRegex = /(\$[^\s]*|;|{)(\s*?\(.*?\))/g;
        let relMatches = relRegex.exec(when);

        while (relMatches) {
          if (!relMatches[1].includes('$')) {
            isRelUnassigned = true;
            break;
          }
          relMatches = relRegex.exec(when);
        }

        return isRelUnassigned;
      };

      const errorUnassigneRels = ruleLabel => (
        // eslint-disable-next-line max-len
        `The 'when' body of the rule [${ruleLabel}] contains at least one unassigned relation. To see the full explanation for this concept, please redefine the rule with relation variables.`
      );

      const isTargetExplAnswer = answer => Array.from(answer.map().values()).map(concept => concept.id).some(id => id === node.id);

      const originalExpl = await node.explanation();
      const rule = originalExpl.getRule();
      const isExplJoin = !rule;
      let finalExplAnswers;

      if (!isExplJoin) {
        const when = await rule.getWhen();
        const label = await rule.label();
        if (isRelUnassigned(when)) {
          commit('setGlobalErrorMsg', errorUnassigneRels(label));
          return false;
        }

        finalExplAnswers = originalExpl.getAnswers();
      } else {
        const ruleExpl = await Promise.all(originalExpl.getAnswers().filter(answer => answer.hasExplanation() && isTargetExplAnswer(answer)).map(answer => answer.explanation()));
        const ruleDetails = await Promise.all(ruleExpl.map((explanation) => {
          const rule = explanation.getRule();
          return Promise.all([rule.label(), rule.getWhen()]);
        }));

        const violatingRule = ruleDetails.find(([, when]) => isRelUnassigned(when));
        if (violatingRule) {
          commit('setGlobalErrorMsg', errorUnassigneRels(violatingRule.label));
          return false;
        }

        finalExplAnswers = ruleExpl.map(expl => expl.getAnswers()).reduce(collect, []);
      }

      if (finalExplAnswers.length > 0) {
        const data = await CDB.buildInstances(finalExplAnswers);
        const rpData = await CDB.buildRPInstances(finalExplAnswers, data, false, graknTx);
        data.nodes.push(...rpData.nodes);
        data.edges.push(...rpData.edges);

        // this is to avoid overriding the explanation object of nodes that are already visualised
        data.nodes = data.nodes.filter(node => !state.visFacade.getNode().some(currNode => currNode.id === node.id));

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
