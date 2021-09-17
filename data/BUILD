load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

package(default_visibility = ["//visibility:public"])

kt_jvm_library(
    name = "data",
    srcs = glob(["*.kt"]),
    kotlin_compiler_plugin = "@org_jetbrains_compose_compiler//file",
    deps = [
        # External dependencies from @vaticle
        "@vaticle_typedb_client_java//:client-java",
        "@vaticle_typedb_client_java//api",
        "@vaticle_typedb_client_java//common",
        "@vaticle_typedb_common//:common",
        "@vaticle_typeql_lang_java//:typeql-lang",
        "@vaticle_typeql_lang_java//pattern",
        "@vaticle_typeql_lang_java//query",

        # Maven
        "@maven//:org_jetbrains_compose_desktop_desktop_jvm",
    ],
    tags = ["maven_coordinates=com.vaticle.typedb:studio-data:{pom_version}"],
)

#java_import(
#    name = "client-java",
#    jars = [
#        "@vaticle_typedb_client_java//common:libcommon.jar",
#        "@vaticle_typedb_client_java//api:libapi.jar",
#    ],
#    deps = [],
#    tags = ["maven_coordinates=com.vaticle.typedb:studio-typedb-client:{pom_version}"]
#)
