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
load("//:rules.bzl", "jvm_application_image", "print_rootpath")
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_binary", "kt_jvm_library")
load("@rules_pkg//:pkg.bzl", "pkg_zip")
load("@vaticle_dependencies//distribution:deployment.bzl", "deployment")
load("@vaticle_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")
load("@vaticle_bazel_distribution//common:rules.bzl", "checksum", "assemble_targz", "assemble_zip", "java_deps")
load("@vaticle_bazel_distribution//github:rules.bzl", "deploy_github")
load("@vaticle_bazel_distribution//brew:rules.bzl", "deploy_brew")
load("@io_bazel_rules_kotlin//kotlin/internal:toolchains.bzl", "define_kt_toolchain")

# TODO: If we remove some of these deps, IntelliJ starts to complain - we should investigate
kt_jvm_binary(
    name = "main",
    srcs = ["main.kt"],
    main_class = "com.vaticle.typedb.studio.MainKt",
    kotlin_compiler_plugin = "@org_jetbrains_compose_compiler//file",
    deps = [
        "//appearance",
        "//ui/elements",
        "//data",
        "//login",
        "//navigation",
        "//workspace",

        # Maven
        "@maven//:org_jetbrains_skiko_skiko_jvm_0_3_9",

        "@maven//:org_jetbrains_compose_ui_ui_util_desktop_1_0_0_alpha3",
        "@maven//:org_jetbrains_compose_ui_ui_unit_desktop_1_0_0_alpha3",
        "@maven//:org_jetbrains_compose_ui_ui_tooling_preview_desktop_1_0_0_alpha3",
        "@maven//:org_jetbrains_compose_ui_ui_text_desktop_1_0_0_alpha3",
        "@maven//:org_jetbrains_compose_ui_ui_graphics_desktop_1_0_0_alpha3",
        "@maven//:org_jetbrains_compose_ui_ui_geometry_desktop_1_0_0_alpha3",
        "@maven//:org_jetbrains_compose_ui_ui_desktop_1_0_0_alpha3",
        "@maven//:org_jetbrains_compose_runtime_runtime_saveable_desktop_1_0_0_alpha3",
        "@maven//:org_jetbrains_compose_runtime_runtime_desktop_1_0_0_alpha3",
        "@maven//:org_jetbrains_compose_material_material_ripple_desktop_1_0_0_alpha3",
        "@maven//:org_jetbrains_compose_material_material_icons_core_desktop_1_0_0_alpha3",
        "@maven//:org_jetbrains_compose_material_material_desktop_1_0_0_alpha3",
        "@maven//:org_jetbrains_compose_foundation_foundation_layout_desktop_1_0_0_alpha3",
        "@maven//:org_jetbrains_compose_foundation_foundation_desktop_1_0_0_alpha3",
        "@maven//:org_jetbrains_compose_desktop_desktop_jvm",
        "@maven//:org_jetbrains_compose_ui_ui_tooling_preview_desktop",
        "@maven//:org_jetbrains_compose_material_material_desktop",
        "@maven//:org_jetbrains_compose_material_material_ripple_desktop",
        "@maven//:org_jetbrains_compose_material_material_icons_core_desktop",
        "@maven//:org_jetbrains_compose_foundation_foundation_desktop",
        "@maven//:org_jetbrains_compose_animation_animation_desktop_1_0_0_alpha3",
        "@maven//:org_jetbrains_compose_animation_animation_desktop",
        "@maven//:org_jetbrains_compose_foundation_foundation_layout_desktop",
        "@maven//:org_jetbrains_compose_animation_animation_core_desktop_1_0_0_alpha3",
        "@maven//:org_jetbrains_compose_animation_animation_core_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_desktop",

        "@maven//:org_jetbrains_compose_ui_ui_text_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_graphics_desktop",
        "@maven//:org_jetbrains_skiko_skiko_jvm",
        "@maven//:org_jetbrains_compose_ui_ui_unit_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_geometry_desktop",
        "@maven//:org_jetbrains_compose_ui_ui_util_desktop",
        "@maven//:org_jetbrains_compose_runtime_runtime_saveable_desktop",
        "@maven//:org_jetbrains_compose_runtime_runtime_desktop",

        "@maven//:org_jetbrains_annotations_13_0",
        "@maven//:org_jetbrains_annotations",
        "@maven//:androidx_annotation_annotation_1_2_0",
        "@maven//:androidx_annotation_annotation",
    ],
    runtime_deps = [
        "@maven//:org_jetbrains_skiko_skiko_jvm_runtime_macos_x64",
    ],
    resources = [
        "//resources:logback-xml",
        "//resources/fonts/titillium_web:light",
        "//resources/fonts/titillium_web:regular",
        "//resources/fonts/titillium_web:semi-bold",
        "//resources/fonts/ubuntu_mono:bold",
        "//resources/fonts/ubuntu_mono:regular",
    ],
    resource_strip_prefix = "resources",
    tags = ["maven_coordinates=com.vaticle.typedb:studio:{pom_version}"],
)

