load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

package(default_visibility = ["//:__pkg__"])

kt_jvm_library(
    name = "workspace",
    srcs = glob(["*.kt"]),
    kotlin_compiler_plugin = "@org_jetbrains_compose_compiler//file",
    deps = [
        "//appearance",
        "//diagnostics",
        "//routing",
        "//ui/elements",
        "//visualiser",

        # External dependencies from @vaticle
        "@vaticle_typedb_client_java//api",

        # Maven
        "@maven//:com_fifesoft_rsyntaxtextarea",
        "@maven//:org_jetbrains_compose_desktop_desktop_jvm",
    ],
    tags = ["maven_coordinates=com.vaticle.typedb:studio-workspace:{pom_version}"],
)
