#
# Copyright (C) 2021 Vaticle
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

package(default_visibility = ["//:__pkg__", "//view:__pkg__"])

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

checkstyle_test(
    name = "checkstyle",
    include = glob(["*"]),
    exclude = glob(["*.png", "*.icns", "*.ico"]),
    license_type = "agpl",
)
