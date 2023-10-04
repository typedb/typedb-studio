### Distribution

TypeDB Studio is available for Linux, Mac and Windows (download binaries below).

For Mac, TypeDB Studio is also available through Homebrew:

```
brew tap vaticle/tap
brew install --cask vaticle/tap/typedb-studio
```

### TypeDB Server Compatible Versions

See the [compatibility table](https://typedb.com/docs/clients/2.x/studio#_version_compatibility) at our Studio
documentation for which versions of Studio are compatible with which versions of TypeDB.

---


## New Features
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
- **Set release compilation mode to optimized**
  
  We set the Bazel compilation mode for releases to `opt`.
  

## Code Refactors


## Other Improvements
- **Fix CircleCI release workflow job references**

- **Allow releasing from development branch**

- **Update README.md**

    

