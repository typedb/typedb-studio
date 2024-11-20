# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

load("@typedb_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")
load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

package(default_visibility = ["//test/integration:__pkg__"])

skiko_runtime_platform = select({
    "@typedb_bazel_distribution//platform:is_linux_arm64": ["@maven//:org_jetbrains_skiko_skiko_awt_runtime_linux_arm64"],
    "@typedb_bazel_distribution//platform:is_linux_x86_64": ["@maven//:org_jetbrains_skiko_skiko_awt_runtime_linux_x64"],
    "@typedb_bazel_distribution//platform:is_mac_arm64": ["@maven//:org_jetbrains_skiko_skiko_awt_runtime_macos_arm64"],
    "@typedb_bazel_distribution//platform:is_mac_x86_64": ["@maven//:org_jetbrains_skiko_skiko_awt_runtime_macos_x64"],
    "@typedb_bazel_distribution//platform:is_windows": ["@maven//:org_jetbrains_skiko_skiko_awt_runtime_windows_x64"],
    "//conditions:default": ["INVALID"],
})

kt_jvm_library(
    name = "common",
    srcs = glob(["*.kt"]),
    plugins = ["@typedb_dependencies//builder/compose:compiler_plugin"],
    runtime_deps = skiko_runtime_platform,
    deps = [
        "//:studio",
        "//framework/material",
        "//framework/common",
        "//service",
        "//service/common",
        "//service/connection",
        "//service/project",
        "//service/page",

        # External TypeDB Dependencies
        "@typedb_driver//java/api:api",
        "@typedb_driver//java:driver-java",
        "@typedb_dependencies//common/java:typedb-common",
        "@typeql//java:typeql-lang",
        "@typeql//java/common:common",
        "@typeql//java/query:query",

        # External Maven Dependencies
#        "@maven//:com_vaticle_typedb_typedb_runner",
#        "@maven//:com_vaticle_typedb_typedb_cloud_runner",
        "@maven//:junit_junit",
        "@maven//:org_jetbrains_compose_runtime_runtime_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_test_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_test_junit4_desktop",
        "@maven//:org_jetbrains_kotlin_kotlin_test",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core_jvm",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_test",
        "@maven//:org_slf4j_slf4j_api",
    ],
)

checkstyle_test(
    name = "checkstyle",
    include = glob(["*"]),
    license_type = "mpl-header",
)
