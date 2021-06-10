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

import {
  ADD_OWNS,
  ADD_PLAYS,
  CANVAS_RESET,
  COMMIT_TX,
  CURRENT_DATABASE_CHANGED,
  DEFINE_ATTRIBUTE_TYPE,
  DEFINE_ENTITY_TYPE,
  DEFINE_RELATION_TYPE,
  DEFINE_RULE,
  DELETE_OWNS,
  DELETE_PLAYS,
  DELETE_SCHEMA_CONCEPT,
  INITIALISE_VISUALISER,
  LOAD_SCHEMA,
  OPEN_TYPEDB_TX,
  REFRESH_SELECTED_NODE,
  UPDATE_METATYPE_INSTANCES,
} from '@/components/shared/StoresActions';
import logger from '@/logger';

import SchemaHandler from '../SchemaHandler';
import { computeAttributes, computeRoles, loadMetaTypeInstances, updateNodePositions, } from '../SchemaUtils';
import SchemaCanvasEventsHandler from '../SchemaCanvasEventsHandler';
import CDB from '../../shared/CanvasDataBuilder';
import { SessionType } from "typedb-client/api/connection/TypeDBSession";
import { TransactionType } from "typedb-client/api/connection/TypeDBTransaction";

export default {
  async [OPEN_TYPEDB_TX]({ commit }) {
    if (global.typeDBSession && global.typeDBSession.type() === SessionType.DATA) {
        const database = global.typeDBSession.database().name();
        global.typeDBSession.close();
        global.typeDBSession = await global.typedb.session(database, SessionType.SCHEMA);
    }
    const tx = await global.typeDBSession.transaction(TransactionType.WRITE);
    if (!global.typeDBTx) global.typeDBTx = {};
    global.typeDBTx.schemaDesign = tx;
    commit('setSchemaHandler', new SchemaHandler(tx));
    return tx;
  },

  async [CURRENT_DATABASE_CHANGED]({ state, dispatch, commit }, database) {
    if (database !== state.currentDatabase) {
      dispatch(CANVAS_RESET);
      commit('currentDatabase', database);
      if (global.typeDBSession) await global.typeDBSession.close();
      global.typeDBSession = await global.typedb.session(database, SessionType.SCHEMA);
      dispatch(UPDATE_METATYPE_INSTANCES);
      dispatch(LOAD_SCHEMA);
    }
  },

  async [UPDATE_METATYPE_INSTANCES]({ dispatch, commit }) {
    const tx = await dispatch(OPEN_TYPEDB_TX);
    const metaTypeInstances = await loadMetaTypeInstances(tx);
    tx.close();
    commit('metaTypeInstances', metaTypeInstances);
  },

  [CANVAS_RESET]({ state, commit }) {
    state.visFacade.resetCanvas();
    commit('selectedNodes', null);
    commit('updateCanvasData');
  },

  [INITIALISE_VISUALISER]({ state, commit, dispatch }, { container, visFacade }) {
    commit('setVisFacade', visFacade.initVisualiser(container, state.visStyle));
    SchemaCanvasEventsHandler.registerHandlers({ state, commit, dispatch });
  },

  async [LOAD_SCHEMA]({ state, commit, dispatch }) {
    const tx = await dispatch(OPEN_TYPEDB_TX);

    try {
      if (!state.visFacade) return;
      commit('loadingSchema', true);

      const answers = await tx.query().match('match $x sub thing;').collect();

      const data = await CDB.buildTypes(answers);
      data.nodes = updateNodePositions(data.nodes);

      state.visFacade.addToCanvas({ nodes: data.nodes, edges: data.edges });
      state.visFacade.fitGraphToWindow();

      data.nodes = await computeAttributes(data.nodes, tx);
      data.nodes = await computeRoles(data.nodes, tx);
      state.visFacade.updateNode(data.nodes);

      tx.close();
      commit('loadingSchema', false);
    } catch (e) {
      logger.error(e.stack);
      tx.close();
      commit('loadingSchema', false);
      throw e;
    }
  },

  async [COMMIT_TX](store, tx) {
    return tx.commit();
  },

  async [DEFINE_ENTITY_TYPE]({ state, dispatch }, payload) {
    let tx = await dispatch(OPEN_TYPEDB_TX);

    // define entity type
    await state.schemaHandler.defineEntityType(payload.entityLabel, payload.superType);

    // add attribute types to entity type
    await Promise.all(payload.attributeTypes.map(async (attributeType) => {
      await state.schemaHandler.addAttribute(payload.entityLabel, attributeType);
    }));

    // add roles to entity type
    await Promise.all(payload.roleTypes.map(async (roleType) => {
      const [relationLabel, roleLabel] = roleType.split(':');
      await state.schemaHandler.addPlaysRole(payload.entityLabel, relationLabel, roleLabel);
    }));

    await dispatch(COMMIT_TX, tx)
      .catch((e) => {
        tx.close();
        logger.error(e.stack);
        throw e;
      });

    await dispatch(UPDATE_METATYPE_INSTANCES);

    tx = await dispatch(OPEN_TYPEDB_TX);

    const concept = await tx.concepts().getEntityType(payload.entityLabel);

    const node = await CDB.getTypeNode(concept);
    const edges = await CDB.getTypeEdges(concept, [node.id, ...state.visFacade.getAllNodes().map(n => n.id)]);

    state.visFacade.addToCanvas({ nodes: [node], edges });

    // attach attributes and roles to visnode and update on graph to render the right bar attributes
    let nodes = await computeAttributes([node], tx);
    nodes = await computeRoles(nodes, tx);
    state.visFacade.updateNode(nodes);
    tx.close();
  },

  async [DEFINE_ATTRIBUTE_TYPE]({ state, dispatch }, payload) {
    let tx = await dispatch(OPEN_TYPEDB_TX);

    // define attribute type
    await state.schemaHandler.defineAttributeType(payload.attributeLabel, payload.superType, payload.valueType);

    // add attribute types to attribute type
    await Promise.all(payload.attributeTypes.map(async (attributeType) => {
      await state.schemaHandler.addAttribute(payload.attributeLabel, attributeType);
    }));

    // add roles to attribute type
    await Promise.all(payload.roleTypes.map(async (roleType) => {
      const [relationLabel, roleLabel] = roleType.split(':');
      await state.schemaHandler.addPlaysRole(payload.attributeLabel, relationLabel, roleLabel);
    }));

    await dispatch(COMMIT_TX, tx)
      .catch((e) => {
        tx.close();
        logger.error(e.stack);
        throw e;
      });

    await dispatch(UPDATE_METATYPE_INSTANCES);

    tx = await dispatch(OPEN_TYPEDB_TX);

    const concept = await tx.concepts().getAttributeType(payload.attributeLabel);

    const node = await CDB.getTypeNode(concept);
    const edges = await CDB.getTypeEdges(concept, [node.id, ...state.visFacade.getAllNodes().map(n => n.id)]);

    state.visFacade.addToCanvas({ nodes: [node], edges });

    // attach attributes and roles to visnode and update on graph to render the right bar attributes
    let nodes = await computeAttributes([node], tx);
    nodes = await computeRoles(nodes, tx);
    state.visFacade.updateNode(nodes);
    tx.close();
  },

  async [ADD_OWNS]({ state, dispatch }, payload) {
    let tx = await dispatch(OPEN_TYPEDB_TX);

    // add attribute types to schema concept
    await Promise.all(payload.attributeTypes.map(async (attributeType) => {
      await state.schemaHandler.addAttribute(payload.schemaLabel, attributeType);
    }));

    await dispatch(COMMIT_TX, tx)
      .catch((e) => {
        tx.close();
        logger.error(e.stack);
        throw e;
      });
    tx = await dispatch(OPEN_TYPEDB_TX);

    const node = state.visFacade.getNode(state.selectedNodes[0].id);
    await Promise.all(payload.attributeTypes.map(async (attributeType) => {
      const valueType = (await tx.concepts().getAttributeType(attributeType)).getValueType();
      node.attributes = [...node.attributes, { type: attributeType, valueType }];
    }));

    const ownerConcept = await tx.concepts().getThingType(node.typeLabel);
    const edges = await CDB.getTypeEdges(ownerConcept, state.visFacade.getAllNodes().map(n => n.id));

    state.visFacade.addToCanvas({ nodes: [], edges });

    tx.close();
    state.visFacade.updateNode(node);
  },

  async [DELETE_OWNS]({ state, dispatch }, payload) {
    let tx = await dispatch(OPEN_TYPEDB_TX);

    await state.schemaHandler.deleteAttribute(payload.schemaLabel, payload.attributeLabel);

    await dispatch(COMMIT_TX, tx)
      .catch((e) => {
        tx.close();
        logger.error(e.stack);
        throw e;
      });

    const node = state.visFacade.getNode(state.selectedNodes[0].id);
    node.attributes = node.attributes.sort((a, b) => ((a.typeLabel > b.typeLabel) ? 1 : -1));
    node.attributes.splice(payload.index, 1);
    state.visFacade.updateNode(node);

    // delete edge to attribute type
    const edgesIds = state.visFacade.edgesConnectedToNode(state.selectedNodes[0].id);

    edgesIds
      .filter(edgeId => (state.visFacade.getEdge(edgeId).to === payload.attributeLabel) &&
        ((state.visFacade.getEdge(edgeId).label === 'owns') || (state.visFacade.getEdge(edgeId).hiddenLabel === 'owns')))
      .forEach((edgeId) => { state.visFacade.deleteEdge(edgeId); });
  },

  async [ADD_PLAYS]({ state, dispatch }, payload) {
    let tx = await dispatch(OPEN_TYPEDB_TX);

    // add role types to schema concept
    await Promise.all(payload.roleTypes.map(async (roleType) => {
      const [relationLabel, roleLabel] = roleType.split(':');
      await state.schemaHandler.addPlaysRole(payload.schemaLabel, relationLabel, roleLabel);
    }));

    await dispatch(COMMIT_TX, tx)
        .catch((e) => {
          tx.close();
          logger.error(e.stack);
          throw e;
        });
    tx = await dispatch(OPEN_TYPEDB_TX);

    const node = state.visFacade.getNode(state.selectedNodes[0].id);

    const edges = await Promise.all(payload.roleTypes.map(async (roleType) => {
      const [relationLabel, _] = roleType.split(':');
      const relationType = await tx.concepts().getRelationType(relationLabel);
      node.roles = [...node.roles, roleType];
      return CDB.getTypeEdges(relationType, state.visFacade.getAllNodes().map(n => n.id));
    })).then(edges => edges.flatMap(x => x));

    state.visFacade.addToCanvas({ nodes: [], edges: edges.flatMap(x => x) });
    tx.close();
    state.visFacade.updateNode(node);
  },

  async [DELETE_PLAYS]({ state, dispatch }, payload) {
    let tx = await dispatch(OPEN_TYPEDB_TX);

    const [relationLabel, roleLabel] = payload.roleLabel.split(':');
    await state.schemaHandler.deletePlaysRole(payload.schemaLabel, relationLabel, roleLabel);

    await dispatch(COMMIT_TX, tx)
      .catch((e) => {
        tx.close();
        logger.error(e.stack);
        throw e;
      });

    const node = state.visFacade.getNode(state.selectedNodes[0].id);
    node.roles = Object.values(node.roles).sort((a, b) => ((a.typeLabel > b.typeLabel) ? 1 : -1));
    node.roles.splice(payload.index, 1);
    state.visFacade.updateNode(node);

    // delete role edge
    const edgesIds = state.visFacade.edgesConnectedToNode(state.selectedNodes[0].id);
    edgesIds
      .filter(edgeId => (state.visFacade.getEdge(edgeId).to === state.selectedNodes[0].id) && (state.visFacade.getEdge(edgeId).from === relationLabel) &&
        ((state.visFacade.getEdge(edgeId).label === roleLabel) || (state.visFacade.getEdge(edgeId).hiddenLabel === roleLabel)))
      .forEach((edgeId) => { state.visFacade.deleteEdge(edgeId); });
  },

  async [DEFINE_RELATION_TYPE]({ state, dispatch }, payload) {
    let tx = await dispatch(OPEN_TYPEDB_TX);
    await state.schemaHandler.defineRelationType(payload.relationLabel, payload.superType);

    // define and relate roles to relation type
    await Promise.all(payload.defineRoles.map(async (roleType) => {
      await state.schemaHandler.addRelatesRole(payload.relationLabel, roleType.label, roleType.overridden);
    }));

    // add attribute types to relation type
    await Promise.all(payload.attributeTypes.map(async (attributeType) => {
      await state.schemaHandler.addAttribute(payload.relationLabel, attributeType);
    }));

    // add roles to relation type
    await Promise.all(payload.roleTypes.map(async (roleType) => {
      const [relationLabel, roleLabel] = roleType.split(':');
      await state.schemaHandler.addPlaysRole(payload.relationLabel, relationLabel, roleLabel);
    }));

    await dispatch(COMMIT_TX, tx)
      .catch((e) => {
        tx.close();
        logger.error(e.stack);
        throw e;
      });

    await dispatch(UPDATE_METATYPE_INSTANCES);

    tx = await dispatch(OPEN_TYPEDB_TX);

    const concept = await tx.concepts().getRelationType(payload.relationLabel);

    const node = await CDB.getTypeNode(concept);
    const edges = await CDB.getTypeEdges(concept, [node.id, ...state.visFacade.getAllNodes().map(n => n.id)]);

    state.visFacade.addToCanvas({ nodes: [node], edges });

    // attach attributes and roles to visnode and update on graph to render the right bar attributes
    let nodes = await computeAttributes([node], tx);
    nodes = await computeRoles(nodes, tx);
    state.visFacade.updateNode(nodes);
    tx.close();
  },

  async [DELETE_SCHEMA_CONCEPT]({ state, dispatch, commit }, payload) {
    const tx = await dispatch(OPEN_TYPEDB_TX);

    const type = await tx.concepts().getThingType(payload.typeLabel);

    if (payload.baseType === 'RELATION_TYPE') {
      const roles = await type.asRemote(tx).getRelates().collect();
      await Promise.all(roles.map(async (role) => {
        const rolePlayers = await role.asRemote(tx).getPlayers().collect();

        await Promise.all(rolePlayers.map(async (player) => {
          await state.schemaHandler.deletePlaysRole(player.getLabel().name(), type.getLabel().name(), role.getLabel().name());
        }));

        await state.schemaHandler.deleteRelatesRole(type.getLabel().name(), role.getLabel().name());
      }));
    } else if (payload.baseType === 'ATTRIBUTE_TYPE') {
      const nodes = state.visFacade.getAllNodes();
      await Promise.all(nodes.map(async (node) => {
        if (node.attributes.some(x => x.typeLabel === payload.typeLabel)) {
          await state.schemaHandler.deleteAttribute(node.typeLabel, payload.typeLabel);
        }
        node.attributes = node.attributes.filter((x => x.typeLabel !== payload.typeLabel));
      }));
      state.visFacade.updateNode(nodes);
    }

    const typeId = await state.schemaHandler.deleteType(payload.typeLabel);

    await dispatch(COMMIT_TX, tx)
      .catch((e) => {
        tx.close();
        logger.error(e.stack);
        throw e;
      });
    state.visFacade.deleteFromCanvas([typeId]);
    commit('selectedNodes', null);
    await dispatch(UPDATE_METATYPE_INSTANCES);
  },

  [REFRESH_SELECTED_NODE]({ state, commit }) {
    const node = state.selectedNodes[0];
    if (!node) return;
    commit('selectedNodes', null);
    commit('selectedNodes', [node.id]);
  },

  async [DEFINE_RULE]({ state, dispatch }, payload) {
    const tx = await dispatch(OPEN_TYPEDB_TX);

    // define rule
    await state.schemaHandler.defineRule(payload.ruleLabel, payload.when, payload.then);

    await dispatch(COMMIT_TX, tx)
      .catch((e) => {
        tx.close();
        logger.error(e.stack);
        throw e;
      });
  },
};
