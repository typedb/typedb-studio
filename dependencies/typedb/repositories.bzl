# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")

def typedb_dependencies():
    git_repository(
        name = "typedb_dependencies",
        remote = "https://github.com/typedb/typedb-dependencies",
        commit = "6b9bc322bab187ebad65b192c129feffd6c704f4", # sync-marker: do not remove this comment, this is used for sync-dependencies by @typedb_dependencies
    )

def typedb_force_graph():
    git_repository(
        name = "typedb_force_graph",
        remote = "https://github.com/typedb/force-graph",
        commit = "13ac7ec8a01809dc9aad11d404cbca0ebad61646",
    )

def typeql():
    git_repository(
        name = "typeql",
        remote = "https://github.com/typedb/typeql",
        commit = "3063987ccb66dd8a2e96cd440ab76865ea886f97",  # sync-marker: do not remove this comment, this is used for sync-dependencies by @typeql
    )

def typedb_driver():
    git_repository(
        name = "typedb_driver",
        remote = "https://github.com/typedb/typedb-driver",
        tag = "3.1.0",  # sync-marker: do not remove this comment, this is used for sync-dependencies by @typedb_driver
    )
