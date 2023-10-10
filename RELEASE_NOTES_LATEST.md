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
- **Update MacOS DMG signing and notarization process via bazel-distribution**
  
  We update the packaging and signing tools used by `//:assemble-platform` to correctly sign the MacOS package vis-à-vis changes in JVM17, XCode 13+, and Apple notarization process. See https://github.com/vaticle/bazel-distribution/pull/391 for more details.
  
- **Windows build fixes**
  
  We shorten bazel workspace path to work around the character limit on Windows, and enable runfiles linking for Rust compilation.
  
- **Deploy studio for each OS + Arch**
  
  We deploy 5 separate distributions of TypeDB Studio, one per platform:
  
  1. `linux-x86_64`
  2. `linux-arm64`
  3. `mac-x86_64`
  4. `mac-arm64`
  5. `windows-x86_64`
  
  Please be aware that this means TypeDB Studio distributions are no longer portable between Intel and Mac variants of the same system - eg. from an Intel mac to an ARM mac. 
  
  
- **Upgrade to 2.24.x and implement native ARM64 targets**
  
  We update the underlying typedb-driver and dependencies, and add arm64 build targets.
  
  

## Bugs Fixed
- **Ensure write transaction has been opened in integration test**
  
  We fix a spurious NPE that arises in the TextEditor integration test when a schema write is attempted before the write transaction is opened.
  
  
- **Set release compilation mode to optimized**
  
  We set the Bazel compilation mode for releases to `opt`.
  

## Code Refactors


## Other Improvements

- **Replace references to vaticle.com**

- **Fix CircleCI release workflow job references**

- **Update README.md**

    

