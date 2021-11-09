load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library", "kt_jvm_test")

package(default_visibility = ["//workspace:__pkg__"])

kt_jvm_library(
    name = "visualiser",
    srcs = glob(["*.kt"], exclude = ["*Test.kt"]),
    kotlin_compiler_plugin = "@org_jetbrains_compose_compiler//file",
    deps = [
        "//appearance",
        "//ui/elements",
        "//data",
        "//diagnostics",
        "//routing",

        # External dependencies from @vaticle
        "@vaticle_force_graph//:force-graph",

        # Maven
        "@maven//:org_jetbrains_compose_desktop_desktop_jvm",
        "@maven//:org_slf4j_slf4j_api",
    ],
    tags = ["maven_coordinates=com.vaticle.typedb:studio-visualiser:{pom_version}"],
)

kt_jvm_test(
    name = "test-geometry",
    srcs = [
        "GeometryTest.kt",
    ],
    deps = [
        "visualiser",

        "@maven//:junit_junit",
        "@maven//:org_jetbrains_kotlin_kotlin_test",
    ],
    test_class = "com.vaticle.typedb.studio.visualiser.GeometryTest",
)
