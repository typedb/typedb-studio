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

export default {
  currentDatabase(state, database) {
    state.currentDatabase = database;
  },
  loadingSchema(state, isRunning) {
    state.loadingSchema = isRunning;
  },
  setVisFacade(state, facade) {
    state.visFacade = Object.freeze(facade); // Freeze it so that Vue does not attach watchers to its properties
  },
  selectedNodes(state, nodeIds) {
    state.selectedNodes = (nodeIds) ? state.visFacade.getNode(nodeIds) : null;
  },
  metaTypeInstances(state, instances) {
    state.metaTypeInstances = instances;
  },
  registerCanvasEvent(state, { event, callback }) {
    state.visFacade.registerEventHandler(event, callback);
  },
  updateCanvasData(state) {
    if (state.visFacade) {
      state.canvasData = {
        entities: state.visFacade.getAllNodes().filter(x => x.baseType === 'ENTITY').length,
        attributes: state.visFacade.getAllNodes().filter(x => x.baseType === 'ATTRIBUTE').length,
        relations: state.visFacade.getAllNodes().filter(x => x.baseType === 'RELATION').length };
    }
  },
  setContextMenu(state, contextMenu) {
    state.contextMenu = contextMenu;
  },
  setSchemaHandler(state, schemaHandler) {
    state.schemaHandler = schemaHandler;
  },
};
