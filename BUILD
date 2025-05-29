# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

load("@typedb_dependencies//distribution:deployment.bzl", "deployment")
load("@typedb_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")
load("@typedb_bazel_distribution//common:rules.bzl", "assemble_targz", "unzip_file", "checksum")
load("@typedb_bazel_distribution//brew:rules.bzl", "deploy_brew")
load("@typedb_bazel_distribution//apt:rules.bzl", "deploy_apt")
load("@io_bazel_rules_kotlin//kotlin/internal:toolchains.bzl", "define_kt_toolchain")
load("@typedb_bazel_distribution//artifact:rules.bzl", "artifact_extractor", "deploy_artifact")
load("@typedb_bazel_distribution//platform:constraints.bzl", "constraint_linux_arm64", "constraint_linux_x86_64",
     "constraint_mac_arm64", "constraint_mac_x86_64", "constraint_win_x86_64")
load("@io_bazel_rules_kotlin//kotlin:core.bzl", "define_kt_toolchain")

package(default_visibility = ["//test/integration:__subpackages__"])

exports_files(["VERSION"])
#
#kt_jvm_library(
#    name = "studio",
#    srcs = glob(["*.kt"]),
#    plugins = ["@typedb_dependencies//builder/compose:compiler_plugin"],
#    deps = [
#        "//framework/common:common",
#        "//framework/material:material",
#        "//module/connection:connection",
#        "//module/preference:preference",
#        "//module/project:project",
#        "//module/role:role",
#        "//module/rule:rule",
##        "//module/type:type",
#        "//module/user:user",
#        "//module:module",
#        "//resources/version:version",
#        "//service/common:common",
#        "//service/connection:connection",
#        "//service/project:project",
#        "//service/page:page",
##        "//service/schema:schema",
#        "//service:service",
#
#        # External TypeDB Dependencies
#        "@typedb_dependencies//common/java:typedb-common",
#        "@typedb_driver//java:driver-java",
#                "@typedb_driver//java/api:api",
#                "@typedb_driver//java/common:common",
#                "@typedb_driver//java/concept:concept", #TODO: remove this after debugging
#                "@typedb_driver//java/connection:connection", #TODO: remove this after debugging
#
#        # External Maven Dependencies
#        "@maven//:io_github_microutils_kotlin_logging_jvm",
#        "@maven//:io_sentry_sentry",
#        "@maven//:org_jetbrains_compose_foundation_foundation_desktop",
#        "@maven//:org_jetbrains_compose_foundation_foundation_layout_desktop",
#        "@maven//:org_jetbrains_compose_runtime_runtime_desktop",
#        "@maven//:org_jetbrains_compose_ui_ui_desktop",
#        "@maven//:org_jetbrains_compose_ui_ui_graphics_desktop",
#        "@maven//:org_jetbrains_compose_ui_ui_text_desktop",
#        "@maven//:org_jetbrains_compose_ui_ui_unit_desktop",
#        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core_jvm",
#        "@maven//:org_slf4j_slf4j_api",
#    ],
#    resources = ["//resources/icons/typedb:typedb-bot-32px"],
#    tags = ["maven_coordinates=com.typedb:typedb-studio:{pom_version}"],
#)

genrule(
    name = "native-artifact-mac-x86_64-dmg",
    outs = ["typedb-studio_3.4.0-e5a9c7d3f1b8426e8d3c4f7a7b9a1e34c1f5d27b_amd64.dmg"],  # replace with your actual output, e.g., .exe, .AppImage
    cmd = """
        cp src-tauri/target/release/bundle/dmg/*.dmg "$(OUTS)"
    """,
    tags = ["local"],
    target_compatible_with = constraint_mac_x86_64,
)

genrule(
    name = "native-artifact-mac-arm64-dmg",
    outs = ["typedb-studio_3.4.0-e5a9c7d3f1b8426e8d3c4f7a7b9a1e34c1f5d27b_aarch64.dmg"],  # replace with your actual output, e.g., .exe, .AppImage
    cmd = """
        cp src-tauri/target/release/bundle/dmg/*.dmg "$(OUTS)"
    """,
    tags = ["local"],
    target_compatible_with = constraint_mac_arm64,
)

genrule(
    name = "native-artifact-linux-arm64-appimage",
    outs = ["typedb-studio_3.4.0-e5a9c7d3f1b8426e8d3c4f7a7b9a1e34c1f5d27b_arm64.deb"],  # replace with your actual output, e.g., .exe, .AppImage
    cmd = """
        cp src-tauri/target/release/bundle/deb/*.deb "$(OUTS)"
    """,
    tags = ["local"],
    target_compatible_with = constraint_linux_arm64,
)

genrule(
    name = "native-artifact-linux-x86_64-appimage",
    outs = ["typedb-studio_3.4.0-e5a9c7d3f1b8426e8d3c4f7a7b9a1e34c1f5d27b_amd64.AppImage"],  # replace with your actual output, e.g., .exe, .AppImage
    cmd = """
        cp src-tauri/target/release/bundle/deb/*.deb "$(OUTS)"
    """,
    tags = ["local"],
    target_compatible_with = constraint_linux_x86_64,
)

