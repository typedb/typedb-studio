### Distribution

TypeDB Studio is hosted on the Web at https://studio.typedb.com.

Alternatively:

- Install: https://typedb.com/docs/home/install/studio
- Direct download: https://cloudsmith.io/~typedb/repos/public-release/packages/?q=name:^typedb-studio+version:3.13.0

Desktop builds of TypeDB Studio run in the following environments:

- Windows 10 or later (x86_64)
- MacOS (x86_64, arm64)
- Debian / Ubuntu 22.04 or later (x86_64, arm64)

### Documentation

- TypeDB Studio docs: https://typedb.com/docs/tools/studio
- Learn more about TypeDB: https://typedb.com/docs/home/learning-journey

### TypeDB server compatibility

TypeDB Studio 3.13.0 is compatible with TypeDB >= 3.3. For older TypeDB versions, enquire on the TypeDB Discord chat server (https://typedb.com/discord).

---

## New features

- Windows desktop builds are now code signed. Windows no longer reports TypeDB Studio as coming from an unknown publisher - the installer and application now identify TypeDB Ltd as the publisher.

## Other improvements

- Windows desktop builds are now correctly named "TypeDB Studio". Please be aware that this may cause Windows to treat Studio >= 3.13.0 as a different application to Studio <= 3.12. You can uninstall the old version separately.
- Add tooltips to all code and output overlay buttons
- https://studio.typedb.com now disallows frame embedding
