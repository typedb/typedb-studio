[![Factory](https://factory.vaticle.com/api/status/vaticle/typedb-studio/badge.svg)](https://factory.vaticle.com/vaticle/typedb-studio)
[![Discord](https://img.shields.io/discord/665254494820368395?color=7389D8&label=chat&logo=discord&logoColor=ffffff)](https://typedb.com/discord)
[![Discussion Forum](https://img.shields.io/discourse/https/forum.typedb.com/topics.svg)](https://forum.typedb.com)
[![Stack Overflow](https://img.shields.io/badge/stackoverflow-typedb-796de3.svg)](https://stackoverflow.com/questions/tagged/typedb)
[![Stack Overflow](https://img.shields.io/badge/stackoverflow-typeql-3dce8c.svg)](https://stackoverflow.com/questions/tagged/typeql)

## TypeDB Studio

[![TypeDB Studio](./docs/images/studio-full-1.png)](./docs/images/studio-full-1.png)

TypeDB Studio is a fully-featured Integrated Development Environment (IDE) for [TypeDB](https://github.com/vaticle/typedb).

You can utilise Studio as you would [TypeDB Console](https://github.com/vaticle/typedb-console)
and the [TypeDB Driver](https://typedb.com/docs/drivers/2.x/drivers): to connect to your TypeDB instance and
perform queries.

Studio is an IDE designed for the end-to-end development of TypeDB databases, database queries, and data exploration,
via a clean and intuitive UI that gives you a top-down view of your entire database.

## Usage

### Connect to TypeDB

Upon launching Studio, you'll be asked for your TypeDB server details and to choose or create a database.

### Manage TypeDB Projects

After connecting to TypeDB and selecting a database, Studio will prompt you to open (or create) a project directory.
This is where Studio will load and save query files, but you can also freely edit text files in the project directory
using Studio. The project browser maintains a live view of the directory contents on the filesystem.

### Manage Database Schemas

[![Manage Database Schemas](./docs/images/type-browser-1.gif)](./docs/images/type-browser-1.gif)

The Type Browser provides a structured tree view of the connected database's type hierarchy. Double-clicking on any of
the listed types will bring up a page that lists their properties in detail — supertype, roles played, attributes owned
and more.

### Run TypeQL Queries

[![Log Output](./docs/images/log-output-1.gif)](./docs/images/log-output-1.gif)

Studio's text editor comes equipped with rich developer tooling, like syntax highlighting
for TypeQL files (\*.tql), with more advanced features currently under development. Studio's intuitive and
self-explanatory interface enables a rapid, iterative workflow.

TypeDB Studio brings session and transaction configuration to the forefront, providing greater visibility to the user
about the state of the system. Studio allows you to run any TypeQL query, pretty-printing results
to the Log Output window which is easily searchable (and supports regex matching.)

### Graph Visualisation

[![Graph Visualisation](./docs/images/graph-vis-1.gif)](./docs/images/graph-vis-1.gif)

Reasoning about text outputs in [TypeDB Console](https://docs.typedb.com/docs/console/console) or through
the various [TypeDB Drivers](https://docs.typedb.com/docs/driver-api/overview) can be difficult to do for large
datasets. Studio's advanced force-graph visualisation makes the underlying structure of data immediately apparent.

### Reasoning and Explanations

[![Inference Visualisation](./docs/images/infer-vis-1.gif)](./docs/images/infer-vis-1.gif)

Select infer and explain, then double-click highlighted inferred concepts to retrieve their explanations and visualise
how the fact was inferred.

## Download TypeDB Studio

You can download TypeDB Studio from the [Download Centre](https://typedb.com/download#typedb-studio)
or [GitHub Releases](https://github.com/vaticle/typedb-studio/releases).

## Developer Resources

- Documentation: https://docs.typedb.com
- Discussion Forum: https://forum.typedb.com
- Discord Chat Server: https://typedb.com/discord
- Community Projects: https://github.com/typedb-osi

## Compiling and Running TypeDB Studio from Source

> Note: You **DO NOT NEED** to compile TypeDB Studio _"from source"_ if you just want to use TypeDB Studio. You can
> simply download TypeDB Studio following the section above.

1. Make sure you have the following dependencies installed on your machine:
    - Java JDK 11 or higher
    - [Bazel 6 or higher](http://bazel.build/). We recommend installing it using [Bazelisk](https://github.com/bazelbuild/bazelisk),
      which manages multiple Bazel versions transparently. Bazelisk runs the appropriate Bazel version for any `bazel` command as
      specified in [`.bazelversion`](https://github.com/vaticle/typedb/blob/master/.bazelversion) file. 
 
2. Depending on your Operating System, you can compile and run TypeDB Studio with either one of the following commands.
   ```sh
   $ bazel run //:studio-bin-mac-arm64  # for Apple Silicon
   $ bazel run //:studio-bin-mac-x86_64  # for Intel
   ```
   ```sh
   $ bazel run //:studio-bin-windows-x86_64
   ```
   ```sh
   $ bazel run //:studio-bin-linux-arm64  # for ARM
   $ bazel run //:studio-bin-linux-x86_64  # for Intel
   ```
   You can also replace `run` with `build` in the command above, and Bazel will simply produce the JAR for TypeDB Studio
   under `bazel-bin/studio-bin-<mac|windows|linux>-<arm64|x86_64>.jar`.

## Contributions

TypeDB Studio has been built using various open-source frameworks throughout its evolution. Today TypeDB Studio is built
using [Kotlin](https://kotlinlang.org), [Compose Multiplatform](https://github.com/JetBrains/compose-jb),
and [Bazel](https://bazel.build). Thank you to the developers!

## Licensing

This software is developed by [Vaticle](https://typedb.com/). It's released under the GNU Affero GENERAL PUBLIC
LICENSE, Version 3, 19 November 2007. For license information, please
see [LICENSE](https://github.com/vaticle/typedb-studio/blob/master/LICENSE). Vaticle also provides a commercial license
for TypeDB Studio - get in touch with our team at commercial@vaticle.com.

Copyright (C) 2022 Vaticle
