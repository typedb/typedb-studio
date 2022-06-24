#
# Copyright (C) 2021 Vaticle
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.
#

load("@vaticle_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_test")

package(default_visibility = [
    "//view:__subpackages__",
    "//:__pkg__"
])

kt_jvm_library(
    name = "common",
    srcs = glob(["*.kt", "*/*.kt"], exclude = ["*/*Test.kt"]),
    kotlin_compiler_plugin = "@org_jetbrains_compose_compiler//file",
    deps = [
        "//state/common:common",

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
    tags = ["maven_coordinates=com.vaticle.typedb:typedb-studio-view-common:{pom_version}"],
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
    test_class = "com.vaticle.typedb.studio.view.common.geometry.GeometryTest",
)

checkstyle_test(
    name = "checkstyle",
    include = glob(["*", "*/*"]),
    license_type = "agpl",
)
