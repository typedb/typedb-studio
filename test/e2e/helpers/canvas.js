// eslint-disable-next-line import/prefer-default-export
export const getNodePosition = async (nodeId, app) => {
  const [width, height] = await app.client.browserWindow.getSize('#graph-div');

  const coordinates = (await app.client.execute((nodeId, width, height) => {
    const visFacade = window.visFacade;
    const coordinates = visFacade.getNetwork().getPositions(nodeId)[nodeId];
    coordinates.x += width / 2;
    coordinates.y += (height / 2) + 10;
    return coordinates;
  }, nodeId, width, height)).value;

  return coordinates;
};

export const getNodeId = async (query, app) => {
  const nodeId = (await app.client.execute((query) => {
    const visFacade = window.visFacade;
    const nodes = visFacade.getAllNodes();
    const targetNode = nodes.find(node => node[query.by] === query.value);
    return targetNode.id;
  }, query)).value;
  return nodeId;
};
