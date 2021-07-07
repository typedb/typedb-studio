set -x
./build.sh
cp build/package.json dist/package.json
cd dist
npm publish
cd ..
set +x
