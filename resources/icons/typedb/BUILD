# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

load("@typedb_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")

package(default_visibility = ["//:__pkg__", "//framework:__pkg__"])

filegroup(
    name = "typedb-bot-linux",
    srcs = ["256x256.png"], # this file name has to be 256x256.png to work in Linux
)

filegroup(
    name = "typedb-bot-mac",
    srcs = ["typedb-bot.icns"],
)

filegroup(
    name = "typedb-bot-windows",
    srcs = ["typedb-bot.ico"],
)

filegroup(
    name = "typedb-bot-32px",
    srcs = ["typedb-bot-32px.png"],
)

checkstyle_test(
    name = "checkstyle",
    include = glob(["*"]),
    exclude = glob(["*.png", "*.icns", "*.ico"]),
    license_type = "mpl-header",
)
