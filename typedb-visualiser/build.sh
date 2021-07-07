set -x
rm -rf dist
npx babel . -d dist --extensions ".ts,.tsx"
tsc
cp -r ./assets/* dist
set +x
