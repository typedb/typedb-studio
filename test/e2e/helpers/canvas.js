import interval from 'interval-promise';

const getCoordinates = async (nodeQuery, app) => {
  const [width, height] = await app.client.browserWindow.getSize('#graph-div');
  const coordinates = (await app.client.execute((nodeQuery, width, height) => {
    // CAUTION: accessing __vue__ to obtin any information nested insided it is considered bad practice
    // and should be avoided unless for exceptional cases, when the target information is otherwise
    // inaccessible normally the need for such information should not arise within end-to-end tests,
    // but rather within integration or unit tests
    // eslint-disable-next-line no-underscore-dangle
    const visualiser = document.getElementsByClassName('visualiser-wrapper')[0].__vue__.$store.state['tab-1'].visFacade.container.visualiser;
    const nodes = visualiser.getNode();
    const targetNode = nodes.find(node => node[nodeQuery.by] === nodeQuery.value);
    const coordinates = visualiser.getNetwork().getPositions(targetNode.id)[targetNode.id];
    coordinates.x += width / 2;
    coordinates.y += (height / 2) + 10;
    return coordinates;
  }, nodeQuery, width, height)).value;
  return coordinates;
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
