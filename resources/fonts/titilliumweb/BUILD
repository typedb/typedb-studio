# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

load("@typedb_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")

package(default_visibility = ["//framework/common:__pkg__"])

filegroup(
    name = "titillium-web",
    srcs = [
        "TitilliumWeb-Light.ttf",
        "TitilliumWeb-Regular.ttf",
        "TitilliumWeb-SemiBold.ttf",
    ]
)

checkstyle_test(
    name = "checkstyle",
    include = glob(["*"]),
    exclude = glob(["*.ttf"]),
    license_type = "mpl-header",
)
