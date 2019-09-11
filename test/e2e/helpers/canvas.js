import interval from 'interval-promise';

const getCoordinates = async (nodeQuery, app) => {
  const [width, height] = await app.client.browserWindow.getSize('#graph-div');
  const position = (await app.client.execute((nodeQuery, width, height) => {
    // CAUTION: accessing __vue__ to obtin any information nested insided it is considered bad practice
    // and should be avoided unless for exceptional cases, when the target information is otherwise
    // inaccessible normally the need for such information should not arise within end-to-end tests,
    // but rather within integration or unit tests
    // eslint-disable-next-line no-underscore-dangle
    const visualiser = document.getElementsByClassName('visualiser-wrapper')[0].__vue__.$store.state['tab-1'].visFacade.container.visualiser;
    const nodes = visualiser.getNode();
    const targetNode = nodes.find(node => node[nodeQuery.by] === nodeQuery.value);
    const coordinates = visualiser.getNetwork().getPositions(targetNode.id)[targetNode.id];
    // coordinates returned by network.getPositions() of VisJS network are relative to the center of the canvas, whereas
    // the position that needs to be returned and handled by the test should in face be the top and left offset of the canvas
    // thus, the calculations below
    coordinates.x += width / 2;
    coordinates.y += (height / 2) + 10; // + 10 here increases the top offset to get closer to the center of the node
    return coordinates;
  }, nodeQuery, width, height)).value;
  return position;
};

// eslint-disable-next-line import/prefer-default-export
export const getNodePosition = async (nodeQuery, app) => {
  let currentCoordinates = await getCoordinates(nodeQuery, app);
  await interval(async (iteration, stop) => {
    const newCoordinates = await getCoordinates(nodeQuery, app);
    if (newCoordinates.x === currentCoordinates.x &&
        newCoordinates.y === currentCoordinates.y) {
      stop();
    }
    currentCoordinates = newCoordinates;
  }, 2000);

  return currentCoordinates;
};
