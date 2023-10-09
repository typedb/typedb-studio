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

workspace(name = "vaticle_typedb_studio")

################################
# Load @vaticle_dependencies #
################################
load("//dependencies/vaticle:repositories.bzl", "vaticle_dependencies")
vaticle_dependencies()

# Load Bazel
load("@vaticle_dependencies//builder/bazel:deps.bzl", "bazel_toolchain")
bazel_toolchain()

# Load //builder/java
load("@vaticle_dependencies//builder/java:deps.bzl", java_deps = "deps")
java_deps()

# Load //builder/kotlin
load("@vaticle_dependencies//builder/kotlin:deps.bzl", kotlin_deps = "deps")
kotlin_deps()
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
load("@vaticle_dependencies//builder/antlr:deps.bzl", antlr_deps = "deps", "antlr_version")
antlr_deps()

load("@rules_antlr//antlr:lang.bzl", "JAVA")
load("@rules_antlr//antlr:repositories.bzl", "rules_antlr_dependencies")
rules_antlr_dependencies(antlr_version, JAVA)

# Load //builder/grpc (required by typedb_driver_java)
load("@vaticle_dependencies//builder/grpc:deps.bzl", grpc_deps = "deps")
grpc_deps()

load("@com_github_grpc_grpc//bazel:grpc_deps.bzl", com_github_grpc_grpc_deps = "grpc_deps")
com_github_grpc_grpc_deps()

# Load //builder/rust (required by typedb_driver_java)
load("@vaticle_dependencies//builder/rust:deps.bzl", rust_deps = "deps")
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

load("@vaticle_dependencies//library/crates:crates.bzl", "fetch_crates")
fetch_crates()
load("@crates//:defs.bzl", "crate_repositories")
crate_repositories()

load("@vaticle_dependencies//tool/swig:deps.bzl", swig_deps = "deps")
swig_deps()

# Load Compose
load("@vaticle_dependencies//builder/compose:deps.bzl", compose_deps = "deps")
compose_deps()

# Load Checkstyle
load("@vaticle_dependencies//tool/checkstyle:deps.bzl", checkstyle_deps = "deps")
checkstyle_deps()

# Load Unused Deps
load("@vaticle_dependencies//tool/unuseddeps:deps.bzl", unuseddeps_deps = "deps")
unuseddeps_deps()

# Load //tool/common
load("@vaticle_dependencies//tool/common:deps.bzl", "vaticle_dependencies_ci_pip",
    vaticle_dependencies_tool_maven_artifacts = "maven_artifacts")

#####################################################################
# Load @vaticle_bazel_distribution from (@vaticle_dependencies) #
#####################################################################
load("//dependencies/vaticle:repositories.bzl", "vaticle_bazel_distribution")
vaticle_bazel_distribution()

# Load //pip
load("@vaticle_bazel_distribution//pip:deps.bzl", pip_deps = "deps")
pip_deps()

# Load //github
load("@vaticle_bazel_distribution//github:deps.bzl", github_deps = "deps")
github_deps()

load("@vaticle_bazel_distribution//common:deps.bzl", "rules_pkg")
rules_pkg()

load("@rules_pkg//:deps.bzl", "rules_pkg_dependencies")
rules_pkg_dependencies()

# Load maven artifacts
load("//dependencies/maven:artifacts.bzl", vaticle_typedb_studio_artifacts = "artifacts")

# Load artifacts
load("//dependencies/vaticle:artifacts.bzl", "vaticle_typedb_artifact")
vaticle_typedb_artifact()

################################
# Load @vaticle dependencies #
################################

# Load repositories
load(
    "//dependencies/vaticle:repositories.bzl",
    "vaticle_force_graph", "vaticle_typedb_common", "vaticle_typedb_driver"
)
vaticle_force_graph()
vaticle_typedb_common()
vaticle_typedb_driver()

load(
    "@vaticle_typedb_driver//dependencies/vaticle:repositories.bzl",
    "vaticle_typedb_protocol", "vaticle_typeql"
)
vaticle_typeql()
vaticle_typedb_protocol()

# Load Maven
load("@vaticle_typeql//dependencies/maven:artifacts.bzl", vaticle_typeql_artifacts = "artifacts")
load("@vaticle_typedb_driver//dependencies/maven:artifacts.bzl", vaticle_typedb_driver_artifacts = "artifacts")
load("@vaticle_typedb_common//dependencies/maven:artifacts.bzl", vaticle_typedb_common_artifacts = "artifacts")
load("@vaticle_force_graph//dependencies/maven:artifacts.bzl", vaticle_force_graph_artifacts = "artifacts")


############################
# Load @maven dependencies #
############################
load("@vaticle_dependencies//library/maven:rules.bzl", "maven")
maven(
    vaticle_dependencies_tool_maven_artifacts +
    vaticle_typeql_artifacts +
    vaticle_typedb_driver_artifacts +
    vaticle_typedb_common_artifacts +
    vaticle_force_graph_artifacts +
    vaticle_typedb_studio_artifacts,
    fail_on_missing_checksum = False,
)


###############################################
# Create @vaticle_typedb_studio_workspace_refs #
###############################################
load("@vaticle_bazel_distribution//common:rules.bzl", "workspace_refs")
workspace_refs(name = "vaticle_typedb_studio_workspace_refs")
