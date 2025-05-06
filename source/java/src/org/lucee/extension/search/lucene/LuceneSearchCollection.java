package org.lucee.extension.search.lucene;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.KnnVectorField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.KnnFloatVectorQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.spell.Dictionary;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.FSDirectory;
import org.lucee.extension.search.IndexResultImpl;
import org.lucee.extension.search.SearchCollectionSupport;
import org.lucee.extension.search.SearchDataImpl;
import org.lucee.extension.search.SearchEngineSupport;
import org.lucee.extension.search.SearchResulItemImpl;
import org.lucee.extension.search.SuggestionItemImpl;
import org.lucee.extension.search.lucene.docs.CustomDocument;
import org.lucee.extension.search.lucene.docs.FieldUtil;
import org.lucee.extension.search.lucene.embedding.EmbeddingService;
import org.lucee.extension.search.lucene.embedding.TfIdfEmbeddingService;
import org.lucee.extension.search.lucene.embedding.Word2VecEmbeddingService;
import org.lucee.extension.search.lucene.highlight.HTMLFormatterWithScore;
import org.lucee.extension.search.lucene.highlight.Text;
import org.lucee.extension.search.lucene.highlight.TextCollection;
import org.lucee.extension.search.lucene.highlight.TextHandler;
import org.lucee.extension.search.lucene.highlight.TextSplitter;
import org.lucee.extension.search.lucene.net.WebCrawler;
import org.lucee.extension.search.lucene.query.Literal;
import org.lucee.extension.search.lucene.query.Op;
import org.lucee.extension.search.lucene.util.CommonUtil;
import org.lucee.extension.search.lucene.util.SerializableObject;

import lucee.commons.io.log.Log;
import lucee.commons.io.res.Resource;
import lucee.commons.io.res.filter.ResourceFilter;
import lucee.commons.io.res.filter.ResourceNameFilter;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.config.Config;
import lucee.runtime.exp.PageException;
import lucee.runtime.search.IndexResult;
import lucee.runtime.search.SearchData;
import lucee.runtime.search.SearchEngine;
import lucee.runtime.search.SearchException;
import lucee.runtime.search.SearchIndex;
import lucee.runtime.search.SearchResulItem;
import lucee.runtime.search.SuggestionItem;
import lucee.runtime.type.QueryColumn;
import lucee.runtime.type.Struct;
import lucee.runtime.type.dt.DateTime;
import lucee.runtime.util.Cast;

/**
 * 
 */
public final class LuceneSearchCollection extends SearchCollectionSupport {

	public class DirectoryResourceFilter implements ResourceFilter {
		@Override
		public boolean accept(Resource pathname) {
			return pathname.isDirectory();
		}

	}

	private static int MODE_KEYWORD = 1;
	private static int MODE_VECTOR = 2;
	private static int MODE_HYBRID = 4;

	private static final long serialVersionUID = 3430238280421965781L;

	private static final String EMBEDDING_TF_IDF = "TF-IDF";
	private static final String EMBEDDING_WORD2VEC = "word2vec";

	private Resource collectionDir;
	private boolean spellcheck;

	private String embedding;
	private final int mode;
	private final double ratio;
	private EmbeddingService embeddingService;

	private Log log;

	private final CFMLEngine engine;
	private Map<String, EmbeddingService> embeddingServices = new ConcurrentHashMap<>();

	private static final SerializableObject token = new SerializableObject();

	public LuceneSearchCollection(SearchEngineSupport searchEngine, String name, Resource path, String language,
			DateTime lastUpdate, DateTime created) throws SearchException {
		this(searchEngine, name, path, language, lastUpdate, created, true, null, null, 0.5D);
	}

	public LuceneSearchCollection(SearchEngineSupport searchEngine, String name, Resource path, String language,
			DateTime lastUpdate, DateTime created, boolean spellcheck) throws SearchException {
		this(searchEngine, name, path, language, lastUpdate, created, spellcheck, null, null, 0.5D);
	}

	public LuceneSearchCollection(SearchEngineSupport searchEngine, String name, Resource path, String language,
			DateTime lastUpdate, DateTime created, boolean spellcheck, String mode, String embedding, double ratio)
			throws SearchException {
		super(searchEngine, name, path, language, lastUpdate, created);
		engine = CFMLEngineFactory.getInstance();

		// mode
		if (Util.isEmpty(mode, true))
			this.mode = MODE_KEYWORD;
		else {
			mode = mode.trim();
			if ("keyword".equals(mode))
				this.mode = MODE_KEYWORD;
			else if ("vector".equals(mode))
				this.mode = MODE_VECTOR;
			else if ("hybrid".equals(mode))
				this.mode = MODE_HYBRID;
			else
				throw new SearchException("invalid mode [" + mode + "], valid values are [keyword, vector, hybrid]");
		}

		if (ratio > 1 || ratio < 0) {
			throw new SearchException(
					"Invalid ratio value: " + ratio + ". The ratio must be between 0.0 and 1.0 inclusive.");
		}
		this.ratio = ratio;

		// embedding
		if (this.mode != MODE_KEYWORD) {
			if (!Util.isEmpty(embedding, true)) {
				this.embedding = embedding.trim();
			} else {
				this.embedding = EMBEDDING_TF_IDF;
			}
		}

		this.spellcheck = spellcheck;
		collectionDir = getPath().getRealResource(toIdentityVariableName(getName()));

		log = searchEngine.getConfig().getLog("search");
	}

	@Override
	protected void _create() throws SearchException {
		try {
			if (!collectionDir.exists())
				collectionDir.createDirectory(true);
		} catch (IOException e) {
			throw CommonUtil.toSearchException(e);
		}
	}

	@Override
	protected void _optimize() throws SearchException {
		IndexWriter[] writers = _getWriters(false);
		for (int i = 0; i < writers.length; i++) {
			try {
				optimizeEL(writers[i]);
			} finally {
				close(writers[i]);
			}
		}
	}

	@Override
	protected void _map(Resource path) throws SearchException {
		throw new SearchException("mapping of existing Collection for file [" + path + "] not supported");
	}