genrule(
    name = "native-artifact-windows-x86_64-msi",
    outs = ["typedb-studio_3.4.0-e5a9c7d3f1b8426e8d3c4f7a7b9a1e34c1f5d27b_amd64.msi"],  # replace with your actual output, e.g., .exe, .AppImage
    cmd = """
        cp src-tauri/target/release/bundle/msi/*.msi "$(OUTS)"
    """,
    tags = ["local"],
    target_compatible_with = constraint_win_x86_64,
)

define_kt_toolchain(
    name = "kotlin_toolchain_strict_deps",
    api_version = "1.7",
    language_version = "1.7",
    experimental_strict_kotlin_deps = "error",
)

#java_binary(
#    name = "studio-bin-linux-arm64",
#    main_class = "com.typedb.studio.Studio",
#    runtime_deps = [
#        "//:studio",
#        "@maven//:org_jetbrains_skiko_skiko_awt_runtime_linux_arm64",
#    ],
#    classpath_resources = ["//config/logback:logback-test-xml"],
#    target_compatible_with = constraint_linux_arm64,
#)
#
#java_binary(
#    name = "studio-bin-linux-x86_64",
#    main_class = "com.typedb.studio.Studio",
#    runtime_deps = [
#        "//:studio",
#        "@maven//:org_jetbrains_skiko_skiko_awt_runtime_linux_x64",
#    ],
#    classpath_resources = ["//config/logback:logback-test-xml"],
#    target_compatible_with = constraint_linux_x86_64,
#)
#
#java_binary(
#    name = "studio-bin-mac-arm64",
#    main_class = "com.typedb.studio.Studio",
#    runtime_deps = [
#        "//:studio",
#        "@maven//:org_jetbrains_skiko_skiko_awt_runtime_macos_arm64",
#    ],
#    classpath_resources = ["//config/logback:logback-test-xml"],
#    target_compatible_with = constraint_mac_arm64,
#)
#
#java_binary(
#    name = "studio-bin-mac-x86_64",
#    main_class = "com.typedb.studio.Studio",
#    runtime_deps = [
#        "//:studio",
#        "@maven//:org_jetbrains_skiko_skiko_awt_runtime_macos_x64",
#    ],
#    classpath_resources = ["//config/logback:logback-test-xml"],
#    target_compatible_with = constraint_mac_x86_64,
#)
#
#java_binary(
#    name = "studio-bin-windows-x86_64",
#    main_class = "com.typedb.studio.Studio",
#    runtime_deps = [
#        "//:studio",
#        "@maven//:org_jetbrains_skiko_skiko_awt_runtime_windows_x64",
#    ],
#    classpath_resources = ["//config/logback:logback-test-xml"],
#    target_compatible_with = constraint_win_x86_64,
#)

#assemble_jvm_platform(
#    name = "assemble-platform",
#    image_name = "TypeDB Studio",
#    image_filename = "typedb-studio-" + select({
#        "@typedb_bazel_distribution//platform:is_mac_arm64": "mac-arm64",
#        "@typedb_bazel_distribution//platform:is_mac_x86_64": "mac-x86_64",
#        "@typedb_bazel_distribution//platform:is_linux_arm64": "linux-arm64",
#        "@typedb_bazel_distribution//platform:is_linux_x86_64": "linux-x86_64",
#        "@typedb_bazel_distribution//platform:is_windows_x86_64": "windows-x86_64",
#        "//conditions:default": "INVALID",
#    }),
#    description = "TypeDB's Integrated Development Environment",
#    vendor = "TypeDB",
#    copyright = "Copyright (C) 2024 TypeDB",
#    license_file = ":LICENSE",
#    version_file = ":VERSION",
#    icon = select({
#        "@typedb_bazel_distribution//platform:is_mac": "//resources/icons/typedb:typedb-bot-mac",
#        "@typedb_bazel_distribution//platform:is_linux": "//resources/icons/typedb:typedb-bot-linux",
#        "@typedb_bazel_distribution//platform:is_windows": "//resources/icons/typedb:typedb-bot-windows",
#        "//conditions:default": "mac",
#    }),
#    java_deps = ":assemble-deps",
#    java_deps_root = "lib/",
#    main_jar_path = "com-typedb-typedb-studio-0.0.0.jar",
#    main_class = "com.typedb.studio.Studio",
#    additional_files = assemble_files,
#    verbose = True,
#    linux_app_category = "database",
#    linux_menu_group = "Utility;Development;IDE;",
#    mac_app_id = "com.typedb.studio",
#    mac_entitlements = "//config/mac:entitlements-mac-plist",
#    mac_code_signing_cert = "@vaticle_apple_developer_id_application_cert//file",
#    mac_deep_sign_jars_regex = ".*(io-netty-netty|typedb-typedb-driver).*",
#    windows_menu_group = "TypeDB Studio",
#)

assemble_files = {
    "//:LICENSE": "LICENSE",
}

