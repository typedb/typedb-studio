[![Grabl](https://grabl.io/api/status/vaticle/typedb/badge.svg)](https://grabl.io/vaticle/typedb-workbase)
[![Discord](https://img.shields.io/discord/665254494820368395?color=7389D8&label=chat&logo=discord&logoColor=ffffff)](https://vaticle.com/discord)
[![Discussion Forum](https://img.shields.io/discourse/https/forum.vaticle.com/topics.svg)](https://forum.vaticle.com)
[![Stack Overflow](https://img.shields.io/badge/stackoverflow-typedb-796de3.svg)](https://stackoverflow.com/questions/tagged/typedb)
[![Stack Overflow](https://img.shields.io/badge/stackoverflow-typeql-3dce8c.svg)](https://stackoverflow.com/questions/tagged/typeql)

## TypeDB Workbase

![TypeDB Workbase 1.0](https://user-images.githubusercontent.com/567679/45933937-7987bc00-bf8e-11e8-8b26-8fb020c77310.png)

TypeDB Workbase is TypeDB's Integrated Development Environment to perform knowledge engineering. At the moment, Workbase provides two main functionalities: visualisation of the database, and modelling of the knowledge schema. 

You can also see Workbase as another interface for a user to interact with their TypeDB database, as an alternative to the [TypeDB Console](http://docs.vaticle.com/docs/running-typedb/console) and [TypeDB Client API/Drivers](http://docs.vaticle.com/docs/client-api/overview).

Workbase Visualiser allows you to visualise data in the TypeDB database, and investigate their relations, by performing read queries ([`match-get` queries](/docs/query/get-query)) as well as one of the computer queries: [`compute path`](/docs/query/compute-query#compute-the-shortest-path) queries. Whether you need a tool to test and experiment with your newly created TypeDB database, or that you prefer a graphical interface for reading data from TypeDB, you will find Workbase extremely useful.

## Download TypeDB Workbase
TypeDB Workbase is available for Linux, Mac and Windows. Head over to the [Workbase Releases page](https://github.com/vaticle/typedb-workbase/releases) to download and install the latest release of Workbase.

## Documentation
Learn how to [connect Workbase to the TypeDB Server](http://docs.vaticle.com/docs/workbase/connection), [execute and visualise TypeQL queries](http://docs.vaticle.com/docs/workbase/visualisation), and interact with the visualiser to [investigate instances of data](http://docs.vaticle.com/docs/workbase/investigation).

- - -

## Build From Source
> Note: You don't need to build Workbase from source if you just want to use it. See the "Download TypeDB Workbase" section above.

1. Make sure you have the following dependencies installed on your machine:
  - `npm` >= 6.4.1
  - `node` >= 10.0
  - `yarn` >= 1.17
2. Run `yarn install`
3. Run `yarn build`
Outputs to `build/typedb-workbase-{version}-{mac|linux|windows}.{dmg|tar.gz|exe}`.

To run TypeDB Workbase in development mode, run `yarn dev`.
To run the unit, integration and end-to-end tests, run `yarn unit`, `yarn integration` and `yarn e2e` respectively.