	@Override
	protected void _repair() throws SearchException {
		// throw new SearchException("repair of existing Collection not supported");
	}

	@Override
	protected IndexResult _indexFile(String id, String title, Resource res, String language) throws SearchException {
		info(res.getAbsolutePath());
		_checkLanguage(language);
		int before = getDocumentCount(id);
		IndexWriter writer = null;
		synchronized (token) {
			try {
				writer = _getWriter(id, true);
				_index(id, writer, res, res.getName());
				writer.forceMerge(1); // replaced optimize() with forceMerge(1)
			} catch (SearchException e) {
				throw e;
			} catch (Exception e) {
				throw CommonUtil.toSearchException(e);
			} finally {
				close(writer);
			}
			indexSpellCheck(id);
		}
		if (getDocumentCount(id) == before)
			return new IndexResultImpl(0, 0, 1);
		return new IndexResultImpl(0, 1, 0);
	}

	@Override
	protected IndexResult _indexPath(String id, String title, Resource dir, String[] extensions, boolean recurse,
			String language) throws SearchException {
		info(dir.getAbsolutePath());
		_checkLanguage(language);
		int doccount = 0;
		IndexWriter writer = null;
		synchronized (token) {
			try {
				writer = _getWriter(id, true);
				doccount = _list(id, 0, writer, dir, new LuceneExtensionFileFilter(extensions, recurse), "");
				// optimizeEL(writer);
				optimize(writer);
			} catch (SearchException e) {
				throw e;
			} catch (Exception e) {
				throw CommonUtil.toSearchException(e);
			} finally {
				close(writer);
			}
			indexSpellCheck(id);
		}

		return new IndexResultImpl(0, 0, doccount);
	}

	private void optimize(IndexWriter writer) throws IOException {
		writer.forceMerge(1);
	}

	private void optimizeEL(IndexWriter writer) {
		if (writer == null)
			return;
		try {
			optimize(writer);
		} catch (Throwable t) {
			if (t instanceof ThreadDeath)
				throw (ThreadDeath) t;
		}
	}

	private void indexSpellCheckOld(String id) throws SearchException {
		if (!spellcheck)
			return;

		IndexReader reader = null;
		FSDirectory spellDir = null;

		Resource dir = _createSpellDirectory(id);
		try {
			Path spellPath = engine.getCastUtil().toFile(dir).toPath();
			spellDir = FSDirectory.open(spellPath);
			reader = _getReader(id, false);
			Dictionary dictionary = new LuceneDictionary(reader, "contents");

			SpellChecker spellChecker = new SpellChecker(spellDir);
			spellChecker.indexDictionary(dictionary, _getConfig(), true);

		} catch (Exception e) {
			throw CommonUtil.toSearchException(e);
		} finally {
			closeEL(reader);
		}
	}

	private void indexSpellCheck(String id) throws SearchException {
		if (!spellcheck)
			return;

		IndexReader reader = null;
		FSDirectory spellDir = null;

		Resource dir = _createSpellDirectory(id);
		try {
			spellDir = FSDirectory.open(engine.getCastUtil().toFile(dir).toPath());
			SpellChecker spellChecker = new SpellChecker(spellDir);

			reader = _getReader(id, false);
			Dictionary dictionary = new LuceneDictionary(reader, "contents");

			spellChecker.indexDictionary(dictionary, _getConfig(), true);

		} catch (Exception e) {
			throw CommonUtil.toSearchException(e);
		} finally {
			closeEL(reader);
		}

	}

	private void close(IndexWriter writer) throws SearchException {
		if (writer != null) {
			// print.out("w-close");
			try {
				writer.close();
			} catch (IOException e) {
				throw CommonUtil.toSearchException(e);
			}
		}
	}

	private static void close(IndexReader reader) throws SearchException {
		if (reader != null) {
			try {
				reader.close();
			} catch (IOException e) {
				throw CommonUtil.toSearchException(e);
			}
		}
	}

	private static void closeEL(IndexReader reader) {
		// print.out("r-closeEL");
		if (reader != null) {
			try {
				reader.close();
			} catch (Throwable t) {
				if (t instanceof ThreadDeath)
					throw (ThreadDeath) t;
			}
		}
	}

	@Override
	protected IndexResult _indexURL(String id, String title, URL url, String[] extensions, boolean recurse,
			String language) throws SearchException {
		// timeout=ThreadLocalPageContext.getConfig().getRequestTimeout().getMillis();
		return _indexURL(id, title, url, extensions, recurse, language, 50000L);
	}

	@Override
	public IndexResult _indexURL(String id, String title, URL url, String[] extensions, boolean recurse,
			String language, long timeout) throws SearchException {
		_checkLanguage(language);
		info(url.toExternalForm());
		int before = getDocumentCount(id);
		IndexWriter writer = null;
		synchronized (token) {
			try {
				writer = _getWriter(id, true);
				new WebCrawler(log).parse(writer, url, extensions, recurse, timeout);

				optimize(writer);
			} catch (SearchException e) {
				throw e;
			} catch (Exception e) {
				throw CommonUtil.toSearchException(e);
			} finally {
				close(writer);
			}
			indexSpellCheck(id);
		}
		if (getDocumentCount(id) == before)
			return new IndexResultImpl(0, 0, 1);
		return new IndexResultImpl(0, 1, 0);
		// throw new SearchException("url indexing not supported");

	}

