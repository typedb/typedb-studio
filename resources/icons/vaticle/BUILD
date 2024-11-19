# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

load("@vaticle_dependencies//tool/checkstyle:rules.bzl", "checkstyle_test")

package(default_visibility = ["//:__pkg__", "//framework:__pkg__"])

filegroup(
    name = "vaticle-bot-linux",
    srcs = ["256x256.png"], # this file name has to be 256x256.png to work in Linux
)

filegroup(
    name = "vaticle-bot-mac",
    srcs = ["vaticle-bot.icns"],
)

filegroup(
    name = "vaticle-bot-windows",
    srcs = ["vaticle-bot.ico"],
)

filegroup(
    name = "vaticle-bot-32px",
    srcs = ["vaticle-bot-32px.png"],
)

filegroup(
    name = "vaticle-bot-256px",
    srcs = ["256x256.png"],
)

checkstyle_test(
    name = "checkstyle",
    include = glob(["*"]),
    exclude = glob(["*.png", "*.icns", "*.ico"]),
    license_type = "mpl-header",
)
