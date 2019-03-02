load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")

git_repository(
    name = "graknlabs_grakn_core",
    remote = "https://github.com/graknlabs/grakn",
    commit = "74c16c99719813dc69de1fd42542244b6e92a85e" # grabl-marker: do not remove this comment, this is used for dependency-update by @graknlabs_grakn_core
)

git_repository(
    name = "graknlabs_client_java",
    remote = "https://github.com/graknlabs/client-java",
    commit = "79c1f242b91fe315a5a52a9c0a04dd4fe0dc04b5"
)

######################################
# Load Node.js dependencies from NPM #
######################################

# Load Node.js depdendencies for Bazel
git_repository(
    name = "build_bazel_rules_nodejs",
    remote = "https://github.com/graknlabs/rules_nodejs.git",
    commit = "ac3f6854365f119130186f971588514ccff503ab",
)

load("@build_bazel_rules_nodejs//:package.bzl", "rules_nodejs_dependencies")
rules_nodejs_dependencies()

# Load NPM dependencies for Node.js programs
load("@build_bazel_rules_nodejs//:defs.bzl", "node_repositories", "npm_install")
node_repositories(package_json = ["//:package.json"], node_version = "10.13.0")

load("@graknlabs_grakn_core//dependencies/compilers:dependencies.bzl", "grpc_dependencies")
grpc_dependencies()

load("@com_github_grpc_grpc//bazel:grpc_deps.bzl", com_github_grpc_grpc_bazel_grpc_deps = "grpc_deps")
com_github_grpc_grpc_bazel_grpc_deps()

load("@graknlabs_grakn_core//dependencies/tools:dependencies.bzl", "tools_dependencies")
tools_dependencies()

load("@graknlabs_grakn_core//dependencies/tools/checkstyle:checkstyle.bzl", "checkstyle_dependencies")
checkstyle_dependencies()

load("@graknlabs_grakn_core//dependencies/distribution:dependencies.bzl", "distribution_dependencies")
distribution_dependencies()

load("@graknlabs_bazel_distribution//github:dependencies.bzl", "github_dependencies_for_deployment")
github_dependencies_for_deployment()

load("@com_github_google_bazel_common//:workspace_defs.bzl", "google_common_workspace_rules")
google_common_workspace_rules()

load("@stackb_rules_proto//java:deps.bzl", "java_grpc_compile")
java_grpc_compile()

load("@graknlabs_grakn_core//dependencies/maven:dependencies.bzl", maven_dependencies_for_build = "maven_dependencies")
maven_dependencies_for_build()

load("@graknlabs_grakn_core//dependencies/git:dependencies.bzl", "graknlabs_graql")
graknlabs_graql()

load("@graknlabs_graql//dependencies/compilers:dependencies.bzl", "antlr_dependencies")
antlr_dependencies()

load("@rules_antlr//antlr:deps.bzl", "antlr_dependencies")
antlr_dependencies()

load("@graknlabs_graql//dependencies/maven:dependencies.bzl", graql_dependencies = "maven_dependencies")
graql_dependencies()

load("@stackb_rules_proto//java:deps.bzl", "java_grpc_compile")
java_grpc_compile()

load("@graknlabs_grakn_core//dependencies/docker:dependencies.bzl", "docker_dependencies")
docker_dependencies()