	/**
	 * @param id
	 * @param title
	 * @param keyColumn
	 * @param bodyColumns
	 * @param language
	 * @param custom1
	 * @param custom2
	 * @param custom3
	 * @param custom4
	 * @return
	 * @throws SearchException
	 */
	@Override
	protected IndexResult _deleteCustom(String id, QueryColumn keyColumn) throws SearchException {

		int countBefore = 0;
		int countAfter = 0;

		Map<String, Document> docs = new HashMap<String, Document>();

		Set<String> keys = toSet(keyColumn);
		IndexWriter writer = null;
		String key;
		IndexReader reader = null;
		Document doc;

		synchronized (token) {
			try {
				try {
					reader = _getReader(id, false);
					countBefore = reader.maxDoc();
					for (int i = 0; i < countBefore; i++) {
						doc = reader.document(i);
						key = doc.getField("key").stringValue();
						if (!keys.contains(key))
							docs.put(key, doc);
					}
				} catch (Exception e) {
				} finally {
					close(reader);
				}
				countAfter = docs.size();

				writer = _getWriter(id, true);
				Iterator<Entry<String, Document>> it = docs.entrySet().iterator();
				while (it.hasNext()) {
					writer.addDocument(embed(it.next().getValue(), id));
				}
				optimizeEL(writer);

			} catch (SearchException e) {
				throw e;
			} catch (Exception e) {
				throw CommonUtil.toSearchException(e);
			} finally {
				close(writer);
			}
			indexSpellCheck(id);
		}
		int removes = countBefore - countAfter;

		return new IndexResultImpl(removes, 0, 0);
	}

	private Set<String> toSet(QueryColumn column) {
		Set<String> set = new HashSet<String>();
		Iterator it = column.valueIterator();
		while (it.hasNext()) {
			set.add(engine.getCastUtil().toString(it.next(), null));
		}
		return set;
	}

	/**
	 * @param id
	 * @param title
	 * @param keyColumn
	 * @param bodyColumns
	 * @param language
	 * @param custom1
	 * @param custom2
	 * @param custom3
	 * @param custom4
	 * @return
	 * @throws SearchException
	 */
	@Override
	protected IndexResult _indexCustom(String id, Object title, QueryColumn keyColumn, QueryColumn[] bodyColumns,
			String language, Object urlpath, Object custom1, Object custom2, Object custom3, Object custom4)
			throws SearchException {
		_checkLanguage(language);
		String t;
		String url;
		String c1;
		String c2;
		String c3;
		String c4;

		int countExisting = 0;
		int countAdd = keyColumn.size();
		int countNew = 0;

		Map<String, Document> docs = new HashMap<String, Document>();
		IndexWriter writer = null;
		synchronized (token) {
			try {
				// read existing reader
				IndexReader reader = null;
				try {
					reader = _getReader(id, false);
					int len = reader.maxDoc();
					Document doc;
					for (int i = 0; i < len; i++) {
						doc = reader.document(i);
						docs.put(doc.getField("key").stringValue(), doc);
					}
				} catch (Exception e) {
				} finally {
					close(reader);
				}

				countExisting = docs.size();
				writer = _getWriter(id, true);
				int len = keyColumn.size();
				String key;
				for (int i = 1; i <= len; i++) {
					key = engine.getCastUtil().toString(keyColumn.get(i, null), null);
					if (key == null)
						continue;

					StringBuilder body = new StringBuilder();
					for (int y = 0; y < bodyColumns.length; y++) {
						Object tmp = bodyColumns[y].get(i, null);
						if (tmp != null) {
							body.append(tmp.toString());
							body.append(' ');
						}
					}
					// t=(title==null)?null:Caster.toString(title.get(i,null),null);
					// url=(urlpath==null)?null:Caster.toString(urlpath.get(i,null),null);

					t = getRow(title, i);
					url = getRow(urlpath, i);
					c1 = getRow(custom1, i);
					c2 = getRow(custom2, i);
					c3 = getRow(custom3, i);
					c4 = getRow(custom4, i);

					docs.put(key, CustomDocument.getDocument(t, key, body.toString(), url, c1, c2, c3, c4));
				}
				countNew = docs.size();
				Iterator<Entry<String, Document>> it = docs.entrySet().iterator();
				Entry<String, Document> entry;
				Document doc;
				while (it.hasNext()) {
					entry = it.next();
					doc = entry.getValue();
					writer.addDocument(embed(doc, id));
				}
				optimizeEL(writer);
				// writer.optimize();

			} catch (Exception ioe) {
				throw CommonUtil.toSearchException(ioe);
			} finally {
				close(writer);
			}
			indexSpellCheck(id);
		}
		int inserts = countNew - countExisting;

		return new IndexResultImpl(0, inserts, countAdd - inserts);
	}

	private String getRow(Object column, int row) {
		if (column instanceof QueryColumn) {
			return engine.getCastUtil().toString(((QueryColumn) column).get(row, null), null);
		}
		if (column != null)
			return engine.getCastUtil().toString(column, null);
		return null;
	}

	@Override
	protected IndexResult _purge() throws SearchException {
		SearchIndex[] indexes = getIndexes();
		int count = 0;
		for (int i = 0; i < indexes.length; i++) {
			count += getDocumentCount(indexes[i].getId());
		}
		engine.getResourceUtil().removeChildrenSilent(collectionDir);
		return new IndexResultImpl(count, 0, 0);
	}

	@Override
	protected IndexResult _delete() throws SearchException {
		SearchIndex[] indexes = getIndexes();
		int count = 0;
		for (int i = 0; i < indexes.length; i++) {
			count += getDocumentCount(indexes[i].getId());
		}
		engine.getResourceUtil().removeSilent(collectionDir, true);
		return new IndexResultImpl(count, 0, 0);
	}

	@Override
	protected IndexResult _deleteIndex(String id) throws SearchException {
		int count = getDocumentCount(id);
		engine.getResourceUtil().removeSilent(_getIndexDirectory(id, true), true);
		return new IndexResultImpl(count, 0, 0);
	}

	@Override
	public SearchResulItem[] _search(SearchData data, String criteria, String language, short type, String categoryTree,
			String[] category) throws SearchException {
		return _search(data, criteria, language, type, 1, -1, categoryTree, category);
	}

	// Helper method to determine which index a document belongs to
	private String determineDocumentIndex(Document doc, List<String> indexIds) {
		// You'll need to store index ID in your documents at index time
		// Or use another identifier to determine which index a document came from
		String storedIndexId = doc.get("indexId");
		if (storedIndexId != null && indexIds.contains(storedIndexId)) {
			return storedIndexId;
		}
		// Fallback to first index if can't determine
		return indexIds.get(0);
	}

