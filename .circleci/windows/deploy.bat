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

ECHO Creating release notes...
SET RELEASE_NOTES_TOKEN=%REPO_GITHUB_TOKEN%
SET /p VERSION=<VERSION
bazel run @vaticle_dependencies//tool/release:create-notes -- typedb-workbase %VERSION% ./RELEASE_TEMPLATE.md
IF %errorlevel% NEQ 0 EXIT /b %errorlevel%

ECHO Creating application image...
bazel build //:application-image
IF %errorlevel% NEQ 0 EXIT /b %errorlevel%

ECHO the distribution file will appear in CircleCI artifacts shortly
ECHO TODO make //:deploy-github work on Windows
REM ECHO Deploying to GitHub...
REM SET DEPLOY_GITHUB_TOKEN=%REPO_GITHUB_TOKEN%
REM SET COMMIT_ID=%CIRCLE_SHA1%
REM bazel run //:deploy-github-windows
REM IF %errorlevel% NEQ 0 EXIT /b %errorlevel%
