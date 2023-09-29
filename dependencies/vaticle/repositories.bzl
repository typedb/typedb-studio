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

load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")

def vaticle_dependencies():
    git_repository(
        name = "vaticle_dependencies",
        remote = "https://github.com/dmitrii-ubskii/vaticle-dependencies",
        commit = "54c29b9560883cdc7d4b37873930618f15995539",  # sync-marker: do not remove this comment, this is used for sync-dependencies by @vaticle_dependencies
    )

def vaticle_force_graph():
    git_repository(
        name = "vaticle_force_graph",
        remote = "https://github.com/vaticle/force-graph",
        commit = "dc5e7119fc09bafae0acadab1bdc14241337bc7b",
    )

def vaticle_typedb_common():
    git_repository(
        name = "vaticle_typedb_common",
        remote = "https://github.com/dmitrii-ubskii/typedb-common",
        commit = "21a29d0751910d40958eb3e52482f9b1f28d73ca",  # sync-marker: do not remove this comment, this is used for sync-dependencies by @vaticle_typedb_common
    )

def vaticle_typedb_driver():
    native.local_repository(
        name = "vaticle_typedb_driver",
        path = "/Users/dmitriiubskii/workspace/assembly-workspace/typedb-driver",
    )
#    git_repository(
#        name = "vaticle_typedb_driver",
#        remote = "https://github.com/vaticle/typedb-driver",
#        commit = "9fbf317598c62ad168439d2b95a49edc5203d6a1",  # sync-marker: do not remove this comment, this is used for sync-dependencies by @vaticle_typedb_client_java
#    )
