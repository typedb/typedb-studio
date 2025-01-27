### Distribution

TypeDB Studio is available for Linux, Mac and Windows operating systems.

- Installation instructions: https://typedb.com/docs/home/install-tools#_studio
- Direct download: https://cloudsmith.io/~typedb/repos/public-release/packages/?q=name:^typedb-studio+version:3.0.4
- User manual: https://typedb.com/docs/manual/tools/studio

### TypeDB Server Compatibility

TypeDB Studio 3.0 is compatible with TypeDB 3.0.

See the [compatibility table](https://typedb.com/docs/manual/tools/studio#_version_compatibility) to see
which versions of Studio are compatible with which versions of TypeDB server.

---


## New Features


## Bugs Fixed
- **Fix printing of variables with empty values and rows with no columns**
  Enhance printing logic to handle two special cases of received answers:
    * When a variable with no values is returned, an empty result will be shown instead of a crash (it used to be considered an impossible situation).
    * When concept rows are returned, but they do not have any columns inside (possible for delete stages), a special message is written for every row processed (this behavior is temporarily different from Console).



## Code Refactors


## Other Improvements

    
