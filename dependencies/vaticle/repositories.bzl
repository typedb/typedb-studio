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
        commit = "e29665e029449bc361aadead62b3900f32bb4c3f", # sync-marker: do not remove this comment, this is used for sync-dependencies by @vaticle_dependencies
    )
#    native.local_repository(
#        name = "vaticle_dependencies",
#        path = "../dependencies",
#    )

def vaticle_force_graph():
    git_repository(
        name = "vaticle_force_graph",
        remote = "https://github.com/vaticle/force-graph",
        commit = "52f1327d90148f0acc5a46293cecca856e06805a",
    )
#    native.local_repository(
#        name = "vaticle_force_graph",
#        path = "../force-graph",
#    )

def vaticle_typedb_client_java():
    git_repository(
        name = "vaticle_typedb_client_java",
        remote = "https://github.com/alexjpwalker/client-java",
        commit = "e177ec35dc16e2db21252954512214b655e29b90",  # sync-marker: do not remove this comment, this is used for sync-dependencies by @vaticle_typedb_client_java
    )
#    native.local_repository(
#        name = "vaticle_typedb_client_java",
#        path = "../client-java",
#    )
