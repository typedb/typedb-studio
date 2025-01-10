# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

workspace(name = "typedb_studio")

################################
# Load @typedb_dependencies #
################################
load("//dependencies/typedb:repositories.bzl", "typedb_dependencies")
typedb_dependencies()

# Load Bazel
load("@typedb_dependencies//builder/bazel:deps.bzl", "bazel_toolchain")
bazel_toolchain()

# Load //builder/python
load("@typedb_dependencies//builder/python:deps.bzl", "rules_python")
rules_python()
load("@rules_python//python:repositories.bzl", "py_repositories", "python_register_toolchains")
py_repositories()

# Load //builder/java
load("@typedb_dependencies//builder/java:deps.bzl", "rules_jvm_external")
rules_jvm_external()

# Load //builder/kotlin
# FIXME studio kotlin dependency is held back, out of sync with dependencies
#load("@typedb_dependencies//builder/kotlin:deps.bzl", "io_bazel_rules_kotlin")
#io_bazel_rules_kotlin()
load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
http_archive(
    name = "io_bazel_rules_kotlin",
    urls = ["https://github.com/bazelbuild/rules_kotlin/releases/download/v1.7.0-RC-3/rules_kotlin_release.tgz"],
    sha256 = "f033fa36f51073eae224f18428d9493966e67c27387728b6be2ebbdae43f140e"
)

load("@io_bazel_rules_kotlin//kotlin:repositories.bzl", "kotlin_repositories", "kotlinc_version")
kotlin_repositories(
    compiler_release = kotlinc_version(
        release = "1.7.20",
        sha256 = "5e3c8d0f965410ff12e90d6f8dc5df2fc09fd595a684d514616851ce7e94ae7d"
    )
)
load("@io_bazel_rules_kotlin//kotlin:core.bzl", "kt_register_toolchains")
kt_register_toolchains()

# Load //builder/antlr (required by typedb_driver_java > typeql)
load("@typedb_dependencies//builder/antlr:deps.bzl", "rules_antlr", "antlr_version")
rules_antlr()

load("@rules_antlr//antlr:lang.bzl", "JAVA")
load("@rules_antlr//antlr:repositories.bzl", "rules_antlr_dependencies")
rules_antlr_dependencies(antlr_version, JAVA)

# Load //builder/proto_grpc (required by typedb_driver_java)
load("@typedb_dependencies//builder/proto_grpc:deps.bzl", proto_grpc_deps = "deps")
proto_grpc_deps()

load("@com_github_grpc_grpc//bazel:grpc_deps.bzl", com_github_grpc_grpc_deps = "grpc_deps")
com_github_grpc_grpc_deps()

# Load //builder/rust (required by typedb_driver_java)
load("@typedb_dependencies//builder/rust:deps.bzl", rust_deps = "deps")
rust_deps()

load("@rules_rust//rust:repositories.bzl", "rules_rust_dependencies", "rust_register_toolchains", "rust_analyzer_toolchain_repository")
load("@rules_rust//tools/rust_analyzer:deps.bzl", "rust_analyzer_dependencies")
rules_rust_dependencies()
load("@rules_rust//rust:defs.bzl", "rust_common")
rust_register_toolchains(
    edition = "2021",
    extra_target_triples = [
        "aarch64-apple-darwin",
        "aarch64-unknown-linux-gnu",
        "x86_64-apple-darwin",
        "x86_64-pc-windows-msvc",
        "x86_64-unknown-linux-gnu",
    ],
    rust_analyzer_version = rust_common.default_version,
)

load("@typedb_dependencies//library/crates:crates.bzl", "fetch_crates")
fetch_crates()
load("@crates//:defs.bzl", "crate_repositories")
crate_repositories()

load("@typedb_dependencies//tool/swig:deps.bzl", "swig")
swig()

# Load Compose
load("@typedb_dependencies//builder/compose:deps.bzl", compose_deps = "deps")
compose_deps()

# Load Checkstyle
load("@typedb_dependencies//tool/checkstyle:deps.bzl", checkstyle_deps = "deps")
checkstyle_deps()

# Load Unused Deps
load("@typedb_dependencies//tool/unuseddeps:deps.bzl", unuseddeps_deps = "deps")
unuseddeps_deps()

