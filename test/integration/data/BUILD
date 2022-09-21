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

package(default_visibility = ["//test/integration:__subpackages__"])

kt_jvm_library(
    name = "paths",
    srcs = glob(["Paths.kt"]),
)

filegroup(
    name = "sample-file-structure-files",
    srcs = glob(["sample_file_structure/*", "sample_file_structure/*/*"]),
    visibility = ["//test/integration:__subpackages__"],
)

filegroup(
    name = "sample-github-data-files",
    srcs = [
        "sample_github_data/github_collaborators_query.tql",
        "sample_github_data/github_schema.tql",
        "sample_github_data/github_data.tql",
    ],
    visibility = ["//test/integration:__subpackages__"],
)

checkstyle_test(
    name = "checkstyle",
    include = glob(["*", "sample_github_data/*"]),
    exclude = glob(["sample_file_structure/*", "sample_file_structure/*/*"]),
    license_type = "agpl-header",
)