# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

load("@typedb_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")
load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

package(default_visibility = ["//framework/editor/highlighter:__pkg__"])

kt_jvm_library(
    name = "typeql",
    srcs = glob(["*.kt"]),
    plugins = ["@typedb_dependencies//builder/compose:compiler_plugin"],
    deps = [
        "//framework/editor/highlighter/common:common",

        # External Vaticle Dependencies
        "@typedb_dependencies//common/java:typedb-common",
        "@typeql//grammar/java:typeql-grammar",
        "@typeql//java:typeql-lang",

        # External Maven Dependencies
        "@maven//:io_github_microutils_kotlin_logging_jvm",
        "@maven//:org_antlr_antlr4_runtime",
        "@maven//:org_jetbrains_compose_runtime_runtime_desktop",
        "@maven//:org_slf4j_slf4j_api",
    ],
    resources = ["typeql_scopes.yml"],
    tags = ["maven_coordinates=com.typedb:typedb-studio-framework-editor-highlighter-typeql:{pom_version}"],
)

checkstyle_test(
    name = "checkstyle",
    include = glob(["*"]),
    license_type = "mpl-header",
)