	@Override
	public SearchResulItem[] _search(SearchData data, String criteria, String language, short type, final int startrow,
			final int maxrow, String categoryTree, String[] category) throws SearchException {
		try {
			if (type != SEARCH_TYPE_SIMPLE)
				throw new SearchException("search type explicit not supported");

			Analyzer analyzer = SearchUtil.getAnalyzer(language);
			Query query = null;
			Op op = null;

			org.lucee.extension.search.lucene.query.QueryParser queryParser = new org.lucee.extension.search.lucene.query.QueryParser();

			// addional attributes
			int contextBytes = 1000;
			int contextPassages = 3;
			int contextPassageLength = 150;
			String contextHighlightBegin = HTMLFormatterWithScore.DEFAULT_PRE_TAG;
			String contextHighlightEnd = HTMLFormatterWithScore.DEFAULT_POST_TAG;

			String tmp;

			if (data instanceof SearchDataImpl) {
				Cast cast = engine.getCastUtil();
				Map<String, Object> attrs = ((SearchDataImpl) data).getAddionalAttributes();
				if (attrs != null) {
					contextBytes = cast.toIntValue(attrs.get("contextBytes"), contextBytes);
					contextPassages = cast.toIntValue(attrs.get("contextPassages"), contextPassages);
					contextPassageLength = cast.toIntValue(attrs.get("contextPassageLength"), contextPassageLength);

					tmp = cast.toString(attrs.get("contextHighlightBegin"), null);
					if (!Util.isEmpty(tmp, true))
						contextHighlightBegin = tmp;
					tmp = cast.toString(attrs.get("contextHighlightEnd"), null);
					if (!Util.isEmpty(tmp, true))
						contextHighlightEnd = tmp;

				}
			}

			HTMLFormatterWithScore formatter = null;
			if (!criteria.equals("*")) {
				op = queryParser.parseOp(criteria);
				if (op == null)
					criteria = "*";
				else
					criteria = op.toString();

				// Add vector search if enabled and service is available
				if (getEmbeddingService() != null && !criteria.equals("*")) {
					try {
						// Generate embedding for query
						float[] queryVector = getEmbeddingService().generate(criteria);
						// Hybrid search parameters
						float keywordWeight = (float) (1.0 - ratio);
						float vectorWeight = (float) ratio;
						// Create vector query
						KnnFloatVectorQuery vectorQuery = new KnnFloatVectorQuery("embedding", queryVector, 100);
						BoostQuery boostedVectorQuery = new BoostQuery(vectorQuery, vectorWeight);

						if (mode == MODE_HYBRID) {

							Query keywordQuery = new QueryParser("contents", analyzer).parse(criteria);
							// Combine queries for hybrid search
							BooleanQuery.Builder bqBuilder = new BooleanQuery.Builder();
							BoostQuery boostedKeywordQuery = new BoostQuery(keywordQuery, keywordWeight);
							bqBuilder.add(boostedKeywordQuery, BooleanClause.Occur.SHOULD);
							bqBuilder.add(boostedVectorQuery, BooleanClause.Occur.SHOULD);
							query = bqBuilder.build();
							log.log(Log.LEVEL_INFO, "Collection:" + getName(),
									"Using hybrid search with weights - Keyword: " + keywordWeight + ", Vector: "
											+ vectorWeight);

						} else {
							query = vectorQuery;
							log.log(Log.LEVEL_INFO, "Collection:" + getName(), "Using vector search");
						}

					} catch (Exception e) {
						// Log error but continue with keyword search
						log.log(Log.LEVEL_ERROR, "Collection:" + getName(),
								"Error in vector search: " + e.getMessage() + " - Falling back to keyword search", e);
					}
				}

				else {
					query = new QueryParser("contents", analyzer).parse(criteria);
				}

				formatter = new HTMLFormatterWithScore(contextHighlightBegin, contextHighlightEnd);
			}
			Resource[] files = _getIndexDirectories();

			if (files == null)
				return new SearchResulItem[0];
			ArrayList<SearchResulItem> list = new ArrayList<SearchResulItem>();
			ArrayList<String> spellCheckIndex = spellcheck ? new ArrayList<String>() : null;
			// Create a list to hold all valid readers
			List<IndexReader> readers = new ArrayList<>();
			List<String> indexIds = new ArrayList<>();
			try {
				// create the readers
				for (int i = 0; i < files.length; i++) {
					if (removeCorrupt(files[i]))
						continue;

					String strFile = files[i].toString();
					SearchIndex si = indexes.get(files[i].getName());
					if (si == null)
						continue;

					// Check category filters
					String ct = si.getCategoryTree();
					String c = engine.getListUtil().toList(si.getCategories(), ",");

					if (!matchCategoryTree(ct, categoryTree))
						continue;
					if (!matchCategories(si.getCategories(), category))
						continue;

					String id = files[i].getName();
					data.addRecordsSearched(_countDocs(strFile));

					// Add this reader and ID to our lists
					IndexReader reader = _getReader(id, false);
					readers.add(reader);
					indexIds.add(id);

					if (spellcheck)
						spellCheckIndex.add(id);
				}

				// If we have any valid indexes, search them all at once with MultiReader
				if (!readers.isEmpty()) {
					// Combine all readers into one MultiReader
					IndexReader reader;
					if (readers.size() == 1) {
						reader = readers.get(0);
					} else {
						reader = new MultiReader(readers.toArray(new IndexReader[0]), false);
					}

					IndexSearcher searcher = new IndexSearcher(reader);

					// Perform the search once across all indexes
					if (query == null && "*".equals(criteria)) {
						// Handle wildcard search
						int len = reader.numDocs();
						for (int y = 0; y < len && (maxrow == -1 || y < maxrow); y++) {
							Document doc = reader.document(y);
							// Need to determine which index this doc came from
							String indexId = determineDocumentIndex(doc, indexIds);

							SearchIndex si = indexes.get(indexId);
							String ct = si != null ? si.getCategoryTree() : null;
							String c = si != null ? engine.getListUtil().toList(si.getCategories(), ",") : null;

							list.add(createSearchResulItem(query, formatter, doc, indexId, 1, ct, c, contextPassages,
									contextPassageLength, contextBytes));
						}
					} else {
						// Normal search
						TopDocs topDocs = searcher.search(query,
								maxrow > -1 ? Math.min(startrow + maxrow, reader.numDocs()) : reader.numDocs());
						ScoreDoc[] scoreDocs = topDocs.scoreDocs;

						if (mode == MODE_VECTOR) {
							// For vector-only searches, use a different approach without QueryScorer
							for (int y = 0; y < scoreDocs.length && (maxrow == -1 || y < maxrow); y++) {
								Document doc = searcher.doc(scoreDocs[y].doc);

								// Determine which index this doc came from
								String indexId = determineDocumentIndex(doc, indexIds);

								SearchIndex si = indexes.get(indexId);
								String ct = si != null ? si.getCategoryTree() : null;
								String c = si != null ? engine.getListUtil().toList(si.getCategories(), ",") : null;

								// Create a special version that doesn't use the formatter/highlighter
								list.add(createVectorSearchResultItem(doc, criteria, indexId, scoreDocs[y].score, ct, c,
										contextPassages, contextPassageLength, contextBytes));
							}
						}

						else {
							// For keyword and hybrid/keyword searches, use the existing approach with
							// highlighting
							for (int y = 0; y < scoreDocs.length && (maxrow == -1 || y < maxrow); y++) {
								Document doc = searcher.doc(scoreDocs[y].doc);

								// Determine which index this doc came from
								String indexId = determineDocumentIndex(doc, indexIds);

								SearchIndex si = indexes.get(indexId);
								String ct = si != null ? si.getCategoryTree() : null;
								String c = si != null ? engine.getListUtil().toList(si.getCategories(), ",") : null;

								list.add(createSearchResulItem(query, formatter, doc, indexId, scoreDocs[y].score, ct,
										c, contextPassages, contextPassageLength, contextBytes));
							}
						}
					}
				}

			} finally {
				// Close all readers
				for (IndexReader reader : readers) {
					close(reader);
				}
			}

			// spellcheck
			if (spellcheck && data != null && data.getSuggestionMax() >= list.size()) {
				Map<String, SuggestionItem> suggestions = data.getSuggestion();
				Iterator<String> it = spellCheckIndex.iterator();
				String id;
				Literal[] literals = queryParser.getLiteralSearchedTerms();
				String[] strLiterals = queryParser.getStringSearchedTerms();
				boolean setSuggestionQuery = false;

				while (it.hasNext()) {
					id = it.next();
					SuggestionItemImpl si;
					SpellChecker sc = getSpellChecker(id);

					for (int i = 0; i < strLiterals.length; i++) {
						String[] arr = sc.suggestSimilar(strLiterals[i], 1000);
						if (arr.length > 0) {
							literals[i].set("<suggestion>" + arr[0] + "</suggestion>");
							setSuggestionQuery = true;
							si = (SuggestionItemImpl) suggestions.get(strLiterals[i]);
							if (si == null)
								suggestions.put(strLiterals[i], new SuggestionItemImpl(arr));
							else
								si.add(arr);
						}
					}
				}
				if (setSuggestionQuery) {
					data.setSuggestionQuery(op.toString());
				}
			}
			// list.sort((item1, item2) -> Float.compare(item2.getScore(),
			// item1.getScore()));

			// the real removal of maxrow only can happen after we have collected results
			// from all indexes, because we want only the result with the best scores

			// remove start rows
			if (startrow > 1) {
				int start = startrow;
				while (start > 1) {
					list.remove(0);
					start--;
				}
				// list.remove(start)
			}

			// remove max rows
			int size = list.size();
			while (maxrow >= 0 && maxrow < size) {
				list.remove(--size);
			}

			return list.toArray(new SearchResulItem[list.size()]);

		} catch (Exception e) {
			throw CommonUtil.toSearchException(e);
		}
	}

