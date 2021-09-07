#load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")
#
#def bazel_toolchain():
#    http_archive(
#      name = "bazel_toolchains",
#      sha256 = "239a1a673861eabf988e9804f45da3b94da28d1aff05c373b013193c315d9d9e",
#      strip_prefix = "bazel-toolchains-3.0.1",
#      urls = [
#        "https://github.com/bazelbuild/bazel-toolchains/releases/download/3.0.1/bazel-toolchains-3.0.1.tar.gz",
#      ],
#    )
#
#alias(
#    name = "remote_jdk16",
#    actual = select({
#        "@vaticle_dependencies//util/platform:is_mac": [name + "-mac"],
#        "@vaticle_dependencies//util/platform:is_linux": [name + "-linux"],
#        "@vaticle_dependencies//util/platform:is_windows": [name + "-windows"],
#        "//conditions:default": [name + "-mac"],
#    })
#)
