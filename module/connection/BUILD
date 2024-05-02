# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

load("@vaticle_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")
load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

package(default_visibility = [
    "//framework:__subpackages__",
    "//module:__pkg__",
    "//:__pkg__"
])

kt_jvm_library(
    name = "connection",
    srcs = glob(["*.kt"]),
    plugins = ["@vaticle_dependencies//builder/compose:compiler_plugin"],
    deps = [
        "//service/common:common",
        "//service/connection:connection",
        "//service/page:page",
        "//service/project:project",
        "//service:service",
        "//framework/common:common",
        "//framework/material:material",

        # External Maven Dependencies
        "@maven//:org_jetbrains_compose_foundation_foundation_desktop",
        "@maven//:org_jetbrains_compose_foundation_foundation_layout_desktop",
        "@maven//:org_jetbrains_compose_runtime_runtime_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_graphics_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_text_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_unit_desktop",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core_jvm",
        "@maven//:org_slf4j_slf4j_api",
    ],
    resource_strip_prefix = "resources",
    tags = ["maven_coordinates=com.vaticle.typedb:typedb-studio-module-connection:{pom_version}"],
)

checkstyle_test(
    name = "checkstyle",
    include = glob(["*"]),
    license_type = "mpl-header",
)