# Load //tool/common
load("@typedb_dependencies//tool/common:deps.bzl", "typedb_dependencies_ci_pip",
    typedb_dependencies_tool_maven_artifacts = "maven_artifacts")

###############################################################
# Load @typedb_bazel_distribution from (@typedb_dependencies) #
###############################################################
load("@typedb_dependencies//distribution:deps.bzl", "typedb_bazel_distribution")
typedb_bazel_distribution()

# Load //pip
load("@typedb_bazel_distribution//pip:deps.bzl", "typedb_bazel_distribution_pip")
typedb_bazel_distribution_pip()

# Load @typedb_bazel_distribution_uploader
load("@typedb_bazel_distribution//common/uploader:deps.bzl", "typedb_bazel_distribution_uploader")
typedb_bazel_distribution_uploader()
load("@typedb_bazel_distribution_uploader//:requirements.bzl", uploader_install_deps = "install_deps")
uploader_install_deps()

# Load //github
load("@typedb_bazel_distribution//github:deps.bzl", "ghr_osx_zip", "ghr_linux_tar")
ghr_osx_zip()
ghr_linux_tar()

load("@typedb_bazel_distribution//common:deps.bzl", "rules_pkg")
rules_pkg()

load("@rules_pkg//:deps.bzl", "rules_pkg_dependencies")
rules_pkg_dependencies()

# Load maven artifacts
load("//dependencies/maven:artifacts.bzl", typedb_studio_artifacts = "artifacts")

# Load artifacts
load("//dependencies/typedb:artifacts.bzl", "typedb_artifact")
typedb_artifact()

# Load //docs
load("@typedb_bazel_distribution//docs:python/deps.bzl", "typedb_bazel_distribution_docs_py")
typedb_bazel_distribution_docs_py()
load("@typedb_bazel_distribution_docs_py//:requirements.bzl", docs_py_install_deps = "install_deps")
docs_py_install_deps()

load("@typedb_bazel_distribution//docs:java/deps.bzl", "google_bazel_common")
google_bazel_common()
load("@google_bazel_common//:workspace_defs.bzl", "google_common_workspace_rules")
google_common_workspace_rules()

################################
# Load @typedb dependencies #
################################

# Load repositories
load("//dependencies/typedb:repositories.bzl", "typedb_force_graph", "typedb_driver", "typeql")
typedb_force_graph()
typedb_driver()
typeql()

load("@typedb_driver//dependencies/typedb:repositories.bzl", "typedb_protocol")
typedb_protocol()

# Load Maven
#load("//dependencies/typedb:artifacts.bzl", typedb_studio_maven_artifacts = "maven_artifacts")
load("@typeql//dependencies/maven:artifacts.bzl", typeql_artifacts = "artifacts")
load("@typedb_driver//dependencies/maven:artifacts.bzl", typedb_driver_artifacts = "artifacts")
load("@typedb_force_graph//dependencies/maven:artifacts.bzl", typedb_force_graph_artifacts = "artifacts")

############################
# Load @maven dependencies #
############################
http_archive(
    name = "io_bazel_stardoc",
    sha256 = "3fd8fec4ddec3c670bd810904e2e33170bedfe12f90adf943508184be458c8bb",
    urls = [
        "https://mirror.bazel.build/github.com/bazelbuild/stardoc/releases/download/0.5.3/stardoc-0.5.3.tar.gz",
        "https://github.com/bazelbuild/stardoc/releases/download/0.5.3/stardoc-0.5.3.tar.gz",
    ],
)
load("@io_bazel_stardoc//:setup.bzl", "stardoc_repositories")
stardoc_repositories()

# FIXME studio compose dependencies are held back, out of sync with dependencies
load("//:maven.bzl", "maven")
maven(
    typedb_dependencies_tool_maven_artifacts +
    typeql_artifacts +
    typedb_driver_artifacts +
    typedb_force_graph_artifacts +
    typedb_studio_artifacts,
#    internal_artifacts = typedb_studio_maven_artifacts,
    fail_on_missing_checksum = False,
)

###############################################
# Create @typedb_studio_workspace_refs #
###############################################
load("@typedb_bazel_distribution//common:rules.bzl", "workspace_refs")
workspace_refs(name = "typedb_studio_workspace_refs")
