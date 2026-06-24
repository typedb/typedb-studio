### Distribution

TypeDB Studio is hosted on the Web at https://studio.typedb.com.

Alternatively:

- Install: https://typedb.com/docs/home/install/studio
- Direct download: https://cloudsmith.io/~typedb/repos/public-release/packages/?q=name:^typedb-studio+version:3.11.2

Desktop builds of TypeDB Studio run in the following environments:

- Windows 10 or later (x86_64)
- MacOS (x86_64, arm64)
- Debian / Ubuntu 22.04 or later (x86_64, arm64)

### Documentation

- TypeDB Studio docs: https://typedb.com/docs/tools/studio
- Learn more about TypeDB: https://typedb.com/docs/home/learning-journey

### TypeDB server compatibility

TypeDB Studio 3.11.2 is compatible with TypeDB >= 3.3. For older TypeDB versions, enquire on the TypeDB Discord chat server (https://typedb.com/discord).

---

## New features

- Make graph visualisations settle faster
- Improve rendering performance of curved graph edges
- Graph nodes now push each other out of the way as you drag them
- Add ability to individually toggle graph node and edge labels on/off 

## Bugs fixed

- Graph nodes now require a minimum movement threshold to trigger a drag action
- Fix checkboxes and paginators being low-contrast in light mode
- Fix graph background sometimes not being responsive to light/dark mode
- Graph control tooltips no longer obstruct the controls themselves
- Refresh WebGL context on window focus

## Other improvements

- Fixed a bug where the TypeDB Studio desktop download link didn't work for some users
