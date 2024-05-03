REM This Source Code Form is subject to the terms of the Mozilla Public
REM License, v. 2.0. If a copy of the MPL was not distributed with this
REM file, You can obtain one at https://mozilla.org/MPL/2.0/.

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

SET DEPLOY_ARTIFACT_USERNAME=%REPO_TYPEDB_USERNAME%
SET DEPLOY_ARTIFACT_PASSWORD=%REPO_TYPEDB_PASSWORD%

bazel --output_user_root=C:/b run //:deploy-windows-x86_64-exe --compilation_mode=opt -- snapshot
IF %errorlevel% NEQ 0 EXIT /b %errorlevel%
