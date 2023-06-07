### Distribution

TypeDB Studio is available for Linux, Mac and Windows (download binaries below).

For Mac, TypeDB Studio is also available through Homebrew:

```
brew tap vaticle/tap
brew install --cask vaticle/tap/typedb-studio
```

### TypeDB Server Compatible Versions

See the [compatibility table](https://docs.vaticle.com/docs/studio/overview#version-compatibility) at our Studio
documentation for which versions of Studio are compatible with which versions of TypeDB.

---


## New Features
- **Refactor schema vertex rendering for graph visualisation**
  
  Previously, Studio would visualise 'owns' edges that are inherited multiple times, even if the type from which it inherited this ownership is also present on the graph with its own 'owns' edge to the same attribute type.
  
  Now, Studio only visualises owns, plays and sub edges for the 'super-est' edges that own, plays or are subtypes for those types that are visible on the graph.
  
  
- **Bump TypeDB Client Java to 2.18**
  
  We've bumped the java client that Studio uses to 2.18.
  
  

## Bugs Fixed


## Code Refactors
- **Use remote concept API to derive has edges for graph visualisation**
  
  We've refactored the graph building process to derive 'has' edges from the remote concept API. This is hugely faster than the previous approach of running explicit reasoning queries per concept.
  
  
- **Terminology update for Type Editor and Type Browser**
  
  We've renamed two parts of our system that pertain to the displaying of information about types. 
  
  

## Other Improvements
- **Update release notes workflow**
  
  We integrate the new release notes tooling. The release notes are now to be written by a person and committed to the repo.
  
  
- **Update CODEOWNERS**

    

