# TypeDB Visualiser Project

Contains visualisation tools for TypeDB, including:

- Force-directed graph layout for TypeDB, powered by [d3-force](https://github.com/d3/d3-force)
- Graph renderer for TypeDB, powered by [PIXI.js](https://pixijs.com)
- [React](https://reactjs.org) component for rendering TypeDB graphs using PIXI and d3-force
- [Prism.js](https://prismjs.com) definitions for TypeQL

TypeDB Studio depends on the source files directly and compiles them as part of its regular Webpack build, but other projects can depend on `typedb-visualiser` via `npm` using NPM:

```shell script
npm install typedb-visualiser
```
or using Yarn:
```shell script
yarn add typedb-visualiser
```

### Usage notes

`react`, `prismjs` and `pixi-viewport` are specified as optional peer dependencies.

- `react` is required in order to use the React components `react/TypeDBVisualiser` and `react/TypeDBStaticVisualiser`
- `prismjs` is required in order to use the Prism.js TypeQL installer, `installPrismTypeQL` (from `prism-typeql`)
- `pixi-viewport` is required in order to use `react/TypeDBVisualiser`

### Build `npm` package

```shell script
cd src/typedb-visualiser
./build.sh
```

### Publish `npm` package

First, set the desired version number in `src/typedb-visualiser/build/package.json`, and then run:
```shell script
cd src/typedb-visualiser
./publish.sh
```
