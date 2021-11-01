#
# Copyright (C) 2021 Vaticle
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
        remote = "https://github.com/alexjpwalker/dependencies",
        commit = "958448d88f1845d9b9495a943ab83fe65400c99d", # sync-marker: do not remove this comment, this is used for sync-dependencies by @vaticle_dependencies
    )
#    native.local_repository(
#        name = "vaticle_dependencies",
#        path = "../dependencies",
#    )

def vaticle_force_graph():
    git_repository(
        name = "vaticle_force_graph",
        remote = "https://github.com/vaticle/force-graph",
        commit = "26b8c64171679f534652bf668ea6140fd33aed72",
    )
#    native.local_repository(
#        name = "vaticle_force_graph",
#        path = "../force-graph",
#    )

def vaticle_typedb_client_java():
    git_repository(
        name = "vaticle_typedb_client_java",
        remote = "https://github.com/vaticle/client-java",
        tag = "2.5.0",  # sync-marker: do not remove this comment, this is used for sync-dependencies by @vaticle_typedb_client_java
    )
#    native.local_repository(
#        name = "vaticle_typedb_client_java",
#        path = "../client-java",
#    )
