load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

package(default_visibility = ["//:__pkg__"])

kt_jvm_library(
    name = "ui",
    srcs = glob(["*.kt"]),
    kotlin_compiler_plugin = "@org_jetbrains_compose_compiler//file",
    deps = [
        "//db",
        "//visualiser",

        # External dependencies from @vaticle
        "@vaticle_force_graph//:force-graph",

        # Maven
        "@maven//:org_jetbrains_compose_desktop_desktop_jvm",
    ],
)
