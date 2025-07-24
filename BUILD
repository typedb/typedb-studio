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

genrule(
    name = "native-artifact-mac-x86_64-dmg",
    outs = ["typedb-studio_3.4.0-rc1_amd64.dmg"],
    cmd = """
        cp src-tauri/target/release/bundle/dmg/*.dmg "$(OUTS)"
    """,
    tags = ["local"],
    target_compatible_with = constraint_mac_x86_64,
)

genrule(
    name = "native-artifact-mac-arm64-dmg",
    outs = ["typedb-studio_3.4.0-rc1_aarch64.dmg"],
    cmd = """
        cp src-tauri/target/release/bundle/dmg/*.dmg "$(OUTS)"
    """,
    tags = ["local"],
    target_compatible_with = constraint_mac_arm64,
)

genrule(
    name = "native-artifact-linux-arm64-deb",
    outs = ["typedb-studio_3.4.0-rc1_arm64.deb"],
    cmd = """
        cp src-tauri/target/release/bundle/deb/*.deb "$(OUTS)"
    """,
    tags = ["local"],
    target_compatible_with = constraint_linux_arm64,
)

genrule(
    name = "native-artifact-linux-x86_64-deb",
    outs = ["typedb-studio_3.4.0-rc1_amd64.deb"],
    cmd = """
        cp src-tauri/target/release/bundle/deb/*.deb "$(OUTS)"
    """,
    tags = ["local"],
    target_compatible_with = constraint_linux_x86_64,
)

genrule(
    name = "native-artifact-windows-x86_64-msi",
    outs = ["typedb-studio_3.4.0-rc1_amd64.msi"],
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

assemble_files = {
    "//:LICENSE": "LICENSE",
}

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
    tags = ["manual", "no-ide"],
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
    tags = ["manual", "no-ide"],
)

deploy_artifact(
    name = "deploy-linux-x86_64-deb",
    target = ":native-artifact-linux-x86_64-deb",
    artifact_group = "typedb-studio-linux-x86_64",
    artifact_name = "typedb-studio-linux-x86_64-{version}.deb",
    snapshot = deployment['artifact']['snapshot']['upload'],
    release = deployment['artifact']['release']['upload'],
    version_file = ":VERSION",
    visibility = ["//visibility:public"],
    target_compatible_with = constraint_linux_x86_64,
    tags = ["manual", "no-ide"],
)

deploy_artifact(
    name = "deploy-linux-arm64-deb",
    target = ":native-artifact-linux-arm64-deb",
    artifact_group = "typedb-studio-linux-arm64",
    artifact_name = "typedb-studio-linux-arm64-{version}.deb",
    snapshot = deployment['artifact']['snapshot']['upload'],
    release = deployment['artifact']['release']['upload'],
    version_file = ":VERSION",
    visibility = ["//visibility:public"],
    target_compatible_with = constraint_linux_arm64,
    tags = ["manual", "no-ide"],
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
    tags = ["manual", "no-ide"],
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
    target = ":native-artifact-linux-x86_64-deb",
    snapshot = deployment['apt']['snapshot']['upload'],
    release = deployment['apt']['release']['upload'],
)

deploy_apt(
    name = "deploy-apt-arm64",
    target = ":native-artifact-linux-arm64-deb",
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
