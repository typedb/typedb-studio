### Distribution

TypeDB Studio is hosted on the Web at https://studio.typedb.com.

Alternatively:

- Install: https://typedb.com/docs/home/install/studio
- Direct download: https://cloudsmith.io/~typedb/repos/public-release/packages/?q=name:^typedb-studio+version:3.10.0

Desktop builds of TypeDB Studio run in the following environments:

- Windows 10 or later (x86_64)
- MacOS (x86_64, arm64)
- Debian / Ubuntu 22.04 or later (x86_64, arm64)

### Documentation

- TypeDB Studio docs: https://typedb.com/docs/tools/studio
- Learn more about TypeDB: https://typedb.com/docs/home/learning-journey

### TypeDB server compatibility

TypeDB Studio 3.10.0 is compatible with TypeDB >= 3.3. For older TypeDB versions, enquire on the TypeDB Discord chat server (https://typedb.com/discord).

---

## New features

### Graph visualiser

We've given graph visualisation a massive overhaul, with brand new physics, styling, and a wide range of customisation options.

We want TypeDB graphs to be both ergonomic and stunning. The previous iteration didn't quite live up to those standards, and we believe this iteration represents a significant level-up.

#### Physics

We migrate from the ForceAtlas2 Static placement algorithm from `graphology` to the Fruchterman-Reingold dynamic algorithm from `d3-force` and tune the parameters to fit different scenarios.

#### Controls

The new graph control panel allows you to set zoom, re-centre, restart/stop simulation, and search graph nodes by label. You can also highlight a node's connections by clicking it, or highlight all nodes of a kind or type from the Graph Styles panel. We've adjusted two-finger swipe behaviour on touchpads - it now pans the canvas. Finally, you can now expand the graph viewer to fullscreen mode.

#### Styling

Nodes have a new sleek default design - dark fill with a brighter border. We've brought back the labelled rectangles, diamonds and ellipses that are typically used to represent TypeDB concepts. Edges are now colourless by default, but it's possible to re-enable highlighting them by query constraint.

#### Customisation

The styles are heavily customisable. Choose from an array of prebuilt themes, optionally add further tweaks. You can set styles per kind or per type, and can customise the graph background. You can also create your own themes for future use.

## Bugs fixed

- Fresh query runs no longer open in a new Run tab unless the current Run tab is explicitly pinned
- Query Run tabs now remember what output type they had selected
- Fix a bug where some icons failed to render

## Other improvements

- Graph vis package refactor - for better readability and clearer structure
- Now depends on and pulls graph structure types from `@typedb/graph-utils` library
- Update `@codemirror/view` to `6.39.16` - may fix input issues in certain environments
