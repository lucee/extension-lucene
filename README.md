# Lucee Lucene Search Extension

Full-text search for Lucee using [Apache Lucene](https://lucene.apache.org/) — collections, indexing, and search via the standard `cfcollection`, `cfindex`, and `cfsearch` tags. No external search service required.

Version 3.0+ is Maven-based and ships with Lucee 6.2+ for keyword search. Lucee 7.0+ adds vector and hybrid search, context passage extraction for RAG, and tighter AI integration.

## Features

- **Keyword search** — traditional full-text indexing and search (default `mode="keyword"`)
- **Vector search** — semantic search via document embeddings (`mode="vector"`)
- **Hybrid search** — combined keyword + vector scoring (`mode="hybrid"`)
- **Context passages** — scored text chunks from matched documents for RAG / AI augmentation
- **Multiple index types** — files, directories, URLs (web crawl), and custom query data
- **Spellcheck / suggestions** — `suggestions` attribute on `cfsearch`
- **Explicit Lucene syntax** — `type="explicit"` for native Lucene query parser

## Requirements

| Feature | Lucee | Extension |
| --- | --- | --- |
| Keyword search | 6.2+ | 3.0+ |
| Vector / hybrid search | 7.0+ | 3.0+ |
| Context highlighting attributes | 7.0.3.30+ or 6.2.6.11+ | 3.0.0.168+ |

Context highlighting attributes (`contextHighlightBegin`, `contextHighlightEnd`, `contextPassages`, `contextPassageLength`, `contextBytes`) require a recent loader. The extension still works on older versions, but those attributes fall back to defaults.

## Installation

- **Extension ID:** `EFDEB172-F52E-4D84-9CD1A1F561B3DFC8`
- **Maven GAV:** `org.lucee:lucene-search-extension`
- **Administrator:** Services → Search (after installing from Extensions)
- **Downloads:** [download.lucee.org](https://download.lucee.org/#EFDEB172-F52E-4D84-9CD1A1F561B3DFC8)

Pin the version in production — see the [extension installation recipe](https://docs.lucee.org/recipes/extension-installation.html).

## Quick Start

```luceescript
// Create a collection
cfcollection(
	action="create",
	collection="helpdesk",
	path=expandPath( "{lucee-config-dir}/collections/helpdesk" )
);

// Index HTML files from a directory
cfindex(
	action="update",
	collection="helpdesk",
	type="path",
	key=expandPath( "/var/www/docs" ),
	urlpath="/docs",
	extensions=".html,.htm,.pdf,.txt",
	recurse="yes"
);

// Search
cfsearch(
	collection="helpdesk",
	criteria="password reset",
	name="results",
	maxrows=20
);
```

Hybrid collection for semantic / RAG use cases (Lucee 7.0+):

```luceescript
cfcollection(
	action="create",
	collection="knowledge",
	path=expandPath( "{lucee-config-dir}/collections/knowledge" ),
	mode="hybrid",
	embedding="TF-IDF",
	ratio="0.5"
);
```

## Documentation

User-facing guides live in [lucee-docs](https://github.com/lucee/lucee-docs):

- [Lucene 3 Extension](https://docs.lucee.org/recipes/lucene-3-extension.html) — collection modes, embeddings, context passages, RAG patterns
- [Adding Full Text Search](https://docs.lucee.org/recipes/search-recipe.html) — collections, indexing, search syntax, maintenance
- [AI Augmentation with Lucene](https://docs.lucee.org/recipes/ai-augmentation-lucene.html) — RAG with AI sessions

Reference: [docs.lucee.org/categories/search.html](https://docs.lucee.org/categories/search.html)

## Development

### Build

Requires JDK 11+ and Maven.

```bash
./maven-install.sh
# or
mvn clean install -Dgoal=install
```

The `.lex` extension bundle is written to `target/`.

### Test

Tests are CFML files under `tests/` with the `search` label. CI runs them against Lucee 6.2 and 7.0 via [script-runner](https://github.com/lucee/script-runner):

```bash
mvn clean install -Dgoal=install
# then run with script-runner, extensionDir=target/, testLabels=search, testAdditional=tests/
```

Key test files:

| File | Covers |
| --- | --- |
| `Index.cfc` | Basic indexing (query, file, HTML) |
| `Vector.cfc` | Vector, hybrid, TF-IDF, word2vec, context passages |
| `SearchFeatures.cfc` | Search syntax, suggestions, explicit type |
| `IndexActions.cfc` | Purge, refresh, delete, list |
| `TagCollection.cfc` | Collection create/delete/repair/optimize |

### Versioning and releases

Works like Lucee core (LDEV-6516): every code change gets its own version number. The version is not edited by hand.
`pom.xml` uses a CI-friendly `${revision}` built from `extension.version.base`, a build number and
`extension.version.qualifier` (`-SNAPSHOT`).

* **Every push to `master`** (a merged PR or a direct commit) takes the highest existing build of the base, from the git tags,
  Maven Central and the Central snapshot repository, adds 1, builds, tests, publishes that version (e.g. `3.0.0.N-SNAPSHOT`)
  to the [Central snapshot repository](https://central.sonatype.com/repository/maven-snapshots/) and creates the tag `3.0.0.N`.
* Pushes run one after another (workflow concurrency queue), so two quick merges get two different numbers.
* PR builds, forks and runs without the deploy secrets build and test `3.0.0.0-SNAPSHOT` and never use up a number.
* **Release candidate or final release:** run the workflow by hand (Actions > Java CI Combined > Run workflow), choose the branch
  `master`, tick `deploy` and pick `RC`, `BETA`, `ALPHA` or `release` as qualifier. That publishes the current code once with the
  next build number (e.g. `3.0.0.N-RC` or `3.0.0.N`) to Maven Central and creates a GitHub release with the `.lex` files,
  without a commit; the pom keeps `-SNAPSHOT`. The `lucee-versions` input only changes the Lucee versions the tests run against.
* To start a new line change `extension.version.base`.

### Project Layout

```
source/java/src/          Java implementation
  org/lucee/extension/search/lucene/
    LuceneSearchEngine.java       Entry point (SearchEngine SPI)
    LuceneSearchCollection.java   Indexing, search, vector/hybrid, passages
    net/WebCrawler.java           URL / web crawl indexing
    embedding/                    TF-IDF, word2vec, custom EmbeddingService
    highlight/                    Context passage extraction and scoring
tests/                    CFML integration tests (label: search)
pom.xml                   Maven build, extension manifest
```

## Issues

[Jira — `cfsearch` label](https://luceeserver.atlassian.net/issues/?jql=labels%20%3D%20cfsearch)

## License

LGPL 2.1 — see [License.txt](License.txt)
