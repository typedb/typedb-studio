### Distribution

TypeDB Studio is available for Linux, Mac and Windows (download binaries below).

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
- **Gracefully handle session failover**
  
  Gracefully handle session failover: session automatically reconnects and the UI is updated accordingly.
  
  
- **Update to driver with fixed error messages and ability to use system CA**

  We update typedb-driver to 2.25.8, which has error UX improvements and the ability to read system certificates when connecting to TypeDB.
  
  

## Bugs Fixed


## Code Refactors


## Other Improvements
- **Update to use vaticle/typedb-driver commit**

- **Unable to connect error carries driver message**

- **Update github PR and issue templates**

    

