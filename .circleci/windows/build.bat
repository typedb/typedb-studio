@echo off
REM
REM Copyright (C) 2021 Vaticle
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
CALL refreshenv

ECHO Building Windows application image...
bazel build //:application-image
IF %errorlevel% NEQ 0 EXIT /b %errorlevel%

ECHO Extracting application image archive...
mkdir ~/src
xcopy ../bazel-bin/application-image.zip ~/src
mkdir ~/dist
cd ~/dist
jar xf ~/src/application-image.zip
IF %errorlevel% NEQ 0 EXIT /b %errorlevel%
