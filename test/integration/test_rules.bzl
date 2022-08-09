#
# Copyright (C) 2022 Vaticle
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

load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_test")

skiko_runtime_platform = select({
     "@vaticle_dependencies//util/platform:is_mac": ["@maven//:org_jetbrains_skiko_skiko_awt_runtime_macos_x64"],
     "@vaticle_dependencies//util/platform:is_linux": ["@maven//:org_jetbrains_skiko_skiko_awt_runtime_linux_x64"],
     "@vaticle_dependencies//util/platform:is_windows": ["@maven//:org_jetbrains_skiko_skiko_awt_runtime_windows_x64"],
     "//conditions:default": ["INVALID"]})


def studio_test(name, class_srcs, data, test_class_path, deps):
    kt_jvm_test(
        name = name,
        srcs = class_srcs,
        data = data,
        kotlin_compiler_plugin = "@org_jetbrains_compose_compiler//file",
        test_class = test_class_path,
        runtime_deps = skiko_runtime_platform,
        deps = [
            "test-utils",

            # External Maven Dependencies
            "@maven//:junit_junit",
            "@maven//:org_jetbrains_compose_foundation_foundation_desktop",
            "@maven//:org_jetbrains_compose_foundation_foundation_layout_desktop",
            "@maven//:org_jetbrains_compose_material_material_desktop",
            "@maven//:org_jetbrains_compose_runtime_runtime_desktop",
            "@maven//:org_jetbrains_compose_ui_ui_desktop",
            "@maven//:org_jetbrains_compose_ui_ui_graphics_desktop",
            "@maven//:org_jetbrains_compose_ui_ui_test_desktop",
            "@maven//:org_jetbrains_compose_ui_ui_test_junit4_desktop",
            "@maven//:org_jetbrains_compose_ui_ui_text_desktop",
            "@maven//:org_jetbrains_compose_ui_ui_unit_desktop",
            "@maven//:org_jetbrains_kotlin_kotlin_test",
            "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core_jvm",
            "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_test",
            "@maven//:org_jetbrains_skiko_skiko_awt",
        ] + deps,
    )