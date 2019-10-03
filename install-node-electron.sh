#!/usr/bin/env bash

set -ex

orig_dir=$(pwd)
temp_dir=$(mktemp -d)

pushd ${temp_dir}
LINUX_URL="https://node-precompiled-binaries.grpc.io/grpc/v1.23.2/electron-v3.0-linux-x64-glibc.tar.gz"
DARWIN_URL="https://node-precompiled-binaries.grpc.io/grpc/v1.23.2/electron-v3.0-darwin-x64-unknown.tar.gz"
WINDOWS_URL="https://node-precompiled-binaries.grpc.io/grpc/v1.23.3/electron-v3.0-win32-x64-unknown.tar.gz"

platform=$(uname)
if [[ "$platform" == "Darwin" ]]; then
    DOWNLOAD_URL="$DARWIN_URL"
elif [[ "$platform" == "Linux" ]]; then
    DOWNLOAD_URL="$LINUX_URL"
elif [[ "$platform" == MSYS_NT-10.0* ]]; then
    DOWNLOAD_URL="$WINDOWS_URL"
else
  echo "grpc does not have a binary for $platform"
  exit 1
fi

curl -LO $DOWNLOAD_URL
tar xvf *.tar.gz && rm *.tar.gz
cp -Rv electron-* ${orig_dir}/node_modules/grpc/src/node/extension_binary/
popd
rm -rf ${temp_dir}
