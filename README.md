[![Grabl](https://grabl.io/api/status/graknlabs/grakn/badge.svg)](https://grabl.io/graknlabs/grakn)
[![Slack Status](http://grakn-slackin.herokuapp.com/badge.svg)](https://grakn.ai/slack)
[![Discussion Forum](https://img.shields.io/discourse/https/discuss.grakn.ai/topics.svg)](https://discuss.grakn.ai)
[![Stack Overflow](https://img.shields.io/badge/stackoverflow-grakn-796de3.svg)](https://stackoverflow.com/questions/tagged/grakn)
[![Stack Overflow](https://img.shields.io/badge/stackoverflow-graql-3dce8c.svg)](https://stackoverflow.com/questions/tagged/graql)

## Grakn Workbase

![Grakn Workbase 1.0](https://user-images.githubusercontent.com/567679/45933937-7987bc00-bf8e-11e8-8b26-8fb020c77310.png)

Grakn Workbase is Grakn's Integrated Development Environment to perform knowledge engineering. At the moment, Workbase provides two main functionalities: visualisation of the knowledge graph, and modelling of the knowledge schema. 

You can also see Workbase as another interface for a user to interact with their Grakn database, as an alternative to the [Grakn Console](http://dev.grakn.ai/docs/running-grakn/console) and [Grakn Client API/Drivers](http://dev.grakn.ai/docs/client-api/overview).

Workbase Visualiser allows you to visualise data in the Grakn knowledge graph, and investigate their relations, by performing read queries ([`match-get` queries](/docs/query/get-query)) as well as one of the computer queries: [`compute path`](/docs/query/compute-query#compute-the-shortest-path) queries. Whether you need a tool to test and experiment with your newly created Grakn knowledge graph, or that you prefer a graphical interface for reading data from Grakn, you will find Workbase extremely useful.

## Download Grakn Workbase
Grakn Workbase is available for Linux, Mac and Windows. Head over to the [Workbase Releases page](https://github.com/graknlabs/workbase/releases) to download and install the latest release of Workbase.

## Documentation
Learn how to [connect Workbase to the Grakn Server](http://dev.grakn.ai/docs/workbase/connection), [execute and visualise Graql queries](http://dev.grakn.ai/docs/workbase/visualisation), and interact with the visualiser to [investigate instances of data](http://dev.grakn.ai/docs/workbase/investigation).

- - -

## Build From Source
> Note: You don't need to build Workbase from source if you just want to use it. See the "Download Grakn Workbase" section above.

1. Make sure you have the following dependencies installed on your machine:
  - `npm` >= 6.4.1
  - `node` >= 10.0
  - `yarn` >= 1.17
2. Run `yarn install`
3. Run `yarn build`
Outputs to `build/grakn-workbase-{version}-{mac|linux|windows}.{dmg|tar.gz|exe}`.

To run Grakn Workbase in development mode, run `yarn dev`.
To run the unit, integration and end-to-end tests, run `yarn unit`, `yarn integration` and `yarn e2e` respectively.
