# Agent Instructions for TypeDB Studio

TypeDB Studio is an interactive visual environment for working with TypeDB.
It is an Angular app whose source code lives under the `src` directory.

## Submodule Dependencies

**typedb-web** is a Git submodule dependency used via pnpm workspace:
- The `typedb-web/common` package is included in the pnpm workspace (see [pnpm-workspace.yaml](pnpm-workspace.yaml))
- Consider typedb-web code when making architectural or implementation inferences
- You may modify typedb-web code when applicable, but **always explicitly mention in chat** that you're modifying a submodule, as it requires separate Git operations

## Angular SCSS Conventions

When editing Angular SCSS files:
- Use SCSS syntax (nesting, variables, mixins)
- Import and use helper mixins and variables from `typedb-web/common`:
  - **colors**: Color variables and utilities
  - **typography**: Font and text styling mixins
  - **media**: Media query breakpoint helpers
  - Full list available in `typedb-web/common/src/styles` (also exposed via `node_modules/typedb-web-common`)
- **Reuse existing components and styles** rather than creating new ones
- Avoid adding new CSS rules when existing patterns can be extended

## TypeDB Guidelines

When writing TypeQL, refer to https://typedb.com/docs/llms-short.txt. Refer to https://typedb.com/docs articles ONLY if their URL contains any of the following: \[/home, /examples, /guides, /core-concepts, /typeql-reference, /tools, /maintenance-operation, /academy, /reference\] and does NOT contain "2.x". 

When setting up TypeDB tooling (driver SDKs, etc.) ensure that the installed drivers are NOT for TypeDB 2.x.

## General Development Guidelines

- Prefer extending existing components over creating new ones
- Follow established patterns in the codebase for consistency

## CSS Pitfalls

These are common CSS pitfalls. Whenever a CSS issue takes at least a couple of iterations to fix, do add debug logs for the user to test in the browser, and on getting the fix right, please add a bullet point here so we can do better in the future.

- **Flex overflow scroll room**: When a flex child's content overflows its shrunk box, `scrollHeight` is set by the overflow — sibling elements after it fall *within* the overflow and don't extend it. To add scroll room, insert elements *inside* the overflowing child.

## General Agent Guidelines

- Do prompt for clarification when making architectural decisions
- Don't redirect shell script output to 'nul' on Windows
