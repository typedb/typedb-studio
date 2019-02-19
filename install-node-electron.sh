#!/usr/bin/env bash

set -ex

orig_dir=$(pwd)
temp_dir=$(mktemp -d)

pushd ${temp_dir}
${npm_node_execpath} ${npm_execpath} install grpc --runtime=electron --target=3.0.6
cp -Rv ./node_modules/grpc/src/node/extension_binary/electron-* ${orig_dir}/node_modules/grpc/src/node/extension_binary/
popd
rm -rf ${temp_dir}
