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

## General Agent Guidelines

- Do prompt for clarification when making architectural decisions
- Don't redirect shell script output to 'nul' on Windows
