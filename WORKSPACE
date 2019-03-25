workspace(name = "graknlabs_workbase")


###########################
# Grakn Labs Dependencies #
###########################

load("//dependencies/graknlabs:dependencies.bzl", "graknlabs_grakn_core", "graknlabs_build_tools")
graknlabs_grakn_core()
graknlabs_build_tools()

load("@graknlabs_build_tools//distribution:dependencies.bzl", "graknlabs_bazel_distribution")
graknlabs_bazel_distribution()


###########################
# Load Bazel Dependencies #
###########################

load("@graknlabs_build_tools//bazel:dependencies.bzl", "bazel_common", "bazel_toolchain")
bazel_common()
bazel_toolchain()


#################################
# Load Build Tools dependencies #
#################################

load("@graknlabs_build_tools//bazel:dependencies.bzl", "bazel_rules_python")
bazel_rules_python()

load("@io_bazel_rules_python//python:pip.bzl", "pip_repositories", "pip_import")
pip_repositories()

pip_import(
    name = "graknlabs_build_tools_ci_pip",
    requirements = "@graknlabs_build_tools//ci:requirements.txt",
)
load("@graknlabs_build_tools_ci_pip//:requirements.bzl",
graknlabs_build_tools_ci_pip_install = "pip_install")
graknlabs_build_tools_ci_pip_install()

#############################
# Load Node.js Dependencies #
#############################

load("@graknlabs_build_tools//bazel:dependencies.bzl", "bazel_rules_nodejs")
bazel_rules_nodejs()

load("@build_bazel_rules_nodejs//:package.bzl", "rules_nodejs_dependencies")
rules_nodejs_dependencies()

load("@build_bazel_rules_nodejs//:defs.bzl", "node_repositories", "npm_install")
node_repositories(package_json = ["//:package.json"])
npm_install(
    name = "nodejs_dependencies",
    package_json = "//:package.json",
    data = [
      "@build_bazel_rules_nodejs//internal/babel_library:package.json",
      "@build_bazel_rules_nodejs//internal/babel_library:babel.js",
      "@build_bazel_rules_nodejs//internal/babel_library:yarn.lock",
    ],
)


##########################
# Load GRPC Dependencies #
##########################

load("@graknlabs_build_tools//grpc:dependencies.bzl", "grpc_dependencies")
grpc_dependencies()

load("@com_github_grpc_grpc//bazel:grpc_deps.bzl", com_github_grpc_grpc_bazel_grpc_deps = "grpc_deps")
com_github_grpc_grpc_bazel_grpc_deps()

# Load GRPC Node.js dependencies
load("@stackb_rules_proto//node:deps.bzl", "node_grpc_compile")
node_grpc_compile()


################################
# Load Grakn Core Dependencies #
################################

load("@graknlabs_grakn_core//dependencies/graknlabs:dependencies.bzl",
     "graknlabs_graql", "graknlabs_client_java", "graknlabs_benchmark")
graknlabs_graql()
graknlabs_client_java()
graknlabs_benchmark()

load("@graknlabs_grakn_core//dependencies/maven:dependencies.bzl", maven_dependencies_for_build = "maven_dependencies")
maven_dependencies_for_build()

# Load Graql dependencies for Grakn Core

load("@graknlabs_graql//dependencies/compilers:dependencies.bzl", "antlr_dependencies")
antlr_dependencies()

load("@rules_antlr//antlr:deps.bzl", "antlr_dependencies")
antlr_dependencies()

load("@graknlabs_graql//dependencies/maven:dependencies.bzl", graql_dependencies = "maven_dependencies")
graql_dependencies()

# Load Client Java dependencies for Grakn Core

load("@stackb_rules_proto//java:deps.bzl", "java_grpc_compile")
java_grpc_compile()

# Load Docker dependencies for Grakn Core

load("@graknlabs_build_tools//bazel:dependencies.bzl", "bazel_rules_docker")
bazel_rules_docker()


#####################################
# Load Bazel Common Workspace Rules #
#####################################

# TODO: Figure out why this cannot be loaded at earlier at the top of the file
load("@com_github_google_bazel_common//:workspace_defs.bzl", "google_common_workspace_rules")
google_common_workspace_rules()