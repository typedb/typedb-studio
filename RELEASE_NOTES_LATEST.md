### Distribution

TypeDB Studio is hosted on the Web at https://studio.typedb.com.

Alternatively:

- Install: https://typedb.com/docs/home/install/studio
- Direct download: https://cloudsmith.io/~typedb/repos/public-release/packages/?q=name:^typedb-studio+version:3.13.1

Desktop builds of TypeDB Studio run in the following environments:

- Windows 10 or later (x86_64)
- MacOS (x86_64, arm64)
- Debian / Ubuntu 22.04 or later (x86_64, arm64)

### Documentation

- TypeDB Studio docs: https://typedb.com/docs/tools/studio
- Learn more about TypeDB: https://typedb.com/docs/home/learning-journey

### TypeDB server compatibility

TypeDB Studio 3.13.1 is compatible with TypeDB >= 3.3. For older TypeDB versions, enquire on the TypeDB Discord chat server (https://typedb.com/discord).

---

## Bugs fixed

### Performance of long query texts (eg. data load scripts)

Running very long queries (for example, sample dataset loading) could previously make Studio unresponsive. That is fixed in this release, as we now **render only the first 40 lines in History pane queries** (you can still view the full query by expanding it).

### Performance of large query result sets

Queries returning a lot of data could previously make Studio unresponsive; this release fixes that.

- Log output is now lazily rendered
- Table output is now paginated
- Graph physics now runs off the main thread

### Other bugs fixed

- Graphs no longer attempt to render when WebGL is not available - graph pane shows a status warning instead
- Fix a stack overflow error when rendering a table with >64k rows
- Linux Debian package is now correctly named "typedb-studio". If you previously installed TypeDB Studio 3.13.0, please uninstall it: `sudo apt remove type-db-studio`. Versions prior to 3.13 were not affected by this bug.

## Other improvements

- Cache code editor's internal configuration to slightly improve code editor performance
- Tweak Sigma initialisation timing to slightly improve graph vis performance