	private SpellChecker getSpellChecker(String id) throws IOException, PageException {
		FSDirectory siDir = FSDirectory.open(engine.getCastUtil().toFile(_getSpellDirectory(id)).toPath());
		SpellChecker spellChecker = new SpellChecker(siDir);
		return spellChecker;
	}

	private boolean removeCorrupt(Resource dir) {
		if (engine.getResourceUtil().isEmptyFile(dir)) {
			engine.getResourceUtil().removeSilent(dir, true);
			return true;
		}
		return false;
	}

	private static SearchResulItem createSearchResulItem(Query query, Formatter formatter, Document doc, String name,
			float score, String ct, String c, int contextPassages, int contextPassageLength, int contextBytes)
			throws IOException, InvalidTokenOffsetsException {

		String summary = doc.get("summary");
		String contents = doc.get("contents");

		Object contextSummary;
		if (contextBytes > 0 && contextPassages > 0) {
			// Extract keyword query from hybrid query if necessary
			Query queryForHighlighting = extractKeywordQueryFromHybrid(query);

			contextSummary = new TextCollection(contents, TextHandler.findBestTexts(queryForHighlighting, formatter,
					contents, contextPassages, contextPassageLength), "...\n", contextBytes);

		} else {
			contextSummary = "";
		}
		return new SearchResulItemImpl(name, doc.get("title"), score, doc.get("key"), doc.get("url"), summary,
				contextSummary, contents, ct, c, doc.get("custom1"), doc.get("custom2"), doc.get("custom3"),
				doc.get("custom4"), doc.get("mime-type"), doc.get("author"), doc.get("size"));
	}

