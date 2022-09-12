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

load("@vaticle_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_test")
load("@vaticle_typedb_common//test:rules.bzl", "typedb_kt_test")

package(default_visibility = ["//:__pkg__"])

skiko_runtime_platform = select({
     "@vaticle_dependencies//util/platform:is_mac": ["@maven//:org_jetbrains_skiko_skiko_awt_runtime_macos_x64"],
     "@vaticle_dependencies//util/platform:is_linux": ["@maven//:org_jetbrains_skiko_skiko_awt_runtime_linux_x64"],
     "@vaticle_dependencies//util/platform:is_windows": ["@maven//:org_jetbrains_skiko_skiko_awt_runtime_windows_x64"],
     "//conditions:default": ["INVALID"]})

compose_test_libraries = [
    "//test/integration/common:common",
    "//test/integration:test-integration",

    "@maven//:junit_junit",
    "@maven//:org_jetbrains_compose_ui_ui_desktop",
    "@maven//:org_jetbrains_compose_ui_ui_graphics_desktop",
    "@maven//:org_jetbrains_compose_ui_ui_test_desktop",
    "@maven//:org_jetbrains_compose_ui_ui_test_junit4_desktop",
    "@maven//:org_jetbrains_compose_ui_ui_text_desktop",
    "@maven//:org_jetbrains_compose_ui_ui_unit_desktop",
    "@maven//:org_jetbrains_kotlin_kotlin_test",
    "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core_jvm",
    "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_test",
    "@maven//:org_jetbrains_skiko_skiko_awt"
]

kt_jvm_library(
    name = "test-integration",
    srcs = ["IntegrationTest.kt"],
    kotlin_compiler_plugin = "@org_jetbrains_compose_compiler//file",
    runtime_deps = skiko_runtime_platform,
    deps = [
        "//:studio",
        "//state",
        "//framework/common",

        # External Maven Dependencies
        "@maven//:junit_junit",
        "@maven//:org_jetbrains_compose_runtime_runtime_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_test_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_test_junit4_desktop",
        "@maven//:org_jetbrains_kotlin_kotlin_test",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core_jvm",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_test",
    ],
)

typedb_kt_test(
    name = "test-quickstart",
    srcs = ["QuickstartTest.kt"],
    data = ["//test/data:query-string-files"],
    test_class = "com.vaticle.typedb.studio.test.integration.QuickstartTest",
    runtime_deps = skiko_runtime_platform,
    deps = compose_test_libraries,
    server_mac_artifact = "@vaticle_typedb_artifact_mac//file",
    server_linux_artifact = "@vaticle_typedb_artifact_linux//file",
    server_windows_artifact = "@vaticle_typedb_artifact_windows//file",
)


kt_jvm_test(
    name = "test-projectbrowser",
    srcs = ["ProjectBrowserTest.kt"],
    data = ["//test/data:sample-file-structure-files"],
    test_class = "com.vaticle.typedb.studio.test.integration.ProjectBrowserTest",
    runtime_deps = skiko_runtime_platform,
    deps = compose_test_libraries + [
        "//framework/material",
        "//state",
        "//state/page",
        "//state/project",
    ],
)

typedb_kt_test(
    name = "test-texteditor",
    srcs = ["TextEditorTest.kt"],
    data = ["//test/data:sample-file-structure-files", "//test/data:query-string-files"],
    test_class= "com.vaticle.typedb.studio.test.integration.TextEditorTest",
    runtime_deps = skiko_runtime_platform,
    deps = compose_test_libraries + [
        "//framework/material",
        "//state",
        "//state/app",
        "//state/common",
        "//state/connection",
        "//state/project",
        "//state/page",
        "//state/schema",

        "@vaticle_typedb_client_java//api",
    ],
    server_mac_artifact = "@vaticle_typedb_artifact_mac//file",
    server_linux_artifact = "@vaticle_typedb_artifact_linux//file",
    server_windows_artifact = "@vaticle_typedb_artifact_windows//file",
)

typedb_kt_test(
    name = "test-typebrowser",
    srcs = ["TypeBrowserTest.kt"],
    data = ["//test/data:sample-file-structure-files", "//test/data:query-string-files"],
    test_class = "com.vaticle.typedb.studio.test.integration.TypeBrowserTest",
    runtime_deps = skiko_runtime_platform,
    deps = compose_test_libraries + [
        "//framework/material",
        "//state",
        "//state/app",
        "//state/common",
        "//state/connection",
        "//state/project",
        "//state/page",
        "//state/schema",

        "@vaticle_typedb_client_java//api",
    ],
    server_mac_artifact = "@vaticle_typedb_artifact_mac//file",
    server_linux_artifact = "@vaticle_typedb_artifact_linux//file",
    server_windows_artifact = "@vaticle_typedb_artifact_windows//file",
)

checkstyle_test(
    name = "checkstyle",
    include = glob(["*"]),
    license_type = "agpl-header",
)
