# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")

def typedb_dependencies():
    git_repository(
        name = "typedb_dependencies",
        remote = "https://github.com/typedb/typedb-dependencies",
        commit = "badb8e50302e3d40304dc987203e82b7cb3a85ed", # sync-marker: do not remove this comment, this is used for sync-dependencies by @typedb_dependencies
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
        commit = "7e2a5b149b48b76fc029b0c883b7b28b2550f27b",  # sync-marker: do not remove this comment, this is used for sync-dependencies by @typeql
    )

def typedb_driver():
    git_repository(
        name = "typedb_driver",
        remote = "https://github.com/typedb/typedb-driver",
        commit = "bcf1ab990b0bd9da341501a80bb7cebb0cfffe2f",  # sync-marker: do not remove this comment, this is used for sync-dependencies by @typedb_driver
    )
