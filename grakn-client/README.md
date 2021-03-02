# Grakn Client for Node.js

[![Grabl](https://grabl.io/api/status/graknlabs/client-nodejs/badge.svg)](https://grabl.io/graknlabs/client-nodejs)
[![Discord](https://img.shields.io/discord/665254494820368395?color=7389D8&label=chat&logo=discord&logoColor=ffffff)](https://grakn.ai/discord)
[![Discussion Forum](https://img.shields.io/discourse/https/discuss.grakn.ai/topics.svg)](https://discuss.grakn.ai)
[![Stack Overflow](https://img.shields.io/badge/stackoverflow-grakn-796de3.svg)](https://stackoverflow.com/questions/tagged/grakn)
[![Stack Overflow](https://img.shields.io/badge/stackoverflow-graql-3dce8c.svg)](https://stackoverflow.com/questions/tagged/graql)

## Client Architecture
To learn about the mechanism that a Grakn Client uses to set up communication with databases running on the Grakn Server, refer to [Grakn > Client API > Overview](http://dev.grakn.ai/docs/client-api/overview).

## API Reference
To learn about the methods available for executing queries and retrieving their answers using Client Node.js, refer to [Grakn > Client API > Node.js > API Reference](http://dev.grakn.ai/docs/client-api/nodejs#api-reference).

## Concept API
To learn about the methods available on the concepts retrieved as the answers to Graql queries, refer to [Grakn > Concept API > Overview](http://dev.grakn.ai/docs/concept-api/overview).

## Import Grakn Client for Node.js through `npm`

```shell script
npm install grakn-client
```
Further documentation: https://dev.grakn.ai/docs/client-api/nodejs

## Build Grakn Client for Node.js from Source

> Note: You don't need to compile Grakn Client from source if you just want to use it in your code. See the _"Import Grakn Client for Node.js"_ section above.

1. Make sure you have [Node.js](https://nodejs.org/) (version 14 or above) and its `npm` package manager installed on your machine
1. Clone the project and run `npm install` at its root directory (containing `package.json`)
1. Run `npm run build`
1. The JavaScript files, and their matching TypeScript type definitions are compiled to the `dist` directory.

> Note: TypeScript is not required in order to run Grakn Client, however its type assertions may make development smoother.
