# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

load("@vaticle_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")
load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

package(default_visibility = [
    "//module/project:__pkg__",
    "//framework/output:__pkg__",
])

kt_jvm_library(
    name = "graph",
    srcs = glob(["*.kt"]),
    plugins = ["@vaticle_dependencies//builder/compose:compiler_plugin"],
    deps = [
        "//service/common:common",
        "//service/connection:connection",
        "//service:service",
        "//framework/common:common",
        "//framework/material:material",

        # External Vaticle Dependencies
        "@vaticle_force_graph//:force_graph",
        "@vaticle_typeql//common/java:common",
        "@vaticle_typedb_driver//java/api:api",
        "@vaticle_typedb_driver//java/common:common",
        "@vaticle_typeql//java:typeql-lang",
        "@vaticle_typeql//java/pattern:pattern",
        "@vaticle_typeql//java/query:query",

        # External Maven Dependencies
        "@maven//:io_github_microutils_kotlin_logging_jvm",
        "@maven//:org_jetbrains_compose_animation_animation_core_desktop",
        "@maven//:org_jetbrains_compose_foundation_foundation_desktop",
        "@maven//:org_jetbrains_compose_foundation_foundation_layout_desktop",
        "@maven//:org_jetbrains_compose_runtime_runtime_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_geometry_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_graphics_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_text_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_unit_desktop",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core_jvm",
        "@maven//:org_jetbrains_skiko_skiko_awt",
        "@maven//:org_slf4j_slf4j_api",
    ],
    tags = ["maven_coordinates=com.vaticle.typedb:typedb-studio-framework-graph:{pom_version}"],
)

checkstyle_test(
    name = "checkstyle",
    include = glob(["*"]),
    license_type = "mpl-header",
)
