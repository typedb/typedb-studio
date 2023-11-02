### Distribution

TypeDB Studio is available for Linux, Mac and Windows (download binaries below).

For Mac Intel and Mac ARM, TypeDB Studio is also available through Homebrew:

```
brew tap vaticle/tap
brew install --cask vaticle/tap/typedb-studio
```

### TypeDB Server Compatibility

See the [compatibility table](https://typedb.com/docs/clients/studio#_version_compatibility) to see
which versions of Studio are compatible with which versions of TypeDB server.

---


## New Features
- **Implement Fetch query**
  
  We update to the newest version of TypeDB Driver which supports Fetch queries. As Fetch queries return a stream of JSONs, they are only available in text output view, not as a graph.
  
  For more details, see https://github.com/vaticle/typeql/pull/300
  
  
- **Support value variables in log output**
  
  We now handle the possibility of Values being returned as part of a ConceptMap. Retrieved pure values are not displayed in the graph view, but printed out as expected in the log view.
  
  

## Bugs Fixed


## Code Refactors


## Other Improvements
- **Update README file**
  
  Update the README file.
  
  
- **Update CODEOWNERS**

    