# A little misleading. Because of the way our java_deps target is generated, this will actually produce a Mac runner
# if built on Mac, and fail to produce anything useful if built on Windows.
#assemble_targz(
#    name = "assemble-linux-targz",
#    targets = [":assemble-deps", "//binary:assemble-bash-targz"],
#    additional_files = assemble_files,
#    output_filename = "typedb-studio-linux",
#    visibility = ["//:__pkg__"],
#)

genrule(
    name = "invalid-checksum",
    outs = ["invalid-checksum.txt"],
    srcs = [],
    cmd = "echo > $@",
)

deploy_artifact(
    name = "deploy-mac-x86_64-dmg",
    target = ":native-artifact-mac-x86_64-dmg",
    artifact_group = "typedb-studio-mac-x86_64",
    artifact_name = "typedb-studio-mac-x86_64-{version}.dmg",
    snapshot = deployment['artifact']['snapshot']['upload'],
    release = deployment['artifact']['release']['upload'],
    version_file = ":VERSION",
    visibility = ["//visibility:public"],
    target_compatible_with = constraint_mac_x86_64,
)

deploy_artifact(
    name = "deploy-mac-arm64-dmg",
    target = ":native-artifact-mac-arm64-dmg",
    artifact_group = "typedb-studio-mac-arm64",
    artifact_name = "typedb-studio-mac-arm64-{version}.dmg",
    snapshot = deployment['artifact']['snapshot']['upload'],
    release = deployment['artifact']['release']['upload'],
    version_file = ":VERSION",
    visibility = ["//visibility:public"],
    target_compatible_with = constraint_mac_arm64,
)

deploy_artifact(
    name = "deploy-linux-x86_64-appimage",
    target = ":native-artifact-linux-x86_64-appimage",
    artifact_group = "typedb-studio-linux-x86_64",
    artifact_name = "typedb-studio-linux-x86_64-{version}.AppImage",
    snapshot = deployment['artifact']['snapshot']['upload'],
    release = deployment['artifact']['release']['upload'],
    version_file = ":VERSION",
    visibility = ["//visibility:public"],
    target_compatible_with = constraint_linux_x86_64,
)

deploy_artifact(
    name = "deploy-linux-arm64-appimage",
    target = ":native-artifact-linux-arm64-appimage",
    artifact_group = "typedb-studio-linux-arm64",
    artifact_name = "typedb-studio-linux-arm64-{version}.AppImage",
    snapshot = deployment['artifact']['snapshot']['upload'],
    release = deployment['artifact']['release']['upload'],
    version_file = ":VERSION",
    visibility = ["//visibility:public"],
    target_compatible_with = constraint_linux_arm64,
)

deploy_artifact(
    name = "deploy-windows-x86_64-msi",
    target = ":native-artifact-windows-x86_64-msi",
    artifact_group = "typedb-studio-windows-x86_64",
    artifact_name = "typedb-studio-windows-x86_64-{version}.msi",
    snapshot = deployment['artifact']['snapshot']['upload'],
    release = deployment['artifact']['release']['upload'],
    version_file = ":VERSION",
    visibility = ["//visibility:public"],
    target_compatible_with = constraint_win_x86_64,
)

label_flag(
    name = "checksum-mac-arm64",
    build_setting_default = ":invalid-checksum",
)

label_flag(
    name = "checksum-mac-x86_64",
    build_setting_default = ":invalid-checksum",
)

deploy_brew(
    name = "deploy-brew",
    snapshot = deployment['brew']['snapshot'],
    release = deployment['brew']['release'],
    formula = "//config/brew:typedb-studio.rb",
    file_substitutions = {
        "//:checksum-mac-arm64": "{sha256-arm64}",
        "//:checksum-mac-x86_64": "{sha256-x86_64}",
    },
    version_file = "//:VERSION",
    type = "cask",
)

deploy_apt(
    name = "deploy-apt-x86_64",
    target = ":native-artifact-linux-x86_64-appimage",
    snapshot = deployment['apt']['snapshot']['upload'],
    release = deployment['apt']['release']['upload'],
)

deploy_apt(
    name = "deploy-apt-arm64",
    target = ":native-artifact-linux-arm64-appimage",
    snapshot = deployment['apt']['snapshot']['upload'],
    release = deployment['apt']['release']['upload'],
)

checkstyle_test(
    name = "checkstyle",
    include = glob([
        "*",
        ".factory/*",
        ".circleci/**",
    ]),
    exclude = glob([
        "*.md",
        ".bazelversion",
        ".circleci/windows/*",
        "LICENSE",
        "VERSION",
        ".bazel-cache-credential.json",
        ".bazel-remote-cache.rc"
    ]),
    license_type = "mpl-header",
)

checkstyle_test(
    name = "checkstyle-license",
    include = ["LICENSE"],
    license_type = "mpl-fulltext",
)

# Tools to be built during `bazel build //...`
filegroup(
    name = "tools",
    data = [
        "@typedb_dependencies//distribution/artifact:create-netrc",
        "@typedb_dependencies//tool/bazelinstall:remote_cache_setup.sh",
        "@typedb_dependencies//tool/checkstyle:test-coverage",
        "@typedb_dependencies//tool/release/notes:create",
    ],
)
