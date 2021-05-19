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

let selectedNodes = null;

export function dimNotHighlightedNodesAndEdges(connectedNodes, connectedEdges) {
  const nodesToUpdate =
  this.getNode()
    .filter(x => !connectedNodes.includes(x.id))
    .map(x => ({ id: x.id, color: x.color.dimmedColor, font: { color: x.font.dimmedColor } }));

  const edgesToUpdate =
  this.getEdge()
    .filter(edge => !connectedEdges.includes(edge.id))
    .map(edge => ({ id: edge.id,
      color: {
        color: edge.color.color.replace('1)', '0.2)'),
        highlight: edge.color.highlight,
        hover: edge.color.hover,
      } }));

  this.updateEdge(edgesToUpdate);
  this.updateNode(nodesToUpdate);
}

export function setDefaultColoursOnDimmedElements(connectedNodes, connectedEdges) {
  const nodesToUpdate =
  this.getNode()
    .filter(x => !connectedNodes.includes(x.id))
    .map(x => ({ id: x.id, color: x.colorClone, font: x.fontClone }));

  const edgesToUpdate =
  this.getEdge()
    .filter(x => !connectedEdges.includes(x.id))
    .map(edge => ({ id: edge.id,
      color: {
        color: edge.color.color.replace('0.2)', '1)'),
        highlight: edge.color.highlight,
        hover: edge.color.hover,
      } }));

  this.updateNode(nodesToUpdate);
  this.updateEdge(edgesToUpdate);
}

export function showEdgeLabels(edges) {
  const edgesToUpdate = edges.map(edge => ({ ...edge, label: edge.hiddenLabel }));
  this.updateEdge(edgesToUpdate);
}

export function showEdgeArrows(edges) {
  const edgesToUpdate = edges.map(edge => ({ ...edge, arrows: { to: { enabled: true, scaleFactor: 0.3 } } }));
  this.updateEdge(edgesToUpdate);
}

export function hideEdgeLabels(edges) {
  const edgesToUpdate = edges.map(edge => ({ ...edge, label: '' }));
  this.updateEdge(edgesToUpdate);
}

export function hideEdgeArrows(edges) {
  const edgesToUpdate = edges.map(edge => ({ ...edge, arrows: { to: { enabled: false } } }));
  this.updateEdge(edgesToUpdate);
}

export function bringFocusToEdgesConnectedToNode(nodeId) {
  const connectedEdgeIds = this.getNetwork().getConnectedEdges(nodeId);
  let connectedEdges = this.getEdge(connectedEdgeIds);

  if (connectedEdges.length) {
    if (connectedEdges[0].options.hideLabel) {
      // Hide labels on ALL edges before showing the ones on connected edges
      this.hideEdgeLabels(this.getEdge());
      this.showEdgeLabels(connectedEdges);
    }

    connectedEdges = this.getEdge(connectedEdgeIds);
    if (connectedEdges[0].options.hideArrow) {
      // Hide arrow on ALL edges before showing the ones on connected edges
      this.hideEdgeArrows(this.getEdge());
      this.showEdgeArrows(connectedEdges);
    }
  }
}

/*
* Events handlers
*/

export function onDragEnd(params) {
// Fix the position of all the dragged nodes
  params.nodes.forEach((nodeId) => {
    this.updateNode({ id: nodeId, fixed: { x: true, y: true } });
  });
}

export function onDragStart(params) {
  if (!params.nodes.length) return;
  // Release the position of all the nodes that are about to be dragged
  params.nodes.forEach((nodeId) => {
    this.updateNode({ id: nodeId, fixed: { x: false, y: false } });
  });
  const nodeId = params.nodes[0];
  this.bringFocusToEdgesConnectedToNode(nodeId);
}

export function onSelectNode(params) {
  selectedNodes = params.nodes;
  const nodeId = params.nodes[0];
  this.bringFocusToEdgesConnectedToNode(nodeId);
}

export function onClick(params) {
  if (!params.nodes.length) selectedNodes = null;
}

export function onContext(params) {
  const nodeId = this.getNetwork().getNodeAt(params.pointer.DOM);
  if (nodeId && !selectedNodes) {
    this.bringFocusToEdgesConnectedToNode(nodeId);
    this.getNetwork().selectNodes([nodeId], true);
  }
}

export function onHoverNode(params) {
  const nodeId = params.node;
  const connectedEdgeIds = this.getNetwork().getConnectedEdges(nodeId);
  const connectedEdges = this.getEdge(connectedEdgeIds);
  let connectedNodes = this.getNetwork().getConnectedNodes(nodeId).concat(nodeId);

  if (connectedEdges.length) {
    if (connectedEdges[0].options.hideLabel) {
      this.showEdgeLabels(connectedEdges);
    }

    if (connectedEdges[0].options.hideArrow) {
      this.showEdgeArrows(connectedEdges);
    }
  }

  // Highlight neighbour nodes
  connectedNodes.forEach((id) => { this.highlightNode(id); });

  if (selectedNodes) {
    connectedNodes = connectedNodes.concat(selectedNodes);
  }
  // Dim remaining nodes in network
  this.dimNotHighlightedNodesAndEdges(connectedNodes, connectedEdgeIds);
}


export function onBlurNode(params) {
  const nodeId = params.node;
  const connectedEdges = [];
  // When node is deleted do not get connected edges
  if (this.nodeExists(nodeId)) {
    const connectedEdgeIds = this.getNetwork().getConnectedEdges(nodeId);
    const connectedEdges = this.getEdge(connectedEdgeIds);
    const isNodeSelected = this.getNetwork().getSelectedNodes().includes(nodeId);

    if (connectedEdges.length && !isNodeSelected && connectedEdges[0].options.hideLabel) {
      // Hide labels on connected edges - if the blurred node is not selected
      this.hideEdgeLabels(connectedEdges);
    }
  }
  const connectedNodes = this.getNetwork().getConnectedNodes(nodeId).concat(nodeId);

  // Remove highlighting from neighbours nodes
  connectedNodes.forEach((id) => { this.removeHighlightNode(id); });
  // Put colour back to the nodes
  this.setDefaultColoursOnDimmedElements(connectedNodes, connectedEdges);
}

export function onDeselectNode(params) {
  const nodeId = params.previousSelection.nodes[0];
  const connectedNodes = this.getNetwork().getConnectedNodes(nodeId).concat(nodeId);
  let allEdges = this.getEdge();
  if (allEdges.length && allEdges[0].options.hideLabel) {
    this.hideEdgeLabels(allEdges);
  }

  allEdges = this.getEdge();
  if (allEdges.length && allEdges[0].options.hideArrow) {
    this.hideEdgeArrows(allEdges);
  }
  // Remove highlighting from neighbours nodes
  connectedNodes.forEach((id) => { this.removeHighlightNode(id); });
}
