load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

package(default_visibility = ["//visibility:public"])

kt_jvm_library(
    name = "diagnostics",
    srcs = glob(["*.kt"]),
    kotlin_compiler_plugin = "@org_jetbrains_compose_compiler//file",
    deps = [
        # Maven
        "@maven//:org_jetbrains_compose_desktop_desktop_jvm",
    ],
    tags = ["maven_coordinates=com.vaticle.typedb:studio-diagnostics:{pom_version}"],
)
