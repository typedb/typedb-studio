@echo off
REM This Source Code Form is subject to the terms of the Mozilla Public
REM License, v. 2.0. If a copy of the MPL was not distributed with this
REM file, You can obtain one at https://mozilla.org/MPL/2.0/.

REM shorten the workspace name so that we can avoid the long path restriction
git apply .circleci\windows\short_workspace.patch

REM uninstall Java 12 installed by CircleCI
choco uninstall openjdk --limit-output --yes --no-progress

REM install dependencies needed for build
choco install .circleci\windows\dependencies.config  --limit-output --yes --no-progress

REM create a symlink python3.exe and make it available in %PATH%
mklink C:\Python39\python3.exe C:\Python39\python.exe
SET PATH=%PATH%;C:\Python39

REM install runtime dependency for the build
C:\Python39\python.exe -m pip install wheel

REM permanently set variables for Bazel build
SETX BAZEL_SH "C:\Program Files\Git\usr\bin\bash.exe"
SETX BAZEL_PYTHON C:\Python39\python.exe
