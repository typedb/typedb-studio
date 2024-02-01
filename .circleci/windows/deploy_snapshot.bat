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

REM TODO Temporary measure. This exists for two reasons:
REM 1) platform-jvm assembly rules currently requires the version to be specified in a version_file
REM 2) jpackage does not support 0 as a major version
REM This writes VERSION-SHA1 into the VERSION file to be used by the assembly rule.
FOR /F "tokens=*" %%V IN (VERSION) DO (SET VERS=%%V)
ECHO %VERS%-%CIRCLE_SHA1%> VERSION

bazel --output_user_root=C:/b run //:deploy-windows-x86_64-exe --compilation_mode=opt -- snapshot
IF %errorlevel% NEQ 0 EXIT /b %errorlevel%
