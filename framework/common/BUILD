# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

load("@vaticle_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")
load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library", "kt_jvm_test")

package(default_visibility = [
    "//module:__subpackages__",
    "//framework:__subpackages__",
    "//test/integration:__subpackages__",
    "//:__pkg__",
])

kt_jvm_library(
    name = "common",
    srcs = glob(["*.kt", "*/*.kt"], exclude = ["*/*Test.kt"]),
    plugins = ["@vaticle_dependencies//builder/compose:compiler_plugin"],
    deps = [
        "//service/common:common",

        # External Maven Dependencies
        "@maven//:io_github_microutils_kotlin_logging_jvm",
        "@maven//:org_jetbrains_compose_foundation_foundation_desktop",
        "@maven//:org_jetbrains_compose_material_material_desktop",
        "@maven//:org_jetbrains_compose_runtime_runtime_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_geometry_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_graphics_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_text_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_unit_desktop",
        "@maven//:org_jetbrains_skiko_skiko_awt",
        "@maven//:org_slf4j_slf4j_api",
    ],
    resources = [
        "//resources/fonts/monaco:monaco",
        "//resources/fonts/titilliumweb:titillium-web",
        "//resources/fonts/ubuntumono:ubuntu-mono",
        "//resources/icons/fontawesome:icons",
    ],
    tags = ["maven_coordinates=com.vaticle.typedb:typedb-studio-framework-common:{pom_version}"],
)

kt_jvm_test(
    name = "test-geometry",
    srcs = [
        "geometry/GeometryTest.kt",
    ],
    deps = [
        ":common",

        "@maven//:junit_junit",
        "@maven//:org_jetbrains_compose_ui_ui_geometry_desktop",
        "@maven//:org_jetbrains_kotlin_kotlin_test",
    ],
    test_class = "com.typedb.studio.framework.common.geometry.GeometryTest",
)

checkstyle_test(
    name = "checkstyle",
    include = glob(["*", "*/*"]),
    license_type = "mpl-header",
)
