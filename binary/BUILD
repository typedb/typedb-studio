# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.
#

load("@typedb_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")
load("@typedb_bazel_distribution//common:rules.bzl", "assemble_targz")

package(default_visibility = ["//:__pkg__"])

exports_files(["typedb-studio"])

assemble_targz(
    name = "assemble-bash-targz",
    additional_files = {
        "//binary:typedb-studio": 'typedb-studio',
    },
    permissions = {
        "typedb-studio": "0755",
    },
    visibility = ["//visibility:public"]
)

checkstyle_test(
    name = "checkstyle",
    include = glob(["*"]),
    license_type = "mpl-header",
)
