### Distribution

TypeDB Studio is hosted on the Web at https://studio.typedb.com.

Alternatively:

- Install: https://typedb.com/docs/home/install/studio
- Direct download: https://cloudsmith.io/~typedb/repos/public-release/packages/?q=name:^typedb-studio+version:3.11.1

Desktop builds of TypeDB Studio run in the following environments:

- Windows 10 or later (x86_64)
- MacOS (x86_64, arm64)
- Debian / Ubuntu 22.04 or later (x86_64, arm64)

### Documentation

- TypeDB Studio docs: https://typedb.com/docs/tools/studio
- Learn more about TypeDB: https://typedb.com/docs/home/learning-journey

### TypeDB server compatibility

TypeDB Studio 3.11.1 is compatible with TypeDB >= 3.3. For older TypeDB versions, enquire on the TypeDB Discord chat server (https://typedb.com/discord).

---

## Graph Explorer

We add the Graph Explorer tool. This allows users to effectively visualise connections and explore graph-structured data without needing to touch TypeQL.

First, retrieve entities, relations or attributes (all, or by type). Then, click any node to inspect its type. The new "Explorer" pane in the graph side panel exposes details of the type's capabilities, and allows you to load and unload connections at various levels of granularity. You can also switch the Explorer pane to focus on the individual instance, which will reveal details of the instance's direct connections and allow you to add them to the graph.

Exploration features are also available by simply right-clicking on any node in the graph. This includes a compact UI to easily customise node color per type.

Exploration features are available in Query and Agent Modes as well as Graph Explorer. We also make various general improvements to graph visualisation.

## New features

- **Graph Explorer**
- **Pretty node labels**: Entity and relation nodes in graphs now use a heuristic to show meaningful node labels (e.g: "person: John Doe"). You can manually select a display attribute for each type in the Explorer pane.
- The **graph side panel** can now be **docked** to either the right or the bottom
- Add ability to **customise graph density** (spacious, default, or compact)
- New graph visualisation theme: **Cal Aesthetics**; new node shape: hexagon; new customisation option: 'always curve edges'
- Add **Saved Queries page**. Add ability to save queries in Query Editor and Agent Mode.
- Schema tree now has a search feature
- Data Explorer: Add ability to load all entities, relations or attributes
- **Add AI "consent wall"**: AI features will now require explicit user opt-in.

## Bugs fixed

- Fix a bug that caused errors to pop up while typing in the TypeQL editor
- Fix a caching issue with Sigma node programs that could cause intermittent errors
- Fix a bug where if either vertex of a logical edge was unavailable, both vertices would be hidden
- Fix visual clipping in query limit box
- Fix visual clipping in Agent Mode header area
- Mouse wheel now zooms instead of panning the graph

## Other improvements

- Add link to load sample datasets when schema is empty
- Slightly restructure graph side panel to save space
