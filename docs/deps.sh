#!/usr/bin/env bash
#
# Copyright (C) 2022 Vaticle
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.
#

# RUN COMMAND #1: ./docs/deps.sh global-package-structure //...
# RUN COMMAND #2: ./docs/deps.sh state-package-structure //state/...
# RUN COMMAND #3: ./docs/deps.sh framework-package-structure //framework/... //state...
# RUN COMMAND #3: ./docs/deps.sh module-package-structure //:studio //framework //state

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
