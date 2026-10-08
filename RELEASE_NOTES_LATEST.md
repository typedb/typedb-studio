### Distribution

TypeDB Studio is hosted on the Web at https://studio.typedb.com.

Alternatively:

- Install: https://typedb.com/docs/home/install/studio
- Direct download: https://cloudsmith.io/~typedb/repos/public-release/packages/?q=name:^typedb-studio+version:3.13.2

Desktop builds of TypeDB Studio run in the following environments:

- Windows 10 or later (x86_64)
- MacOS (x86_64, arm64)
- Debian / Ubuntu 22.04 or later (x86_64, arm64)

### Documentation

- TypeDB Studio docs: https://typedb.com/docs/tools/studio
- Learn more about TypeDB: https://typedb.com/docs/home/learning-journey

### TypeDB server compatibility

TypeDB Studio 3.13.2 is compatible with TypeDB >= 3.3. For older TypeDB versions, enquire on the TypeDB Discord chat server (https://typedb.com/discord).

---

## New features

- Sub-schema visualisation: right click any type in the schema tree in Schema Visualiser to open options to visualise a subset of your schema.

## Bugs fixed

- Fix degraded performance in Agent Mode after running queries with long outputs
- Syntax highlighter should no longer highlight keywords that are part of another word (e.g. relation-ship). `@doc` and `@card` are now highlighted correctly.
- Right-clicking an attribute node in a graph no longer brings up a blank menu
- Fix a bug where Data Explorer search would error if the search string contained a double quote
- Suppress auto-reconnect when the entry URL of Studio has an address query parameter that differs from the stored connection parameters
- Graph exploration in Query Editor's graph output now correctly updates explored states in nodes' context menus, and is now undoable
- Fix a bug where hitting Ctrl+F in Query Editor's code editor would additionally open a search box in the schema tree view
- Fix a bug where various graph actions would stop working after changing the dock position of the graph side panel

## Other improvements

- Improve overall UI performance
