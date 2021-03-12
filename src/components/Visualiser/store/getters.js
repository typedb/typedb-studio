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

export default {
  currentQuery: state => state.currentQuery,
  currentDatabase: state => state.currentDatabase,
  metaTypeInstances: state => state.metaTypeInstances,
  showSpinner: state => state.loadingQuery,
  selectedNodes: state => state.selectedNodes,
  selectedNode: state => ((state.selectedNodes) ? state.selectedNodes[0] : null),
  canvasData: state => state.canvasData,
  isActive: state => (state.currentDatabase !== null),
  contextMenu: state => state.contextMenu,
  globalErrorMsg: state => state.globalErrorMsg,
};