	private SearchResulItem createVectorSearchResultItem(Document doc, String criteria, String name, final float score,
			String ct, String c, int contextPassages, int contextPassageLength, int contextBytes) {

		String summary = doc.get("summary");
		String contents = doc.get("contents");

		Object contextSummary;
		if (contextBytes > 0 && contextPassages > 0 && contents != null) {
			// Extract keywords from the search criteria (assuming criteria is available
			// here)
			// Remove common words like "the", "and", "of", etc.
			String[] keywords = criteria.toLowerCase().split("\\s+");
			Set<String> stopWords = new HashSet<>(Arrays.asList("the", "a", "an", "and", "or", "but", "is", "in", "on",
					"at", "to", "for", "with", "by", "about", "as", "of", "this", "that"));

			List<String> significantKeywords = new ArrayList<>();
			for (String keyword : keywords) {
				if (keyword.length() > 2 && !stopWords.contains(keyword)) {
					significantKeywords.add(keyword);
				}
			}

			// Create simple text passages and score them based on keyword occurrences
			TextSplitter splitter = new TextSplitter(contextPassageLength);
			List<Text> texts = splitter.split(contents);

			// Create a list to store scored paragraphs
			List<TextHandler.ScoredParagraph> scoredParagraphs = new ArrayList<>();

			// Score and highlight each paragraph
			for (int i = 0; i < texts.size(); i++) {
				Text text = texts.get(i);
				String paragraph = text.text.toLowerCase();

				// Calculate score based on keyword occurrences
				float paragraphScore = 0.0f;
				for (String keyword : significantKeywords) {
					int count = countOccurrences(paragraph, keyword);
					paragraphScore += count;
				}

				// Only include paragraphs with matches or if we don't have enough yet
				if (paragraphScore > 0 || scoredParagraphs.size() < contextPassages) {
					// Create a simple highlighted version
					String highlighted = text.text;
					for (String keyword : significantKeywords) {
						highlighted = highlightKeyword(highlighted, keyword);
					}

					scoredParagraphs.add(new TextHandler.ScoredParagraph(text, // original Text object
							highlighted, // highlighted version
							paragraphScore, // calculated score
							i // original index in the list
					));
				}
			}

			// Sort by score (higher scores first)
			scoredParagraphs.sort(Comparator.comparing(p -> -p.score));

			// Take top contextPassages
			if (scoredParagraphs.size() > contextPassages) {
				scoredParagraphs = scoredParagraphs.subList(0, contextPassages);
			}

			// Re-sort by original index to maintain document order
			scoredParagraphs.sort(Comparator.comparing(p -> p.originalIndex));

			// Create a TextCollection with the scored paragraphs
			contextSummary = new TextCollection(contents, scoredParagraphs, "...\n", contextBytes);
		} else {
			contextSummary = "";
		}

		return new SearchResulItemImpl(name, doc.get("title"), score, doc.get("key"), doc.get("url"), summary,
				contextSummary, contents, ct, c, doc.get("custom1"), doc.get("custom2"), doc.get("custom3"),
				doc.get("custom4"), doc.get("mime-type"), doc.get("author"), doc.get("size"));
	}

	// Helper method to count keyword occurrences
	private int countOccurrences(String text, String keyword) {
		int count = 0;
		int index = 0;
		while ((index = text.indexOf(keyword, index)) != -1) {
			count++;
			index += keyword.length();
		}
		return count;
	}

