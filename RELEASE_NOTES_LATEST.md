### Distribution

TypeDB Studio is available for Linux, Mac and Windows. [Download TypeDB Studio 2.26.6-rc3.](https://cloudsmith.io/~typedb/repos/public-release/packages/?q=name:^typedb-studio+version:2.26.6-rc3)

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
- **Deploy artifacts to CloudSmith**
  
  We no longer upload build artifacts to the github releases page. Instead, the artifacts are available from our public cloudsmith repository, linked in the release notes.
  
- **Migrate artifact hosting to cloudsmith**
  Updates artifact deployment & consumption rules to use cloudsmith (repo.typedb.com) instead of the self-hosted sonatype repository (repo.vaticle.com).
  
- **Update typedb-driver with null fix**

