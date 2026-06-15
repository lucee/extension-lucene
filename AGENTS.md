# Lucene Search Extension — Agent Guide

Maven-based Lucee extension implementing full-text search via Apache Lucene. Exposes the standard CFML search tags (`cfcollection`, `cfindex`, `cfsearch`) through Lucee's `SearchEngine` SPI.

User documentation lives in [lucee-docs](https://github.com/lucee/lucee-docs) — update recipes there, not in this repo, unless adding release notes or API behaviour that belongs in `CHANGELOG.md`.

## Folder Structure

- `source/java/src/` — Java source
- `source/java/pom.xml` — module POM (compiled into parent build)
- `pom.xml` — root Maven build, extension manifest, `.lex` packaging
- `tests/` — CFML integration tests (label: `search`)
- `build.xml` — legacy Ant wrapper around Maven (prefer `mvn` directly)
- `target/` — build output (`.lex` bundle, generated, not committed)
- `CHANGELOG.md` — release notes; update for user-visible changes

## Build

Requires JDK 11+ and Maven.

```bash
mvn clean install -Dgoal=install
```

Output: `target/*.lex`

Deploy snapshot/release (maintainers only):

```bash
mvn clean deploy -Dgoal=deploy --settings maven-settings.xml
```

## Test

CI (`.github/workflows/main.yml`) builds once, then runs tests against Lucee 6.2 and 7.0 (snapshot + stable, light) on Java 11 and 21.

Locally, after `mvn clean install -Dgoal=install`, run tests with [script-runner](https://github.com/lucee/script-runner):

- `webroot`: Lucee test bootstrap (see CI workflow)
- `extensionDir`: `{workspace}/target`
- `testLabels`: `search`
- `testAdditional`: `{workspace}/tests`

When adding behaviour, add or extend a test in `tests/` rather than only manual verification.

## Key Java Classes

| Class | Role |
| --- | --- |
| `LuceneSearchEngine` | `SearchEngine` entry point; creates collections |
| `LuceneSearchCollection` | Core logic — index, search, vector/hybrid, spellcheck, passages |
| `SearchCollectionSupport` | Shared collection/index abstractions |
| `SearchEngineSupport` | Base engine implementation |
| `WebCrawler` | `cfindex type="url"` — crawl and index linked pages |
| `EmbeddingService` / `TfIdfEmbeddingService` / `Word2VecEmbeddingService` | Vector embeddings for `mode="vector"` and `mode="hybrid"` |
| `HTMLFormatterWithScore` / `TextSplitter` / `TextCollection` | Context passage extraction for RAG |
| `QueryParser` / `Simple` / `Op` | Verity-compatible and explicit query parsing |
| `HTMLDocument` / `PDFDocument` / `FileDocument` / `CustomDocument` | Document parsers per index type |

Collection modes (set at create time):

- `keyword` — default, Lucene full-text only
- `vector` — Knn vector search with embeddings
- `hybrid` — boolean query combining keyword + vector, weighted by `ratio`

## CFML Surface

This extension does not define new tags — it implements the built-in search engine behind:

- `cfcollection` — create, list, delete, repair, optimize collections
- `cfindex` — update, delete, purge, refresh, list indexes (`type`: file, path, url, custom)
- `cfsearch` — search with optional context passages, suggestions, categories

v3 attributes on collection create: `mode`, `embedding`, `ratio`.

v3 attributes on search: `contextPassages`, `contextPassageLength`, `contextBytes`, `contextHighlightBegin`, `contextHighlightEnd`.

Loader passthrough for context attrs requires Lucee 7.0.3.30+ or 6.2.6.11+ (see `AddionalAttrsHelper`, `CHANGELOG.md`).

## Code Style

- Match existing Java style in surrounding files — tabs, minimal comments, no drive-by refactors
- Keep changes focused; this is a search extension, not a general-purpose library
- User-visible changes need a `CHANGELOG.md` entry with Jira ticket when applicable
- CFML tests use `component extends="org.lucee.cfml.test.LuceeTestCase" labels="search"`

## Documentation

End-user docs are recipes in [lucee-docs `/docs/recipes/`](https://github.com/lucee/lucee-docs/tree/master/docs/recipes):

| Recipe | ID | When to update |
| --- | --- | --- |
| Lucene 3 Extension | `lucene-3-extension` | New modes, embeddings, passage API, version requirements |
| Adding Full Text Search | `search-recipe` | Index/search syntax, categories, maintenance |
| AI Augmentation with Lucene | `ai-augmentation-lucene` | RAG patterns, AI integration |

After editing a recipe, run `node .github/scripts/generate-index-recipes.js` in lucee-docs to refresh `index.json`.

Real-world usage reference in Lucee core:

- `Lucee7/core/src/main/cfml/context/debug/modern/reference.cfm` — hybrid collection, hash-based incremental indexing, `augmentSearchCriteria()` for AI
- `Lucee7/core/src/main/cfml/context/admin/services.search.cfm` — Administrator search UI

## Extension Metadata

- **Extension ID:** `EFDEB172-F52E-4D84-9CD1A1F561B3DFC8`
- **Maven GAV:** `org.lucee:lucene-search-extension`
- **Bundle:** `lucene.search.extension`
- **Engine class:** `org.lucee.extension.search.lucene.LuceneSearchEngine`
- **GitHub:** [lucee/extension-lucene](https://github.com/lucee/extension-lucene)
