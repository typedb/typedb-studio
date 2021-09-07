load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

package(default_visibility = ["//:__pkg__"])

kt_jvm_library(
    name = "login",
    srcs = glob(["*.kt"]),
    kotlin_compiler_plugin = "@org_jetbrains_compose_compiler//file",
    deps = [
        "//appearance",
        "//ui/elements",
        "//data",
        "//navigation",

        # Maven
        "@maven//:org_jetbrains_compose_desktop_desktop_jvm",
    ],
    tags = ["maven_coordinates=com.vaticle.typedb:studio-login:{pom_version}"],
)
