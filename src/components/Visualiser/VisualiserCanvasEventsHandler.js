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

import QuerySettings from './RightBar/SettingsTab/QuerySettings';
import { LOAD_ATTRIBUTES, LOAD_NEIGHBOURS } from '../shared/StoresActions';

export default {
  registerHandlers({ state, dispatch, commit }) {
    commit('registerCanvasEvent', {
      event: 'selectNode',
      callback: (params) => {
        commit('selectedNodes', params.nodes);
        commit('setContextMenu', { show: false, x: null, y: null });
      },
    });

    commit('registerCanvasEvent', {
      event: 'dragStart',
      callback: (params) => {
        if (!params.nodes.length > 1) {
          commit('selectedNodes', [params.nodes[0]]);
        }
      },
    });

    commit('registerCanvasEvent', {
      event: 'click',
      callback: (params) => {
        if (!params.nodes.length) { commit('selectedNodes', null); }
        commit('setContextMenu', { show: false, x: null, y: null });
      },
    });

    commit('registerCanvasEvent', {
      event: 'oncontext',
      callback: (params) => {
        const nodeId = state.visFacade.getNetwork().getNodeAt(params.pointer.DOM);
        if (nodeId) {
          if (!(params.nodes.length > 1)) {
            state.visFacade.getNetwork().unselectAll();
            commit('selectedNodes', [nodeId]);
            state.visFacade.getNetwork().selectNodes([nodeId]);
          }
        } else if (!(params.nodes.length > 1)) {
          commit('selectedNodes', null);
          state.visFacade.getNetwork().unselectAll();
        }
      },
    });

    commit('registerCanvasEvent', {
      event: 'oncontext',
      callback: (params) => {
        // Show context menu when database is selected and canvas has data
        if (state.currentDatabase && (state.canvasData.entities || state.canvasData.attributes || state.canvasData.relations)) {
          commit('setContextMenu', { show: true, x: params.pointer.DOM.x, y: params.pointer.DOM.y });
        }
      },
    });

    commit('registerCanvasEvent', {
      event: 'doubleClick',
      callback: async (params) => {
        const nodeId = params.nodes[0];
        if (!nodeId) return;

        const neighboursLimit = QuerySettings.getNeighboursLimit();
        const visNode = state.visFacade.getNode(nodeId);
        const action = (params.event.srcEvent.shiftKey) ? LOAD_ATTRIBUTES : LOAD_NEIGHBOURS;
        dispatch(action, { visNode, neighboursLimit });
      },
    });

    commit('registerCanvasEvent', {
      event: 'deselectNode',
      callback: () => {
        commit('setContextMenu', { show: false, x: null, y: null });
      },
    });

    commit('registerCanvasEvent', {
      event: 'dragStart',
      callback: () => {
        commit('setContextMenu', { show: false, x: null, y: null });
      },
    });

    commit('registerCanvasEvent', {
      event: 'zoom',
      callback: () => {
        commit('setContextMenu', { show: false, x: null, y: null });
      },
    });

    commit('registerCanvasEvent', {
      event: 'hold',
      callback: (params) => {
        if (params.nodes.length) { commit('selectedNodes', null); state.visFacade.getNetwork().unselectAll(); }
      },
    });
  },
};
