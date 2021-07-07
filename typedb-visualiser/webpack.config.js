const path = require("path");
const { CleanWebpackPlugin } = require("clean-webpack-plugin");

module.exports = {
  entry: "src/index.ts",
  output: {
    path: path.resolve(__dirname, "dist"),
    libraryTarget: "umd",
    library: "typedb-visualiser",
    filename: "index.js",
  },
  plugins: [
    new CleanWebpackPlugin(),
  ],
  resolve: {
    modules: [__dirname, "node_modules"],
    extensions: ["*", ".js", ".jsx", ".tsx", ".ts"],
  },
  module: {
    rules: [
      {
        test: /\.(ts|js)x?$/,
        exclude: /node_modules/,
        use: {
          loader: "babel-loader",
        },
      },
      {
        test: /\.s?css$/,
        use: [
          { loader: "style-loader" }, "css-loader", "sass-loader"],
      },
      {
        test: /\.tsx?$/,
        use: "ts-loader",
        exclude: /node_modules/,
      },
      {
        test: /\.(png|svg|jpe?g|gif|otf)$/,
        use: 'file-loader'
      },
    ],
  },
};
