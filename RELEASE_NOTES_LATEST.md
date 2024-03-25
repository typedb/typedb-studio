### Distribution

TypeDB Studio is available for Linux, Mac and Windows. [Download TypeDB Studio 2.27.0.](https://cloudsmith.io/~typedb/repos/public-release/packages/?q=name:^typedb-studio+version:2.27.0)

For Mac Intel and Mac ARM, TypeDB Studio is also available through Homebrew:

```
brew tap vaticle/tap
brew install --cask vaticle/tap/typedb-studio
```

### TypeDB Server Compatibility

See the [compatibility table](https://typedb.com/docs/typedb/connecting/studio#_version_compatibility) to see
which versions of Studio are compatible with which versions of TypeDB server.

---


## New Features


## Bugs Fixed


## Code Refactors


## Other Improvements
- **Fix syntax highlighting for quoted strings**
  We fix the highlighting for string literals to make it yellow as it used to be. The highlighting had not been correct due to a bug introduced in TypeDB Studio 2.25.0. 
  
  Before:
  ![image](https://github.com/vaticle/typedb-studio/assets/22564079/f4a9165b-93c7-4115-8646-5f9352e232f0)
  
  After:
  ![image](https://github.com/vaticle/typedb-studio/assets/22564079/24d88b06-4ff5-4f44-9feb-f30131b9116e)
  
  
    

