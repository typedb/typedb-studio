@echo off
REM
REM GRAKN.AI - THE KNOWLEDGE GRAPH
REM Copyright (C) 2019 Grakn Labs Ltd
REM
REM This program is free software: you can redistribute it and/or modify
REM it under the terms of the GNU Affero General Public License as
REM published by the Free Software Foundation, either version 3 of the
REM License, or (at your option) any later version.
REM
REM This program is distributed in the hope that it will be useful,
REM but WITHOUT ANY WARRANTY; without even the implied warranty of
REM MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
REM GNU Affero General Public License for more details.
REM
REM You should have received a copy of the GNU Affero General Public License
REM along with this program.  If not, see <https://www.gnu.org/licenses/>.
REM

REM needs to be called such that software installed
REM by Chocolatey in prepare.bat is accessible

REM extract the grakn-core-all-windows artifact
bazel run //:grakn-extractor-windows --enable_runfiles=yes -- dist/grakn-core-all-windows || GOTO :error

REM start Grakn server
PUSHD dist\grakn-core-all-windows\
CALL grakn.bat server start || GOTO :error
POPD

REM run unit tests
bazel run @nodejs//:bin/yarn.cmd -- run unit || GOTO :error

REM run integration tests
bazel run @nodejs//:bin/yarn.cmd -- run integration || GOTO :error

REM FIXME(vmax): e2e tests on Windows fail due to
REM 'too long with no output' error
REM Once the cause is identified, they should be reenabled

REM run e2e tests
REM bazel run @nodejs//:bin/yarn.cmd -- run e2e || GOTO :error

REM stop Grakn server
PUSHD bazel-bin\dist\grakn-core-all-windows\
CALL grakn.bat server stop
POPD

REM build Grakn Workbase
bazel run @nodejs//:bin/yarn.cmd -- run build || GOTO :error

REM store Grakn Workbase for subsequent deployment
MKDIR distribution
set /p version=<VERSION
MOVE .\build\GRAKNW~1.EXE distribution\grakn-workbase-windows-%version%.exe || GOTO :error

REM verify that Grakn Workbase executable file was successfully copied
DIR distribution\

:error
IF %errorlevel% NEQ 0 EXIT /b %errorlevel%
