# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

load("@typedb_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")
load("@typedb_dependencies//distribution:deployment.bzl", "deployment")
load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

package(default_visibility = ["//visibility:public"])

genrule(
    name = "version-src",
    srcs = [
        "templates/Version.kt",
        "//:VERSION",
    ],
    cmd = "VERSION=`cat $(location //:VERSION)`;sed -e \"s/{version}/$$VERSION/g\" $(location templates/Version.kt) >> $@",
    outs = ["Version.kt"],
)

kt_jvm_library(
    name = "version",
    srcs = [":version-src"],
    tags = ["maven_coordinates=com.typedb:typedb-studio-resources-version:{pom_version}"],
)

checkstyle_test(
    name = "checkstyle",
    include = glob(["*", "templates/*"]),
    license_type = "mpl-header",
)
