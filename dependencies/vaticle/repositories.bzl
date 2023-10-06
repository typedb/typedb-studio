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

def vaticle_bazel_distribution():
    git_repository(
        name = "vaticle_bazel_distribution",
        remote = "https://github.com/dmitrii-ubskii/bazel-distribution",
        commit = "1bfbed403f6cf1f804af435e7c2dbd43ae25a671",
    )

def vaticle_dependencies():
    git_repository(
        name = "vaticle_dependencies",
        remote = "https://github.com/vaticle/dependencies",
        commit = "fcc9a56b65e6ab69bbf0f1680affe38e12617ed6", # sync-marker: do not remove this comment, this is used for sync-dependencies by @vaticle_dependencies
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
        remote = "https://github.com/vaticle/typedb-common",
        tag = "2.24.10",  # sync-marker: do not remove this comment, this is used for sync-dependencies by @vaticle_typedb_common
    )

def vaticle_typedb_driver():
    git_repository(
        name = "vaticle_typedb_driver",
        remote = "https://github.com/vaticle/typedb-driver",
        tag = "2.24.11",  # sync-marker: do not remove this comment, this is used for sync-dependencies by @vaticle_typedb_client_java
    )