	// Helper method to highlight keywords
	private String highlightKeyword(String text, String keyword) {
		// Case-insensitive highlighting that preserves original case
		Pattern pattern = Pattern.compile(Pattern.quote(keyword), Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(text);
		StringBuilder result = new StringBuilder();
		int lastEnd = 0;

		while (matcher.find()) {
			// Append text before the match
			result.append(text.substring(lastEnd, matcher.start()));
			// Append highlighting tags with the original case of the matched text
			result.append(HTMLFormatterWithScore.DEFAULT_PRE_TAG);
			result.append(text.substring(matcher.start(), matcher.end()));
			result.append(HTMLFormatterWithScore.DEFAULT_POST_TAG);
			lastEnd = matcher.end();
		}

		// Append the remainder of the text
		if (lastEnd < text.length()) {
			result.append(text.substring(lastEnd));
		}

		return result.toString();
	}

	/**
	 * Extracts the keyword query component from a potentially hybrid query. If the
	 * input query is a BooleanQuery containing a KnnVectorQuery, this method
	 * returns just the non-vector portion for highlighting.
	 * 
	 * @param query
	 *            The original query
	 * @return A query suitable for highlighting (without vector components)
	 */
	private static Query extractKeywordQueryFromHybrid(Query query) {
		// If not a boolean query, return as is
		if (!(query instanceof BooleanQuery)) {
			return query;
		}

		BooleanQuery bq = (BooleanQuery) query;
		boolean hasVectorComponent = false;
		Query keywordComponent = null;

		// Check if this is a hybrid query with vector components
		for (BooleanClause clause : bq.clauses()) {
			Query subQuery = clause.getQuery();

			// Handle BoostQuery wrapping
			if (subQuery instanceof BoostQuery) {
				subQuery = ((BoostQuery) subQuery).getQuery();
			}

			// Check if this is a vector query
			if (subQuery instanceof KnnFloatVectorQuery) {
				hasVectorComponent = true;
			} else {
				// Store the non-vector component
				keywordComponent = subQuery;
			}
		}

		// If we found a vector component and have a keyword component, return just the
		// keyword part
		if (hasVectorComponent && keywordComponent != null) {
			return keywordComponent;
		}

		// Otherwise, return the original query
		return query;
	}

	private boolean matchCategories(String[] categoryIndex, String[] categorySearch) {
		if (categorySearch == null || categorySearch.length == 0)
			return true;
		String search;
		for (int s = 0; s < categorySearch.length; s++) {
			search = categorySearch[s];
			for (int i = 0; i < categoryIndex.length; i++) {
				if (search.equals(categoryIndex[i]))
					return true;
			}
		}
		return false;
	}

	private boolean matchCategoryTree(String categoryTreeIndex, String categoryTreeSearch) {
		// if(StringUtil.isEmpty(categoryTreeIndex) || categoryTreeIndex.equals("/"))
		// return true;
		// if(StringUtil.isEmpty(categoryTreeSearch) || categoryTreeSearch.equals("/"))
		// return true;
		return categoryTreeIndex.startsWith(categoryTreeSearch);
	}

	/**
	 * list a directory and call every file
	 * 
	 * @param writer
	 * @param res
	 * @param filter
	 * @param url
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private int _list(String id, int doccount, IndexWriter writer, Resource res, ResourceFilter filter, String url) {

		if (res.isReadable()) {
			if (res.exists() && res.isDirectory()) {
				Resource[] files = (filter == null) ? res.listResources() : res.listResources(filter);
				if (files != null) {
					for (int i = 0; i < files.length; i++) {
						if (removeCorrupt(files[i])) {
							continue;
						}
						doccount = _list(id, doccount, writer, files[i], filter, url + "/" + files[i].getName());
					}
				}
			} else {
				try {
					info(res.getAbsolutePath());
					_index(id, writer, res, url);
					doccount++;
				} catch (Exception e) {
					error(e);
				}
			}
		}
		return doccount;
	}

	/**
	 * index a single file
	 * 
	 * @param writer
	 * @param file
	 * @param url
	 * @throws IOException
	 * @throws PageException
	 * @throws InterruptedException
	 */

	private void _index(String id, IndexWriter writer, Resource file, String url) throws IOException, PageException {
		if (!file.exists())
			return;

		writer.addDocument(embed(DocumentUtil.toDocument(file, url, engine.getSystemUtil().getCharset().name()), id));
	}

	private Document embed(Document doc, String id) throws IOException {
		EmbeddingService es = getEmbeddingService();
		doc.add(FieldUtil.UnIndexed("indexId", id));
		if (es != null) {
			String contents = doc.get("contents");
			if (contents != null) {
				float[] embedding = es.generate(contents);

				// Add the vector field
				doc.add(new KnnVectorField("embedding", embedding, VectorSimilarityFunction.COSINE));
			}

		}
		return doc;
	}

	public EmbeddingService getEmbeddingService() throws IOException {
		if (mode != MODE_KEYWORD && embeddingService == null) {
			String key = embedding + ":" + getLanguage();
			synchronized (CommonUtil.createToken("getEmbeddingService", key)) {
				if (embeddingService == null) {
					Config config = null;
					SearchEngine se = getSearchEngine();
					if (se instanceof SearchEngineSupport) {
						config = ((SearchEngineSupport) se).getConfig();
					}
					if (config == null) {
						config = CFMLEngineFactory.getInstance().getThreadConfig();
					}

					EmbeddingService tmp = embeddingServices.get(key);
					if (tmp == null) {
						tmp = createEmbeddingService(config, embedding, getLanguage());
						embeddingServices.put(key, tmp);
					}
					embeddingService = tmp;
				}
			}
		}
		return embeddingService;
	}

	public static EmbeddingService createEmbeddingService(Config config, String embedding, String language)
			throws IOException {
		EmbeddingService embeddingService;
		if (EMBEDDING_TF_IDF.equalsIgnoreCase(embedding)) {
			embeddingService = new TfIdfEmbeddingService();
		} else if (EMBEDDING_WORD2VEC.equalsIgnoreCase(embedding)) {
			embeddingService = new Word2VecEmbeddingService();
		}
		// TODO allow bundle defintion and Maven
		else {
			embeddingService = (EmbeddingService) CFMLEngineFactory.getInstance().getClassUtil()
					.loadInstance(embedding);

		}

		Struct params = CFMLEngineFactory.getInstance().getCreationUtil().createStruct();
		params.setEL("language", language);
		embeddingService.init(config, params);

		return embeddingService;
	}

	@Override
	public String getEmbedding() {
		return embedding;
	}

	@Override
	public double getRatio() {
		return ratio;
	}

	@Override
	public String getMode() {
		if (mode == MODE_HYBRID)
			return "hybrid";
		if (mode == MODE_VECTOR)
			return "vector";
		return "keyword";
	}

	/**
	 * @param id
	 * @return returns the Index Directory
	 */
	private Resource _getIndexDirectory(String id, boolean createIfNotExists) {
		Resource indexDir = collectionDir.getRealResource(id);
		if (createIfNotExists && !indexDir.exists())
			indexDir.mkdirs();
		return indexDir;
	}

	private IndexWriterConfig _getConfig() throws SearchException {
		return new IndexWriterConfig(SearchUtil.getAnalyzer(getLanguage()));
	}

	/**
	 * get writer to id
	 * 
	 * @param id
	 * @return returns the Writer
	 * @throws IOException
	 * @throws SearchException
	 * @throws IOException
	 * @throws PageException
	 */
	private IndexWriter _getWriter(String id, boolean create) throws SearchException, IOException, PageException {
		Resource dir = _getIndexDirectory(id, true);
		IndexWriterConfig config = _getConfig();
		// Set create/append mode
		if (create) {
			config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
		} else {
			config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
		}
		return new IndexWriter(FSDirectory.open(engine.getCastUtil().toFile(dir).toPath()), config);
	}

	private IndexReader _getReader(String id, boolean absolute) throws IOException, PageException {
		return _getReader(_getFile(id, absolute).toPath());
	}

	public static IndexReader _getReader(Path path) throws IOException {
		// Check if index exists using FSDirectory
		FSDirectory dir = FSDirectory.open(path);
		if (!DirectoryReader.indexExists(dir)) {
			throw new IOException("there is no index in [" + path + "]");
		}

		// Open reader using DirectoryReader
		return DirectoryReader.open(dir);
	}

	private File _getFile(String id, boolean absolute) throws IOException, PageException {
		Resource res = absolute ? engine.getResourceUtil().getFileResourceProvider().getResource(id)
				: _getIndexDirectory(id, true);
		res.getResourceProvider().read(res);
		return engine.getCastUtil().toFile(res);
	}

	/**
	 * @return returns all existing IndexWriter
	 */
	private Resource[] _getIndexDirectories() {
		Resource[] files = collectionDir.listResources(new DirectoryResourceFilter());

		return files;
	}

	/**
	 * @return returns all existing IndexWriter
	 * @throws SearchException
	 */
	private IndexWriter[] _getWriters(boolean create) throws SearchException {
		Resource[] files = _getIndexDirectories();
		if (files == null)
			return new IndexWriter[0];

		IndexWriter[] writers = new IndexWriter[files.length];
		for (int i = 0; i < files.length; i++) {
			try {
				writers[i] = _getWriter(files[i].getName(), create);
			} catch (IOException e) {
			} catch (PageException e) {
			}
		}
		return writers;
	}

	private int _countDocs(String col) {
		int totalDocs;
		IndexReader reader = null;
		try {
			reader = _getReader(col, true);
			totalDocs = reader.numDocs();
		} catch (Exception e) {
			return 0;
		} finally {
			closeEL(reader);
		}
		return totalDocs;
	}

	/**
	 * @deprecated see SearchUtil.getAnalyzer(String language);
	 * @param language
	 * @return returns language matching Analyzer
	 * @throws SearchException
	 */
	@Deprecated
	public static Analyzer _getAnalyzer(String language) throws SearchException {
		return SearchUtil.getAnalyzer(language);
	}

	/**
	 * check given language against collection language
	 * 
	 * @param language
	 * @throws SearchException
	 */
	private void _checkLanguage(String language) throws SearchException {

		if (language != null && !language.trim().equalsIgnoreCase(getLanguage())) {
			throw new SearchException(
					"collection Language and Index Language must be of same type, but collection language is of type ["
							+ getLanguage() + "] and index language is of type [" + language + "]");
		}
	}

	@Override
	public int getDocumentCount(String id) {
		try {
			if (!_getIndexDirectory(id, false).exists())
				return 0;
			IndexReader r = null;
			int num = 0;
			try {
				r = _getReader(id, false);
				num = r.numDocs();
			} finally {
				close(r);
			}
			return num;
		} catch (Exception e) {
		}
		return 0;
	}

	@Override
	public int getDocumentCount() {
		int count = 0;
		SearchIndex[] _indexes = getIndexes();
		for (int i = 0; i < _indexes.length; i++) {
			count += getDocumentCount(_indexes[i].getId());
		}

		return count;
	}

	@Override
	public long getSize() {
		return engine.getResourceUtil().getRealSize(collectionDir, null) / 1024;
	}

	@Override
	public Object getCategoryInfo() {
		Struct categories = engine.getCreationUtil().createStruct();
		Struct categorytrees = engine.getCreationUtil().createStruct();
		Struct info = engine.getCreationUtil().createStruct();
		info.setEL("categories", categories);
		info.setEL("categorytrees", categorytrees);

		Iterator it = indexes.keySet().iterator();
		String[] cats;
		String catTree;
		Double tmp;

		while (it.hasNext()) {
			SearchIndex index = indexes.get(it.next());

			// category tree
			catTree = index.getCategoryTree();
			tmp = (Double) categorytrees.get(catTree, null);
			if (tmp == null)
				categorytrees.setEL(catTree, engine.getCastUtil().toDouble(1));
			else
				categorytrees.setEL(catTree, engine.getCastUtil().toDouble(tmp.doubleValue() + 1));

			// categories
			cats = index.getCategories();
			for (int i = 0; i < cats.length; i++) {
				tmp = (Double) categories.get(cats[i], null);
				if (tmp == null)
					categories.setEL(cats[i], engine.getCastUtil().toDouble(1));
				else
					categories.setEL(cats[i], engine.getCastUtil().toDouble(tmp.doubleValue() + 1));
			}
		}
		return info;
	}

	class ResourceIndexWriter extends IndexWriter {
		private Resource dir;

		public ResourceIndexWriter(Resource dir, Analyzer analyzer, boolean create) throws IOException, PageException {
			super(FSDirectory.open(CFMLEngineFactory.getInstance().getCastUtil().toFile(dir).toPath()),
					new IndexWriterConfig(analyzer).setOpenMode(create ? OpenMode.CREATE : OpenMode.CREATE_OR_APPEND));
			this.dir = dir;
			dir.getResourceProvider().lock(dir);
		}

		@Override
		public synchronized void close() throws IOException {
			super.close();
			dir.getResourceProvider().unlock(dir);
		}
	}

	private Resource _createSpellDirectory(String id) {
		Resource indexDir = collectionDir.getRealResource(id + "_" + (_getMax(true) + 1) + "_spell");
		// print.out("create:"+indexDir);
		indexDir.mkdirs();
		return indexDir;
	}

	private Resource _getSpellDirectory(String id) {
		Resource indexDir = collectionDir.getRealResource(id + "_" + _getMax(false) + "_spell");
		// print.out("get:"+indexDir);
		return indexDir;
	}

	public Resource getRootDirectory() {
		return collectionDir;
	}

	private long _getMax(boolean delete) {
		Resource[] children = collectionDir.listResources(new SpellDirFilter());
		long max = 0, nbr;
		String name;
		for (int i = 0; i < children.length; i++) {
			name = children[i].getName();
			name = name.substring(0, name.length() - 6);
			nbr = engine.getCastUtil().toLongValue(name.substring(name.lastIndexOf('_') + 1), 0);
			if (delete) {
				try {
					children[i].remove(true);
					continue;
				} catch (Throwable t) {
					if (t instanceof ThreadDeath)
						throw (ThreadDeath) t;
				}
			}
			if (nbr > max)
				max = nbr;
		}
		return max;
	}

	private void error(Exception e) {
		e.printStackTrace();
		if (log == null) {
			e.printStackTrace();
			return;
		}
		log.log(Log.LEVEL_ERROR, "Collection:" + getName(), e);
	}

	private void info(String doc) {
		if (log == null)
			return;
		log.log(Log.LEVEL_INFO, "Collection:" + getName(), "indexing " + doc);
	}

	public class SpellDirFilter implements ResourceNameFilter {

		@Override
		public boolean accept(Resource parent, String name) {
			return name.endsWith("_spell");
		}

	}

	private static String toIdentityVariableName(String varName) {
		char[] chars = varName.toCharArray();
		long changes = 0;

		StringBuilder rtn = new StringBuilder(chars.length + 2);
		rtn.append("CF");

		for (int i = 0; i < chars.length; i++) {
			char c = chars[i];
			if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9'))
				rtn.append(c);
			else {
				rtn.append('_');
				changes += (c * (i + 1));
			}
		}

		return rtn.append(changes).toString();
	}
}