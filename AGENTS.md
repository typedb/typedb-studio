# Agent Instructions for TypeDB Studio

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

## General Development Guidelines

- Prefer extending existing components over creating new ones
- Follow established patterns in the codebase for consistency
- Do prompt for clarification when making architectural decisions
