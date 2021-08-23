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
load("@vaticle_dependencies//library/maven:rules.bzl", "maven")

# Load Python
load("@vaticle_dependencies//builder/python:deps.bzl", python_deps = "deps")
python_deps()
load("@rules_python//python:pip.bzl", "pip_install")
pip_install(
    name = "vaticle_dependencies_ci_pip",
    requirements = "@vaticle_dependencies//tool:requirements.txt",
)

# Load Kotlin
load("@vaticle_dependencies//builder/kotlin:deps.bzl", kotlin_deps = "deps")
kotlin_deps()
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kotlin_repositories", "kt_register_toolchains")
kotlin_repositories()
kt_register_toolchains()

# Load //builder/antlr (required by typedb_client_java > typeql_lang_java)
load("@vaticle_dependencies//builder/antlr:deps.bzl", antlr_deps = "deps", "antlr_version")
antlr_deps()

load("@rules_antlr//antlr:lang.bzl", "JAVA")
load("@rules_antlr//antlr:repositories.bzl", "rules_antlr_dependencies")
rules_antlr_dependencies(antlr_version, JAVA)

# Load //builder/grpc (required by typedb_client_java)
load("@vaticle_dependencies//builder/grpc:deps.bzl", grpc_deps = "deps")
grpc_deps()
load("@com_github_grpc_grpc//bazel:grpc_deps.bzl",
com_github_grpc_grpc_deps = "grpc_deps")
com_github_grpc_grpc_deps()

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
load("@vaticle_dependencies//distribution:deps.bzl", "vaticle_bazel_distribution")
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


################################
# Load @vaticle dependencies #
################################

# Load repositories
load("//dependencies/vaticle:repositories.bzl", "vaticle_force_graph", "vaticle_typedb_client_java")
vaticle_force_graph()
vaticle_typedb_client_java()

load("@vaticle_typedb_client_java//dependencies/vaticle:repositories.bzl", "vaticle_typedb_common", "vaticle_factory_tracing", "vaticle_typedb_protocol", "vaticle_typeql_lang_java")
vaticle_typedb_common()
vaticle_typeql_lang_java()
vaticle_factory_tracing()
vaticle_typedb_protocol()

load("@vaticle_typeql_lang_java//dependencies/vaticle:repositories.bzl", "vaticle_typeql")
vaticle_typeql()

# Load Maven
load("@vaticle_typeql_lang_java//dependencies/maven:artifacts.bzl", vaticle_typeql_lang_java_artifacts = "artifacts")
load("@vaticle_typedb_client_java//dependencies/maven:artifacts.bzl", vaticle_typedb_client_java_artifacts = "artifacts")
load("@vaticle_typedb_common//dependencies/maven:artifacts.bzl", vaticle_typedb_common_artifacts = "artifacts")
load("@vaticle_factory_tracing//dependencies/maven:artifacts.bzl", vaticle_factory_tracing_artifacts = "artifacts")
load("@vaticle_force_graph//dependencies/maven:artifacts.bzl", vaticle_force_graph_artifacts = "artifacts")


############################
# Load @maven dependencies #
############################
maven(
    vaticle_dependencies_tool_maven_artifacts +
    vaticle_typeql_lang_java_artifacts +
    vaticle_typedb_client_java_artifacts +
    vaticle_typedb_common_artifacts +
    vaticle_factory_tracing_artifacts +
    vaticle_force_graph_artifacts +
    vaticle_typedb_studio_artifacts,

    fail_on_missing_checksum = False,
)


###############################################
# Create @vaticle_typedb_studio_workspace_refs #
###############################################
load("@vaticle_bazel_distribution//common:rules.bzl", "workspace_refs")
workspace_refs(name = "vaticle_typedb_studio_workspace_refs")
