### Distribution

TypeDB Studio is available for Linux, Mac and Windows (download binaries below).

For Mac Intel and Mac ARM, TypeDB Studio is also available through Homebrew:

```
brew tap vaticle/tap
brew install --cask vaticle/tap/typedb-studio
```

### TypeDB Server Compatible Versions

See the [compatibility table](https://typedb.com/docs/clients/2.x/studio#_version_compatibility) at our Studio
documentation for which versions of Studio are compatible with which versions of TypeDB.

---


## New Features


## Bugs Fixed
- **Fix unpacking driver runtime files into working directory**
  
  Due to a bug in the Java driver, we unpacked the driver dynamic library into the current working directory of Studio, rather than into a temporary directory. This was at best confusing, and at worst caused an unrecognized error in cases where the Studio was run from a read-only directory (such as `/Applications/` on mac OS.
  
  We update to the driver version with the fix.
  

## Code Refactors


## Other Improvements

    

