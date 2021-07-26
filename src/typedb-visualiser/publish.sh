set -ex
./build.sh
cp build/package.json dist/package.json
cp README.md dist/README.md
cd dist
npm publish
cd ..
set +ex
