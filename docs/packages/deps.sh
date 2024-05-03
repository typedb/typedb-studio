#!/usr/bin/env bash
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

# RUN COMMAND #1: ./docs/packages/deps.sh service-package-structure //service/...
# RUN COMMAND #2: ./docs/packages/deps.sh framework-package-structure //framework/... //service
# RUN COMMAND #3: ./docs/packages/deps.sh module-package-structure //:studio //framework //service
# RUN COMMAND #4: ./docs/packages/deps.sh global-package-structure //...

popd > /dev/null

[[ $(readlink $0) ]] && path=$(readlink $0) || path=$0
OUT_DIR=$(cd "$(dirname "${path}")" && pwd -P)
pushd "$OUT_DIR" > /dev/null


if [ -z "$3" ]
  then
    exclude=""
  else
    exclude="|$3"
fi
if [ -z "$4" ]
  then
    exclude=$exclude
  else
    exclude="$exclude|$4"
fi

filter="filter('^(?!(//dependencies|@vaticle|//test$exclude).*$).*', kind(kt_jvm_library, deps($2)))"
bazel query $filter --output graph > "$1".dot
dot -Tpng < "$1".dot > "$1".png
open "$1".png

popd > /dev/null
