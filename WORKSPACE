# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

workspace(name = "typedb_studio")

################################
# Load @typedb_dependencies #
################################

load("//dependencies/typedb:repositories.bzl", "typedb_dependencies")
typedb_dependencies()

# Load //builder/bazel for RBE
load("@typedb_dependencies//builder/bazel:deps.bzl", "bazel_toolchain")
bazel_toolchain()

# Load //builder/java
load("@typedb_dependencies//builder/java:deps.bzl", "rules_jvm_external")
rules_jvm_external()
load("@typedb_dependencies//library/maven:rules.bzl", "maven")

load("@rules_jvm_external//:repositories.bzl", "rules_jvm_external_deps")
rules_jvm_external_deps()

# Load //builder/kotlin
load("@typedb_dependencies//builder/kotlin:deps.bzl", "io_bazel_rules_kotlin")
io_bazel_rules_kotlin()
load("@io_bazel_rules_kotlin//kotlin:repositories.bzl", "kotlin_repositories")
kotlin_repositories()
load("@io_bazel_rules_kotlin//kotlin:core.bzl", "kt_register_toolchains")
kt_register_toolchains()

# Load //tool/common
load("@typedb_dependencies//tool/common:deps.bzl", "typedb_dependencies_ci_pip",
    typedb_dependencies_tool_maven_artifacts = "maven_artifacts")
typedb_dependencies_ci_pip()

# Load //tool/checkstyle
load("@typedb_dependencies//tool/checkstyle:deps.bzl", checkstyle_deps = "deps")
checkstyle_deps()

######################################
# Load @typedb_bazel_distribution #
######################################

load("//dependencies/typedb:repositories.bzl", "typedb_bazel_distribution")
typedb_bazel_distribution()

# Load @typedb_bazel_distribution_uploader
load("@typedb_bazel_distribution//common/uploader:deps.bzl", "typedb_bazel_distribution_uploader")
typedb_bazel_distribution_uploader()
load("@typedb_bazel_distribution_uploader//:requirements.bzl", install_uploader_deps = "install_deps")
install_uploader_deps()

# Load //common
load("@typedb_bazel_distribution//common:deps.bzl", "rules_pkg")
rules_pkg()
load("@rules_pkg//:deps.bzl", "rules_pkg_dependencies")
rules_pkg_dependencies()

# Load //github
load("@typedb_bazel_distribution//github:deps.bzl", "ghr_linux_tar", "ghr_osx_zip")
ghr_linux_tar()
ghr_osx_zip()

# Load //pip
load("@typedb_bazel_distribution//pip:deps.bzl", "typedb_bazel_distribution_pip")
typedb_bazel_distribution_pip()

############################
# Load @maven dependencies #
############################
load("@typedb_dependencies//library/maven:rules.bzl", "maven")
maven(
    typedb_dependencies_tool_maven_artifacts
)

###############################################
# Create @typedb_studio_workspace_refs #
###############################################

load("@typedb_bazel_distribution//common:rules.bzl", "workspace_refs")
workspace_refs(name = "typedb_studio_workspace_refs")
