@echo on
REM This Source Code Form is subject to the terms of the Mozilla Public
REM License, v. 2.0. If a copy of the MPL was not distributed with this
REM file, You can obtain one at https://mozilla.org/MPL/2.0/.

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

REM install code-signing tool used by tauri.conf.json's bundle.windows.signCommand
REM (signs via Azure Trusted Signing; auth comes from AZURE_INFRA_TENANT_ID/AZURE_INFRA_CLIENT_ID/AZURE_INFRA_CLIENT_SECRET)
cargo install trusted-signing-cli --locked

REM install node modules

CALL nvm install 22.16.0
CALL nvm use 22.16.0
CALL corepack enable
CALL corepack prepare pnpm@10.12.1 --activate
CALL pnpm config set store-dir .pnpm-store
CALL pnpm install

REM Azure Trusted Signing credentials must be present, or tauri's signCommand will fail mid-bundle.
REM Named AZURE_INFRA_* in CircleCI (we have multiple Azure subscriptions); trusted-signing-cli's
REM default Azure credential chain only recognizes the SDK-standard AZURE_* names, so rename here.
IF "%AZURE_INFRA_TENANT_ID%"=="" GOTO :missing_signing_creds
IF "%AZURE_INFRA_CLIENT_ID%"=="" GOTO :missing_signing_creds
IF "%AZURE_INFRA_CLIENT_SECRET%"=="" GOTO :missing_signing_creds
GOTO :signing_creds_ok
:missing_signing_creds
ECHO Error: AZURE_INFRA_TENANT_ID, AZURE_INFRA_CLIENT_ID, AZURE_INFRA_CLIENT_SECRET must be set to sign the Windows build
EXIT /b 1
:signing_creds_ok
SET AZURE_TENANT_ID=%AZURE_INFRA_TENANT_ID%
SET AZURE_CLIENT_ID=%AZURE_INFRA_CLIENT_ID%
SET AZURE_CLIENT_SECRET=%AZURE_INFRA_CLIENT_SECRET%

REM CI jobs may request a throwaway build version (e.g. snapshot testing) via STUDIO_VERSION
IF NOT "%STUDIO_VERSION%"=="" CALL pnpm set-version %STUDIO_VERSION%
IF %errorlevel% NEQ 0 EXIT /b %errorlevel%

REM compile MSI installer using tauri
CALL pnpm build
CALL npx tauri build
