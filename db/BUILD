load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

package(default_visibility = ["//:__pkg__", "//visualiser:__subpackages__"])

kt_jvm_library(
    name = "db",
    srcs = glob(["*.kt"]),
    kotlin_compiler_plugin = "@org_jetbrains_compose_compiler//file",
    deps = [
        # External dependencies from @vaticle
        "@vaticle_typedb_client_java//:client-java",
        "@vaticle_typedb_client_java//api",
        "@vaticle_typedb_client_java//common",
        "@vaticle_typedb_common//:common",

        # Maven
        "@maven//:org_jetbrains_compose_desktop_desktop_jvm",
    ],
)
