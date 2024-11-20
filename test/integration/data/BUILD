# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

load("@typedb_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")
load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")

package(default_visibility = ["//test/integration:__subpackages__"])

skiko_runtime_platform = select({
     "@typedb_bazel_distribution//platform:is_mac": ["@maven//:org_jetbrains_skiko_skiko_awt_runtime_macos_x64"],
     "@typedb_bazel_distribution//platform:is_linux": ["@maven//:org_jetbrains_skiko_skiko_awt_runtime_linux_x64"],
     "@typedb_bazel_distribution//platform:is_windows": ["@maven//:org_jetbrains_skiko_skiko_awt_runtime_windows_x64"],
     "//conditions:default": ["INVALID"]})

kt_jvm_library(
    name = "paths",
    srcs = ["Paths.kt"],
    plugins = ["@typedb_dependencies//builder/compose:compiler_plugin"],
    runtime_deps = skiko_runtime_platform,
    deps = [
        # External Maven Dependencies
        "@maven//:org_jetbrains_compose_runtime_runtime_desktop",
    ],
)


filegroup(
    name = "sample-file-structure-files",
    srcs = glob(["sample_file_structure/*", "sample_file_structure/*/*"]),
    visibility = ["//test/integration:__subpackages__"],
)

filegroup(
    name = "sample-github-data-files",
    srcs = [
        "sample_github_data/github_collaborators_query.tql",
        "sample_github_data/github_schema.tql",
        "sample_github_data/github_data.tql",
    ],
    visibility = ["//test/integration:__subpackages__"],
)

checkstyle_test(
    name = "checkstyle",
    include = glob(["*", "sample_github_data/*"]),
    exclude = glob(["sample_file_structure/*", "sample_file_structure/*/*"]),
    license_type = "mpl-header",
)
