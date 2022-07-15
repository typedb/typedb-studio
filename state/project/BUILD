#
# Copyright (C) 2022 Vaticle
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.
#

load("@vaticle_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")
load("@io_bazel_rules_kotlin//kotlin:kotlin.bzl", "kt_jvm_library")

package(default_visibility = [
    "//:__pkg__",
    "//framework:__subpackages__",
    "//module:__subpackages__",
    "//state:__subpackages__",
    "//test:__pkg__",
])

kt_jvm_library(
    name = "project",
    srcs = glob(["*.kt"]),
    tags = ["maven_coordinates=com.vaticle.typedb:typedb-studio-state-project:{pom_version}"],
    deps = [
        "//state/app",
        "//state/common",
        "//state/connection",
        "//state/page",

        # External Vaticle Dependencies
        "@vaticle_typedb_common//:common",

        # External Maven Dependencies
        "@maven//:io_github_microutils_kotlin_logging_jvm",
        "@maven//:org_jetbrains_compose_runtime_runtime_desktop",
        "@maven//:org_jetbrains_kotlinx_kotlinx_coroutines_core_jvm",
        "@maven//:org_slf4j_slf4j_api",
    ],
)

checkstyle_test(
    name = "checkstyle",
    include = glob([
        "*",
        "*/*",
    ]),
    license_type = "agpl",
)
