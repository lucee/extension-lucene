# Changelog

## 3.0.0.170

- [LDEV-6196](https://luceeserver.atlassian.net/browse/LDEV-6196) — rework context highlighting to use core's `AddionalAttrs` thread-local via reflection, removing `SearchDataPro` from the loader (requires Lucee 7.0.3.33+ or 6.2.6.11+)

## 3.0.0.169

- [LDEV-6198](https://luceeserver.atlassian.net/browse/LDEV-6198) — fix spellcheck/suggestions: real Lucene string distance scores (0-100), fix `</suggestion>` tag leak in suggestedQuery, fix resource leaks (SpellChecker/FSDirectory never closed), fix NPE with `type="explicit"`, remove dead code. `suggestions="always"` now returns max 10 suggestions per term (was 1000); `suggestions="5"` returns up to 5.

## 3.0.0.168

- [LDEV-6196](https://luceeserver.atlassian.net/browse/LDEV-6196) — fix `cfsearch` context highlighting markers not reaching extension (requires Lucee Loader 7.0.3.30+)

## 3.0.0.167

- [LDEV-6195](https://luceeserver.atlassian.net/browse/LDEV-6195) — fix `cfindex action="delete"` not removing individual documents
- [LDEV-6197](https://luceeserver.atlassian.net/browse/LDEV-6197) — make filenames searchable in path-type indexes

## 3.0.0.166

- [LDEV-6194](https://luceeserver.atlassian.net/browse/LDEV-6194) — fix NPE on `criteria="*"`, guard topN for empty results, escape special chars in Verity parser
- [LDEV-6193](https://luceeserver.atlassian.net/browse/LDEV-6193) — `type="explicit"` enables native Lucene QueryParser syntax
- [LDEV-2025](https://luceeserver.atlassian.net/browse/LDEV-2025) — fix special characters in search criteria
- [LDEV-1745](https://luceeserver.atlassian.net/browse/LDEV-1745) — fix phrase queries with `+`/`-` modifiers, fix numHits error
- Add test cases for [LDEV-1879](https://luceeserver.atlassian.net/browse/LDEV-1879), [LDEV-3310](https://luceeserver.atlassian.net/browse/LDEV-3310), [LDEV-2025](https://luceeserver.atlassian.net/browse/LDEV-2025), [LDEV-1166](https://luceeserver.atlassian.net/browse/LDEV-1166), [LDEV-1550](https://luceeserver.atlassian.net/browse/LDEV-1550), [LDEV-1023](https://luceeserver.atlassian.net/browse/LDEV-1023), [LDEV-2032](https://luceeserver.atlassian.net/browse/LDEV-2032)

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
