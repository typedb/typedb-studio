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

load("//:deployment.bzl", deployment_github = "deployment")
load("//:rules.bzl", "jvm_application_image")
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_binary", "kt_jvm_library")
load("@rules_pkg//:pkg.bzl", "pkg_zip")
load("@vaticle_dependencies//distribution:deployment.bzl", "deployment")
load("@vaticle_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")
load("@vaticle_bazel_distribution//common:rules.bzl", "checksum", "assemble_targz", "assemble_zip", "java_deps", "assemble_versioned")
load("@vaticle_bazel_distribution//common/tgz2zip:rules.bzl", "tgz2zip")
load("@vaticle_bazel_distribution//github:rules.bzl", "deploy_github")
load("@vaticle_bazel_distribution//brew:rules.bzl", "deploy_brew")
load("@io_bazel_rules_kotlin//kotlin/internal:toolchains.bzl", "define_kt_toolchain")

kt_jvm_library(
    name = "studio",
    srcs = [
        "Studio.kt",
    ],
    kotlin_compiler_plugin = "@org_jetbrains_compose_compiler//file",
    deps = [
        "//common",
        "//connection",
        "//navigator",
        "//notification",
        "//page",
        "//service",
        "//statusbar",
        "//toolbar",

        # External Maven Dependencies
        "@maven//:ch_qos_logback_logback_classic",
        "@maven//:ch_qos_logback_logback_core",
        "@maven//:io_github_microutils_kotlin_logging_jvm",
        "@maven//:org_jetbrains_compose_desktop_desktop_jvm",
        "@maven//:org_jetbrains_compose_foundation_foundation_desktop",
        "@maven//:org_jetbrains_compose_foundation_foundation_layout_desktop",
        "@maven//:org_jetbrains_compose_runtime_runtime_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_geometry_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_graphics_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_text_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_unit_desktop",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core",
        "@maven//:org_slf4j_slf4j_api",
    ],
    resources = [
        "//resources/fonts:titillium-web",
        "//resources/fonts:ubuntu-mono",
        "//resources/icons:icons",
    ],
    resource_strip_prefix = "resources",
    tags = ["maven_coordinates=com.vaticle.typedb:studio:{pom_version}"],
)

java_binary(
    name = "studio-bin-mac",
    main_class = "com.vaticle.typedb.studio.Studio",
    runtime_deps = [
        ":studio",
        "@maven//:org_jetbrains_skiko_skiko_jvm_runtime_macos_x64",
    ],
    classpath_resources = ["//resources:logback-test-xml"],
)

java_binary(
    name = "studio-bin-windows",
    main_class = "com.vaticle.typedb.studio.Studio",
    runtime_deps = [
        ":studio",
        "@maven//:org_jetbrains_skiko_skiko_jvm_runtime_windows_x64",
    ],
    classpath_resources = ["//resources:logback-test-xml"],
)

java_binary(
    name = "studio-bin-linux",
    main_class = "com.vaticle.typedb.studio.Studio",
    runtime_deps = [
        ":studio",
        "@maven//:org_jetbrains_skiko_skiko_jvm_runtime_linux_x64",
    ],
    classpath_resources = ["//resources:logback-test-xml"],
)

assemble_files = {
    "//resources:logback-xml": "logback.xml",
    "//:LICENSE": "LICENSE",
}

kt_jvm_library(
    name = "jvm-application-image-builder-lib",
    srcs = ["JVMApplicationImageBuilder.kt"],
    deps = ["@maven//:org_zeroturnaround_zt_exec"],
)

java_binary(
    name = "jvm-application-image-builder-bin",
    runtime_deps = [":jvm-application-image-builder-lib"],
    main_class = "com.vaticle.typedb.studio.JVMApplicationImageBuilderKt",
)

# TODO: Create a MANIFEST file in the jvm_binary and read it to determine the main jar and main class
jvm_application_image(
    name = "application-image",
    application_name = "TypeDB Studio",
    icon_mac = "//resources/icons/application:vaticle-bot-mac",
    icon_linux = "//resources/icons/application:vaticle-bot-linux",
    icon_windows = "//resources/icons/application:vaticle-bot-windows",
    filename = "typedb-studio-" + select({
        "@vaticle_dependencies//util/platform:is_mac": "mac",
        "@vaticle_dependencies//util/platform:is_linux": "linux",
        "@vaticle_dependencies//util/platform:is_windows": "windows",
        "//conditions:default": "mac",
    }),
    version_file = ":VERSION",
    jvm_binary = select({
        "@vaticle_dependencies//util/platform:is_mac": ":studio-bin-mac",
        "@vaticle_dependencies//util/platform:is_linux": ":studio-bin-linux",
        "@vaticle_dependencies//util/platform:is_windows": ":studio-bin-windows",
        "//conditions:default": ":studio-bin-mac",
    }),
    main_jar = "com-vaticle-typedb-studio-0.0.0.jar",
    main_class = "com.vaticle.typedb.studio.Studio",
    deps_use_maven_name = False,
    additional_files = assemble_files,
    mac_entitlements = "//resources:entitlements-mac-plist",
    mac_code_signing_cert = "@vaticle_apple_developer_id_application_cert//file",
)

# A little misleading. Because of the way our java_deps target is generated, this will actually produce a Mac runner
# if built on Mac, and fail to produce anything useful if built on Windows.
assemble_targz(
    name = "linux-java-binary",
    targets = [":application-image-deps", "//binary:assemble-bash-targz"],
    additional_files = assemble_files,
    output_filename = "typedb-studio-linux-java-binary",
    visibility = ["//:__pkg__"]
)

deploy_github(
    name = "deploy-github",
    organisation = deployment_github['github.organisation'],
    repository = deployment_github['github.repository'],
    title = "TypeDB Studio",
    title_append_version = True,
    release_description = "//:RELEASE_TEMPLATE.md",
    archive = ":application-image",
#    archive = ":hello-bundle",
    version_file = ":VERSION",
    draft = False
)

deploy_brew(
    name = "deploy-brew",
    snapshot = deployment['brew.snapshot'],
    release = deployment['brew.release'],
    formula = "//config/brew:typedb-studio.rb",
#    checksum = "//:checksum",
    version_file = "//:VERSION",
    type = "cask",
)

checkstyle_test(
    name = "checkstyle",
    include = glob(["*", ".grabl/*", ".circleci/**"]),
    exclude = glob(["docs/*", ".circleci/windows/*"]),
    license_type = "agpl",
)

# CI targets that are not declared in any BUILD file, but are called externally
filegroup(
    name = "ci",
    data = [
        "@vaticle_dependencies//tool/checkstyle:test-coverage",
        "@vaticle_dependencies//tool/release:create-notes",
        "@vaticle_dependencies//tool/bazelrun:rbe",
    ],
)
