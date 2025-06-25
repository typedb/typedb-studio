# TypeDB Studio

[![Netlify Status](https://api.netlify.com/api/v1/badges/5e9c0038-d5ec-48d8-8217-27654feae68c/deploy-status)](https://app.netlify.com/sites/typedb-studio/deploys)
[![CircleCI](https://circleci.com/gh/typedb/typedb-studio/tree/master.svg?style=shield)](https://circleci.com/gh/typedb/typedb-studio/tree/master)
[![GitHub release](https://img.shields.io/github/release/typedb/typedb-studio.svg)](https://github.com/typedb/typedb-studio/releases/latest)
[![Discord](https://img.shields.io/discord/665254494820368395?color=7389D8&label=discord&logo=discord&logoColor=ffffff)](https://typedb.com/discord)

TypeDB Studio is a graphical tool for managing and querying [TypeDB](https://typedb.com) databases.

With Studio, developers can efficiently manage databases, execute queries, and explore query results,
all within a unified environment.

- [Install TypeDB Studio Desktop](https://typedb.com/docs/home/install-tools#_studio)
- [Open studio.typedb.com](https://studio.typedb.com)
- [Read docs](https://typedb.com/docs/manual/tools/studio)

## Quickstart

### Connect to TypeDB

Select TypeDB edition below, and follow the instructions:

#### Cloud
1. In the TypeDB Cloud website, navigate to your cluster and click *Connect*. Then, click *Connect with TypeDB Studio*. This will launch TypeDB Studio.
2. Fill in your password and hit *Connect*. Your password can be found in your downloaded credentials file (if you have one).

#### Enterprise
1. Launch TypeDB Studio.
2. Enter the address of the HTTP endpoint of your cluster. By default, this is at port 8000.
3. Enter your username and password.
4. Click `Connect`.

#### Community Edition
1. Launch TypeDB Studio.
2. Enter the address of the HTTP endpoint of your cluster. By default, this is at port 8000 and for local instances you can use `http://localhost:8000`.
3. Enter your username and password - defaults are `admin` and `password`.
4. Click `Connect`.

### Select a database

To select a database to work with, use the dropdown menu on the right of the database icon in the top toolbar. You can also create new databases here.

TypeDB Studio will automatically select a *default* database if there are no others present.

## Build from source

TypeDB Studio is a Web application powered by [Angular](https://angular.dev), with desktop application support provided by [Tauri](https://tauri.app).

There is a wide variety of Web toolchains; the process below is one way to compile TypeDB Studio from source.

### Install toolchains and dependencies

1. Install [nvm](https://github.com/nvm-sh/nvm) on MacOS or Linux, [nvm-windows](https://github.com/coreybutler/nvm-windows) on Windows.
2. `nvm install 22.16.0`
3. `nvm use 22.16.0`
4. `npm install --global corepack@0.17.0`
5. `corepack enable`
6. `corepack prepare pnpm@10.12.1 --activate`
7. `pnpm i -g @angular/cli`
8. `pnpm i`
9. (Optional) Install [Rust](https://www.rust-lang.org/tools/install). Only required if you want to compile as a desktop application.

### Launch local development server (Angular)

```sh
ng serve --open
```

### Other build commands

Launch Tauri server for local development of desktop app:
```sh
npx tauri dev
```

Build web app distribution:
```sh
pnpm build
```

Build desktop app distribution:
```sh
npx tauri build
```

_Instructions are accurate at the time of writing (25 Jun 2025); see [.circleci/config.yml](.circleci/config.yml) and [.circleci/prepare.bat](.circleci/prepare.bat) for the most up-to-date build process that we use in our CircleCI automation._