# TODO: need native libraries deps
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
    name = "jpackage-runner-lib",
    srcs = ["JPackageRunner.kt"],
    deps = [
        "@maven//:org_apache_commons_commons_compress",
        "@maven//:org_zeroturnaround_zt_exec"
    ],
)

java_binary(
    name = "jpackage-runner",
    runtime_deps = [":jpackage-runner-lib"],
    main_class = "com.vaticle.typedb.studio.JPackageRunnerKt",
)

# TODO: Create a MANIFEST file in the jvm_binary and read it to determine the main jar and main class
jvm_application_image(
    name = "application-image",
    application_name = "TypeDB Studio",
    jvm_binary = ":main",
    main_jar = "com-vaticle-typedb-studio-0.0.0.jar",
    main_class = "com.vaticle.typedb.studio.MainKt",
    deps_use_maven_name = False,
    additional_files = assemble_files,
)

pkg_zip(
    name = "mac-zip",
    srcs = glob(["release/*.dmg"]),
)

deploy_github(
    name = "deploy-github-mac",
    organisation = deployment_github['github.organisation'],
    repository = deployment_github['github.repository'],
    title = "TypeDB Workbase",
    title_append_version = True,
    release_description = "//:RELEASE_TEMPLATE.md",
    archive = ":mac-zip",
    version_file = ":VERSION",
    draft = False
)

pkg_zip(
    name = "windows-zip",
    srcs = glob(["release/*.exe"]),
)

deploy_github(
    name = "deploy-github-windows",
    organisation = deployment_github['github.organisation'],
    repository = deployment_github['github.repository'],
    title = "TypeDB Workbase",
    title_append_version = True,
    release_description = "//:RELEASE_TEMPLATE.md",
    archive = ":windows-zip",
    version_file = ":VERSION",
    draft = False
)

pkg_zip(
    name = "linux-zip",
    srcs = glob(["release/*.AppImage"]),
)

deploy_github(
    name = "deploy-github-linux",
    organisation = deployment_github['github.organisation'],
    repository = deployment_github['github.repository'],
    title = "TypeDB Workbase",
    title_append_version = True,
    release_description = "//:RELEASE_TEMPLATE.md",
    archive = ":linux-zip",
    version_file = ":VERSION",
    draft = False
)

py_binary(
    name = "wait-for-release",
    srcs = [".grabl/wait-for-release.py"],
)

checksum(
    name = "checksum-mac",
    archive = ":mac-zip",
)

deploy_brew(
    name = "deploy-brew",
    snapshot = deployment['brew.snapshot'],
    release = deployment['brew.release'],
    formula = "//config/brew:typedb-workbase.rb",
    checksum = "//:checksum-mac",
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

# TODO: this is just a test genrule
genrule(
    name = "compose-compiler-path-genrule",
    outs = [
        "compose-compiler-path-genrule.txt",
    ],
    srcs = ["VERSION"],
    cmd = "read -a outs <<< '$(OUTS)' && echo $(rootpath @org_jetbrains_compose_compiler//file) > $${outs[0]}",
    tools = ["@org_jetbrains_compose_compiler//file"],
)

# TODO: this is just a test rule
print_rootpath(
    name = "compose-compiler-path",
    out = "compose-compiler-path.txt",
    target = "@org_jetbrains_compose_compiler//file",
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
