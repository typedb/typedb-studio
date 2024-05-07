### Distribution

TypeDB Studio is available for Linux, Mac and Windows. [Download TypeDB Studio 2.28.0.](https://cloudsmith.io/~typedb/repos/public-release/packages/?q=name:^typedb-studio+version:2.28.0)

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
- **Cloud address translation**
  
  We introduce a way to provide address translation when attempting to connect to cloud servers (cf. https://github.com/vaticle/typedb-driver/pull/624). This is useful when the route from the user to the servers differs from the route the servers are configured with (e.g. connection to public-facing servers from an internal network).
  
  Note: we currently require that the user provides translation for the addresses of _all_ nodes in the Cloud deployment.
  
  <img width="532" src="https://github.com/vaticle/typedb-studio/assets/18616863/74859fbd-de4f-4844-b1e6-f3507dc364b7">
  

- **Store cursor position when changing lines**
  
  We store cursor positions when changing lines as described in https://github.com/vaticle/typedb-studio/issues/748. This is standard behaviour in other IDEs, such as IntelliJ IDEA and VSCode.
  
  Before:
  
  https://github.com/vaticle/typedb-studio/assets/51956016/9a4232b2-fb41-4276-9b48-76d741143329
  
  After:
  
  https://github.com/vaticle/typedb-studio/assets/51956016/0f311437-15f4-4a66-96d8-0005dedc6ad7
  
## Bugs Fixed


## Code Refactors


## Other Improvements
- **Fix Windows short workspace git patch**

- **Replace licenses with MPL version 2.0**

