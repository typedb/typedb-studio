# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

load("@typedb_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")
load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

package(default_visibility = [
    "//framework:__pkg__",
    "//:__pkg__"
])

kt_jvm_library(
    name = "rule",
    srcs = glob(["*.kt"]),
    plugins = ["@typedb_dependencies//builder/compose:compiler_plugin"],
    deps = [
        "//service/common:common",
        "//framework/common:common",
        "//framework/material:material",

        # External Vaticle Dependencies

        # External Maven Dependencies
        "@maven//:io_github_microutils_kotlin_logging_jvm",
        "@maven//:org_jetbrains_compose_runtime_runtime_desktop",
        "@maven//:org_slf4j_slf4j_api",
    ],
    tags = ["maven_coordinates=com.typedb:typedb-studio-module-rule:{pom_version}"],
)

checkstyle_test(
    name = "checkstyle",
    include = glob(["*"]),
    license_type = "mpl-header",
)
