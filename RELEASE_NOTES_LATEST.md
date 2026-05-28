### Distribution

TypeDB Studio is hosted on the Web at https://studio.typedb.com.

Alternatively:

- Install: https://typedb.com/docs/home/install/studio
- Direct download: https://cloudsmith.io/~typedb/repos/public-release/packages/?q=name:^typedb-studio+version:3.10.4

Desktop builds of TypeDB Studio run in the following environments:

- Windows 10 or later (x86_64)
- MacOS (x86_64, arm64)
- Debian / Ubuntu 22.04 or later (x86_64, arm64)

### Documentation

- TypeDB Studio docs: https://typedb.com/docs/tools/studio
- Learn more about TypeDB: https://typedb.com/docs/home/learning-journey

### TypeDB server compatibility

TypeDB Studio 3.10.4 is compatible with TypeDB >= 3.3. For older TypeDB versions, enquire on the TypeDB Discord chat server (https://typedb.com/discord).

---

## New features

- **Export query output**: You can now copy or download a JSON or CSV export of your query results. Additionally, you can now easily export a result graph as a PNG
- **Graph Elements view**: Shows how many of each element type is in the currently rendered graph and allows higlighting specific types. Replaces and extends the functionality of the old Highlights view
- **History page**: Shows the timestamps, execution times and results of all queries in the current Studio session. Full-page version of the existing History pane on the Query page
- Connection page now prefills username and server address with the current connection details, if available
- Sharply improve app performance on large schemas

## Bugs fixed

- Fix a bug where schemas with over 10,000 types would not render properly in tooling (reported by `@richard_clouter` on Discord)
- Sorting table columns now works for multi-queries
- Fix a bug where Studio would erroneously claim it had "Committed" read-only multi-queries
- Fix a bug where Fetch queries' table output could incorrectly omit some columns
