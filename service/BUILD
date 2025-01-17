# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

load("@typedb_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")
load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

package(default_visibility = [
    "//module:__subpackages__",
    "//framework:__subpackages__",
    "//:__pkg__",
    "//test/integration:__subpackages__",
])

kt_jvm_library(
    name = "service",
    srcs = glob(["*.kt"]),
    deps = [
        "//service/common:common",
        "//service/connection:connection",
        "//service/project:project",
        "//service/page:page",
#        "//service/schema:schema",
        "//service/user:user",

        # External Maven Dependencies
        "@maven//:io_github_microutils_kotlin_logging_jvm",
        "@maven//:org_jetbrains_compose_runtime_runtime_desktop",
        "@maven//:org_slf4j_slf4j_api",
    ],
    tags = ["maven_coordinates=com.typedb:typedb-studio-service:{pom_version}"],
)

checkstyle_test(
    name = "checkstyle",
    include = glob(["*", "*/*"]),
    license_type = "mpl-header",
)
