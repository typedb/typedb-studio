# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

load("@typedb_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")
load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

package(default_visibility = [
    "//framework:__subpackages__",
    "//module:__subpackages__",
    "//service:__subpackages__",
    "//:__pkg__",
    "//test/integration:__subpackages__"
])

kt_jvm_library(
    name = "connection",
    srcs = glob(["*.kt"]),
    deps = [
        "//service/common:common",

        # External Vaticle Dependencies
        "@typedb_driver//java:driver-java",
        "@typedb_driver//java/api:api",
        "@typedb_driver//java/common:common",
        "@typedb_driver//java/concept:concept", #TODO: remove this after debugging
        "@typedb_driver//java/connection:connection", #TODO: remove this after debugging
        "@typedb_dependencies//common/java:typedb-common",
        "@typeql//java/query:query",
        "@typeql//java/pattern:pattern",
        "@typeql//java:typeql-lang",

        # External Maven Dependencies
        "@maven//:io_github_microutils_kotlin_logging_jvm",
        "@maven//:org_jetbrains_compose_runtime_runtime_desktop",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core_jvm",
        "@maven//:org_slf4j_slf4j_api",
    ],
    tags = ["maven_coordinates=com.typedb:typedb-studio-state-connection:{pom_version}"],
)

checkstyle_test(
    name = "checkstyle",
    include = glob(["*", "*/*"]),
    license_type = "mpl-header",
)
