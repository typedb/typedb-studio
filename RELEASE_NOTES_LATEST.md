### Distribution

TypeDB Studio is hosted on the Web at https://studio.typedb.com.

Alternatively:

- Install: https://typedb.com/docs/home/install/studio
- Direct download: https://cloudsmith.io/~typedb/repos/public-release/packages/?q=name:^typedb-studio+version:3.10.3

Desktop builds of TypeDB Studio run in the following environments:

- Windows 10 or later (x86_64)
- MacOS (x86_64, arm64)
- Debian / Ubuntu 22.04 or later (x86_64, arm64)

### Documentation

- TypeDB Studio docs: https://typedb.com/docs/tools/studio
- Learn more about TypeDB: https://typedb.com/docs/home/learning-journey

### TypeDB server compatibility

TypeDB Studio 3.10.3 is compatible with TypeDB >= 3.3. For older TypeDB versions, enquire on the TypeDB Discord chat server (https://typedb.com/discord).

---

## New features

### Multi-queries

Studio now supports running multi-queries (multiple queries separated by the `end;` marker to be run sequentially).

### Load sample datasets

Studio now offers UI actions for loading sample datasets. These are always loaded into a newly created database. The current sample datasets on offer are Bookstore, Cyber Threat Intelligence, and Robotics.

### Other features

- TypeQL editor now offers 'find and replace'
- Add a transaction inspector that shows the state of the current or most recent transaction
- Add ability to configure transaction timeout (default is 5 minutes)

## Bugs fixed

- Fixed various UI elements being low-contrast in light mode
- Fixed various TypeQL tokens not being syntax-highlighted
- Fixed a few cases where the current query tab's header would not correctly scroll into view
- Committing a schema transaction now auto-refreshes schema views
- Log output now auto-scrolls to the end unless manually scrolled
- Fixed a bug where Data Explorer would fail to load when viewing a type with no playable relation types (reported by `@markmclellan` on Discord)
- Schema tool window now sorts capabilities by type, then alphabetically (reported by `@markmclellan` on Discord)
- Fix a bug where non-admin users couldn't load the Users page (to change their own password)
- Fix a bug where Studio would hang on startup if the last used server was unresponsive

## Other improvements

- Studio will now try to detect transaction type when in auto-transaction mode (instead of sending everything as a read query and escalating on failure)
- Connecting to a server with no databases no longer results in Studio automatically creating a 'default' database. While this was convenient for some users, it was odd behaviour
- The Run button and Limit setting now take up much less space (reported by `@markmclellan` on Discord)
- When trying to connect to an HTTP server over HTTPS, warn that this most likely won't work and suggest using Studio Desktop
- Enforce a max answer limit of 100k to tackle server resource starvation
