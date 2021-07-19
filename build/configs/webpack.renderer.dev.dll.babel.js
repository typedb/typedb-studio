/**
 * Builds the DLL for development electron renderer process
 */

// NOTE: Do NOT remove the fragment "babel" from the name of this file - Babel requires it

import webpack from 'webpack';
import path from 'path';
import { merge } from 'webpack-merge';
import baseConfig from './webpack.config';
import { dependencies } from '../../package.json';
import checkNodeEnv from '../scripts/check-node-env';

checkNodeEnv('development');

const dist = path.join(__dirname, '../dll');

export default merge(baseConfig, {
  context: path.join(__dirname, '../..'),

  devtool: 'eval',

  mode: 'development',

  target: 'electron-renderer',

  externals: ['fsevents', 'crypto-browserify'],

  /**
   * Use `module` from `webpack.renderer.dev.js`
   */
  module: require('./webpack.renderer.dev.babel').default.module,

  // If any of our dependencies don't have an "index.js" file, they must be added to the exclude list in entry.renderer
  entry: {
    renderer: [
      ...Object.keys(dependencies || {}),
    ].filter(key => ![].includes(key))
  },

  output: {
    library: 'renderer',
    path: dist,
    filename: '[name].dev.dll.js',
    libraryTarget: 'var',
  },

  plugins: [
    new webpack.DllPlugin({
      path: path.join(dist, '[name].json'),
      name: '[name]',
    }),

    /**
     * Create global constants which can be configured at compile time.
     *
     * Useful for allowing different behaviour between development builds and
     * release builds
     *
     * NODE_ENV should be production so that modules do not perform certain
     * development checks
     */
    new webpack.EnvironmentPlugin({
      NODE_ENV: 'development',
    }),

    new webpack.LoaderOptionsPlugin({
      debug: true,
      options: {
        context: path.join(__dirname, '../../src'),
        output: {
          path: path.join(__dirname, '../dll'),
        },
      },
    }),
  ],
});
