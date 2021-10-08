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

java_binary(
    name = "hello",
    srcs = ["Hello.java"],
    main_class = "com.vaticle.typedb.studio.Hello",
    deps = [
#        "@maven//:org_jetbrains_compose_material_material_desktop",
#        "@maven//:org_jetbrains_compose_material_material_icons_core_desktop"
#        "@maven//:org_jetbrains_compose_ui_ui_desktop",
    ],
)

java_deps(
    name = "hello-deps",
    target = ":hello",
    java_deps_root = "lib/",
    maven_name = False,
)

assemble_zip(
    name = "hello-bundle",
    targets = [":hello-deps"],
    output_filename = "hello-assemble",
)

# TODO: If we remove some of these deps, IntelliJ starts to complain - we should investigate
kt_jvm_library(
    name = "studio",
    srcs = ["main.kt"],
    kotlin_compiler_plugin = "@org_jetbrains_compose_compiler//file",
    deps = [
        "//appearance",
        "//ui/elements",
        "//data",
        "//login",
        "//navigation",
        "//workspace",

        # Maven
#        "@maven//:androidx_annotation_annotation_1_2_0",
#        "@maven//:androidx_annotation_annotation",
#        "@maven//:org_jetbrains_annotations_13_0",
#        "@maven//:org_jetbrains_annotations",
#        "@maven//:org_jetbrains_compose_ui_ui_tooling_preview_desktop_1_0_0_alpha3",
#        "@maven//:org_jetbrains_compose_desktop_desktop_jvm",
#        "@maven//:org_jetbrains_compose_ui_ui_tooling_preview_desktop",
#        "@maven//:org_jetbrains_compose_material_material_desktop",
#        "@maven//:org_jetbrains_compose_material_material_ripple_desktop",
#        "@maven//:org_jetbrains_compose_material_material_icons_core_desktop",
#        "@maven//:org_jetbrains_compose_foundation_foundation_desktop",
#        "@maven//:org_jetbrains_compose_animation_animation_desktop",
#        "@maven//:org_jetbrains_compose_foundation_foundation_layout_desktop",
#        "@maven//:org_jetbrains_compose_animation_animation_core_desktop",
#        "@maven//:org_jetbrains_compose_ui_ui_desktop",
#        "@maven//:org_jetbrains_compose_ui_ui_text_desktop",
#        "@maven//:org_jetbrains_compose_ui_ui_graphics_desktop",
#        "@maven//:org_jetbrains_compose_ui_ui_unit_desktop",
#        "@maven//:org_jetbrains_compose_ui_ui_geometry_desktop",
#        "@maven//:org_jetbrains_compose_ui_ui_util_desktop",
#        "@maven//:org_jetbrains_compose_runtime_runtime_saveable_desktop",
#        "@maven//:org_jetbrains_compose_runtime_runtime_desktop",
#        "@maven//:org_jetbrains_skiko_skiko_jvm",
    ],
    resources = [
        "//resources:logback-xml",
        "//resources/fonts/titillium_web:light",
        "//resources/fonts/titillium_web:regular",
        "//resources/fonts/titillium_web:semi-bold",
        "//resources/fonts/ubuntu_mono:bold",
        "//resources/fonts/ubuntu_mono:regular",
        "//resources/icons:blueprint-icons-16",
        "//resources/icons:blueprint-icons-20",
        "//resources/icons:database-png",
        "//resources/icons:database-svg",
    ],
    resource_strip_prefix = "resources",
    tags = ["maven_coordinates=com.vaticle.typedb:studio:{pom_version}"],
)

java_binary(
    name = "studio-bin-mac",
    main_class = "com.vaticle.typedb.studio.MainKt",
    runtime_deps = [
        ":studio",
        "@maven//:org_jetbrains_skiko_skiko_jvm_runtime_macos_x64",
    ],
)

java_binary(
    name = "studio-bin-windows",
    main_class = "com.vaticle.typedb.studio.MainKt",
    runtime_deps = [
        ":studio",
        "@maven//:org_jetbrains_skiko_skiko_jvm_runtime_windows_x64",
    ],
)

java_binary(
    name = "studio-bin-linux",
    main_class = "com.vaticle.typedb.studio.MainKt",
    runtime_deps = [
        ":studio",
        "@maven//:org_jetbrains_skiko_skiko_jvm_runtime_linux_x64",
    ],
)

#java_deps(
#    name = "main-deps-mac",
#    target = ":main",
#    java_deps_root = "lib/",
#    visibility = ["//:__pkg__"],
#    maven_name = False,
#)

assemble_files = {
    "//resources:logback-xml": "logback.xml",
    "//resources/fonts/titillium_web:light": "fonts/titillium_web/TitilliumWeb-Light.ttf",
    "//resources/fonts/titillium_web:regular": "fonts/titillium_web/TitilliumWeb-Regular.ttf",
    "//resources/fonts/titillium_web:semi-bold": "fonts/titillium_web/TitilliumWeb-SemiBold.ttf",
    "//resources/fonts/ubuntu_mono:bold": "fonts/ubuntu_mono/UbuntuMono-Bold.ttf",
    "//resources/fonts/ubuntu_mono:regular": "fonts/ubuntu_mono/UbuntuMono-Regular.ttf",
    "//:LICENSE": "LICENSE",
}

#assemble_zip(
#    name = "assemble-mac-zip",
#    targets = [":main-deps-mac"],
#    additional_files = assemble_files,
#    output_filename = "typedb-studio-mac",
#    visibility = ["//:__pkg__"]
#)

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
    main_class = "com.vaticle.typedb.studio.MainKt",
    deps_use_maven_name = False,
    additional_files = assemble_files,
    mac_entitlements = "//resources:entitlements-mac-plist",
    mac_code_signing_cert = "@vaticle_apple_developer_id_application_cert//file",
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

#deploy_github(
#    name = "deploy-github-windows",
#    organisation = deployment_github['github.organisation'],
#    repository = deployment_github['github.repository'],
#    title = "TypeDB Studio",
#    title_append_version = True,
#    release_description = "//:RELEASE_TEMPLATE.md",
#    archive = ":application-image",
##    archive = ":hello-bundle",
#    version_file = ":VERSION",
#    draft = True,
#    windows = True,
#)

# TODO: add release_validate_deps

#checksum(
#    name = "checksum",
#    archive = ":application-image",
#)

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
    exclude = glob([
        ".circleci/windows/dependencies.config",
    ]),
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
