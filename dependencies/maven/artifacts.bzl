# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at https://mozilla.org/MPL/2.0/.

artifacts = [
    "ch.qos.logback:logback-classic",
    "ch.qos.logback:logback-core",
    "io.github.microutils:kotlin-logging-jvm",
    "junit:junit",
    "org.antlr:antlr4-runtime",
    "org.slf4j:slf4j-api",
    "org.zeroturnaround:zt-exec",
]

# FIXME studio kotlin dependency is held back, out of sync with dependencies
version_overrides = [
    {"group": "org.jetbrains.compose.animation", "artifact": "animation-core-desktop", "version": "1.2.1"}, 
    {"group": "org.jetbrains.compose.compiler", "artifact": "compiler", "version": "1.3.2"}, 
    {"group": "org.jetbrains.compose.desktop", "artifact": "desktop-jvm", "version": "1.2.1"}, 
    {"group": "org.jetbrains.compose.foundation", "artifact": "foundation-desktop", "version": "1.2.1"}, 
    {"group": "org.jetbrains.compose.foundation", "artifact": "foundation-layout-desktop", "version": "1.2.1"}, 
    {"group": "org.jetbrains.compose.material", "artifact": "material-desktop", "version": "1.2.1"}, 
    {"group": "org.jetbrains.compose.runtime", "artifact": "runtime-desktop", "version": "1.2.1"}, 
    {"group": "org.jetbrains.compose.ui", "artifact": "ui-desktop", "version": "1.2.1"}, 
    {"group": "org.jetbrains.compose.ui", "artifact": "ui-geometry-desktop", "version": "1.2.1"}, 
    {"group": "org.jetbrains.compose.ui", "artifact": "ui-graphics-desktop", "version": "1.2.1"}, 
    {"group": "org.jetbrains.compose.ui", "artifact": "ui-test-desktop", "version": "1.2.1"}, 
    {"group": "org.jetbrains.compose.ui", "artifact": "ui-test-junit4-desktop", "version": "1.2.1"}, 
    {"group": "org.jetbrains.compose.ui", "artifact": "ui-text-desktop", "version": "1.2.1"}, 
    {"group": "org.jetbrains.compose.ui", "artifact": "ui-unit-desktop", "version": "1.2.1"}, 
    {"group": "org.jetbrains.kotlin", "artifact": "kotlin-test", "version": "1.7.20"}, 
    {"group": "org.jetbrains.kotlinx", "artifact": "kotlinx-coroutines-core", "version": "1.6.0"}, 
    {"group": "org.jetbrains.kotlinx", "artifact": "kotlinx-coroutines-core-jvm", "version": "1.6.0"}, 
    {"group": "org.jetbrains.kotlinx", "artifact": "kotlinx-coroutines-test", "version": "1.6.0"}, 
    {"group": "org.jetbrains.skiko", "artifact": "skiko-awt", "version": "0.7.34"}, 
    {"group": "org.jetbrains.skiko", "artifact": "skiko-awt-runtime-linux-arm64", "version": "0.7.34"}, 
    {"group": "org.jetbrains.skiko", "artifact": "skiko-awt-runtime-linux-x64", "version": "0.7.34"}, 
    {"group": "org.jetbrains.skiko", "artifact": "skiko-awt-runtime-macos-arm64", "version": "0.7.34"}, 
    {"group": "org.jetbrains.skiko", "artifact": "skiko-awt-runtime-macos-x64", "version": "0.7.34"}, 
    {"group": "org.jetbrains.skiko", "artifact": "skiko-awt-runtime-windows-x64", "version": "0.7.34"}
]
