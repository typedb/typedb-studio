@echo on
REM This Source Code Form is subject to the terms of the Mozilla Public
REM License, v. 2.0. If a copy of the MPL was not distributed with this
REM file, You can obtain one at https://mozilla.org/MPL/2.0/.

REM shorten the workspace name so that we can avoid the long path restriction
git apply .circleci\windows\short_workspace.patch
IF %ERRORLEVEL% NEQ 0 EXIT /B %ERRORLEVEL%

REM uninstall Java 12 installed by CircleCI
choco uninstall openjdk --limit-output --yes --no-progress

REM install dependencies needed for build
choco install .circleci\windows\dependencies.config  --limit-output --yes --no-progress

REM create a symlink python3.exe and make it available in %PATH%
mklink C:\Python311\python3.exe C:\Python311\python.exe
set PATH=%PATH%;C:\Python311

REM install runtime dependency for the build
C:\Python311\python.exe -m pip install wheel

REM permanently set variables for Bazel build
SETX BAZEL_SH "C:\Program Files\Git\usr\bin\bash.exe"
SETX BAZEL_PYTHON C:\Python311\python.exe
SETX BAZEL_VC "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\VC"

REM install Rust
curl -L -o rustup-init.exe https://win.rustup.rs/x86_64
rustup-init.exe -y
set PATH=%USERPROFILE%\.cargo\bin;%PATH%
rustup install 1.93.1
rustup default 1.93.1

REM install node modules

CALL nvm install 22.16.0
CALL nvm use 22.16.0
CALL corepack enable
CALL corepack prepare pnpm@10.12.1 --activate
CALL pnpm config set store-dir .pnpm-store
CALL pnpm install

REM compile MSI installer using tauri
CALL pnpm build
CALL npx tauri build
