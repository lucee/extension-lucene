# Changelog

## 3.0.0.165

- [LDEV-6182](https://luceeserver.atlassian.net/browse/LDEV-6182) — fix Lucee 6.2 compatibility
- [LDEV-6183](https://luceeserver.atlassian.net/browse/LDEV-6183) — allow file path as embedding attribute for custom vectors
- CI: bump actions to v5, fix adopt→temurin

## 3.0.0.164

- [LDEV-6093](https://luceeserver.atlassian.net/browse/LDEV-6093) — auto-bundle parent POMs to make extension fully self-contained
- CI: build once, test against Lucee 6.2 + 7.0, gate deploy to master

## 3.0.0.163

- Switch from XML to JSON for search config storage
- Remove dependency on `org.lucee.xml` bundle
- Add vector indexing and embedding support (TF-IDF, Word2Vec)
- Add context passage extraction
- Update Lucene libraries (3.6.1 through 9.12.1)
- Switch to Maven build
- Update Sonatype endpoints

## 2.4.2.x

- Update PDFBox bundled library
- Java 17 compatibility

## 2.4.1.x

- [LDEV-1023](https://luceeserver.atlassian.net/browse/LDEV-1023) — search engine fix
- Remove the requirement of the `org.lucee.xml` bundle
- Use Lucee XML library to handle xml
- Update HTMLParser
- Fix wrong quote
- Remove old `.tld` in favour of `.tldx`
- Make sure ThreadDeath is never caught and kept
- Move to `org.lucee` xerces version
- Update PDFBox jar and build properties
- Add GitHub Actions CI
- Switch to Lucee script runner for tests
- Run tests with Lucee light

## 1.0.0.x

- Initial commit
