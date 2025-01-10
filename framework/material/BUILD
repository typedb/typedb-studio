# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

load("@typedb_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")
load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library", "kt_jvm_test")

package(default_visibility = [
    "//module:__subpackages__",
    "//framework:__subpackages__",
    "//:__pkg__",
    "//test/integration:__subpackages__"
])

kt_jvm_library(
    name = "material",
    srcs = glob(["*.kt", "*/*.kt"], exclude = ["*/*Test.kt"]),
    plugins = ["@typedb_dependencies//builder/compose:compiler_plugin"],
    deps = [
        "//service/common:common",
        "//service/page:page",
        "//service/project:project",
        "//service:service",
        "//framework/common:common",

        # External TypeDB Dependencies
        "@typedb_driver//java/api:api",
        "@typedb_driver//java/common:common",
        "@typedb_dependencies//common/java:typedb-common",

        # External Maven Dependencies
        "@maven//:org_jetbrains_compose_animation_animation_core_desktop",
        "@maven//:org_jetbrains_compose_foundation_foundation_desktop",
        "@maven//:org_jetbrains_compose_foundation_foundation_layout_desktop",
        "@maven//:org_jetbrains_compose_material_material_desktop",
        "@maven//:org_jetbrains_compose_runtime_runtime_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_geometry_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_graphics_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_text_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_unit_desktop",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core_jvm",
        "@maven//:org_slf4j_slf4j_api",
    ],
    tags = ["maven_coordinates=com.typedb:typedb-studio-framework-material:{pom_version}"],
)

checkstyle_test(
    name = "checkstyle",
    include = glob(["*", "*/*"]),
    license_type = "mpl-header",
)
