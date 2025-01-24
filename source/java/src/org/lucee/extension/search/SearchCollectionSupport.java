package org.lucee.extension.search;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.w3c.dom.Element;

import lucee.commons.io.log.Log;
import lucee.commons.io.res.Resource;
import lucee.commons.lock.KeyLock;
import lucee.commons.lock.Lock;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.PageContext;
import lucee.runtime.exp.PageException;
import lucee.runtime.search.IndexResult;
import lucee.runtime.search.SearchCollection;
import lucee.runtime.search.SearchData;
import lucee.runtime.search.SearchEngine;
import lucee.runtime.search.SearchException;
import lucee.runtime.search.SearchIndex;
import lucee.runtime.search.SearchResulItem;
import lucee.runtime.type.Collection.Key;
import lucee.runtime.type.Query;
import lucee.runtime.type.QueryColumn;
import lucee.runtime.type.dt.DateTime;
import lucee.runtime.util.Creation;
import lucee.runtime.util.HTTPUtil;

/**
 * represent a single Collection
 */
public abstract class SearchCollectionSupport implements SearchCollection {

	public static final char FILE_SEPERATOR = File.separatorChar;
	public static final char FILE_ANTI_SEPERATOR = (FILE_SEPERATOR == '/') ? '\\' : '/';

	private static final long serialVersionUID = 8089312879341384114L;

	private static final int LOCK_TIMEOUT = 10 * 60 * 1000; // ten minutes
	private final String name;
	private final Resource path;
	private String language;
	private DateTime lastUpdate;
	private SearchEngineSupport searchEngine;
	// TODO change visibility to private
	protected Map<String, SearchIndex> indexes = new ConcurrentHashMap<String, SearchIndex>();

	private DateTime created;

	private Log log;
	// private static LockManager manager=Lock Manager Impl.getInstance();
	private final KeyLock<String> lock;

	private final CFMLEngine engine;

	/**
	 * constructor of the class
	 * 
	 * @param searchEngine
	 * @param name
	 *            name of the Collection
	 * @param path
	 * @param language
	 * @param count
	 *            total count of documents in the collection
	 * @param lastUpdate
	 * @param created
	 */
	public SearchCollectionSupport(SearchEngineSupport searchEngine, String name, Resource path, String language,
			DateTime lastUpdate, DateTime created) {
		engine = CFMLEngineFactory.getInstance();
		lock = engine.getCreationUtil().createKeyLock();

		this.searchEngine = searchEngine;
		this.name = name;
		this.path = path;
		this.language = SearchUtil.translateLanguage(language);
		this.lastUpdate = lastUpdate;
		this.created = created;

		log = searchEngine.getConfig().getLog("search");
	}

	@Override
	public final void create() throws SearchException {
		Lock l = lock();
		try {
			_create();
		} finally {
			unlock(l);
		}
	}

	/**
	 * create a collection
	 * 
	 * @throws SearchException
	 */
	protected abstract void _create() throws SearchException;

	@Override
	public final void optimize() throws SearchException {
		Lock l = lock();
		try {
			_optimize();
			changeLastUpdate();
		} finally {
			unlock(l);
		}
	}

	/**
	 * optimize a Collection
	 * 
	 * @throws SearchException
	 */
	protected abstract void _optimize() throws SearchException;

	@Override
	public final void map(Resource path) throws SearchException {
		Lock l = lock();
		try {
			_map(path);
			changeLastUpdate();
		} finally {
			unlock(l);
		}
	}

	/**
	 * map a Collection
	 * 
	 * @param path
	 * @throws SearchException
	 */
	protected abstract void _map(Resource path) throws SearchException;

	@Override
	public final void repair() throws SearchException {
		Lock l = lock();
		try {
			_repair();
			changeLastUpdate();
		} finally {
			unlock(l);
		}
	}

	/**
	 * repair a Collection
	 * 
	 * @throws SearchException
	 */
	protected abstract void _repair() throws SearchException;

	@Override
	public IndexResult index(PageContext pc, String key, short type, String urlpath, String title, String body,
			String language, String[] extensions, String query, boolean recurse, String categoryTree,
			String[] categories, long timeout, String custom1, String custom2, String custom3, String custom4)
			throws PageException, MalformedURLException, SearchException {
		language = SearchUtil.translateLanguage(language);
		Lock l = lock();
		try {
			SearchIndex si = new SearchIndexImpl(title, key, type, query, extensions, language, urlpath, categoryTree,
					categories, custom1, custom2, custom3, custom4);
			String id = si.getId();
			IndexResult ir = IndexResultImpl.EMPTY;
			if (type == SearchIndex.TYPE_FILE) {
				Resource file = engine.getResourceUtil().toResourceNotExisting(pc, key);
				if (!file.isFile())
					throw new SearchException(
							"value of attribute key must specify a existing file, [" + key + "] is invalid");
				ir = indexFile(id, title, file, language);
			} else if (type == SearchIndex.TYPE_PATH) {
				Resource dir = engine.getResourceUtil().toResourceNotExisting(pc, key);
				if (!dir.isDirectory())
					throw new SearchException(
							"value of attribute key must specify a existing directory, [" + key + "] is invalid");
				ir = indexPath(id, title, dir, extensions, recurse, language);
			} else if (type == SearchIndex.TYPE_URL) {
				ir = indexURL(id, title, new URL(key), extensions, recurse, language, timeout);
			} else if (type == SearchIndex.TYPE_CUSTOM) {
				Query qv;
				if (engine.getStringUtil().isEmpty(query)) {

					// set columns
					List<String> cols = new ArrayList<String>();
					cols.add("key");
					cols.add("body");
					if (!Util.isEmpty(title))
						cols.add("title");
					if (!Util.isEmpty(urlpath))
						cols.add("urlpath");
					if (!Util.isEmpty(custom1))
						cols.add("custom1");
					if (!Util.isEmpty(custom2))
						cols.add("custom2");
					if (!Util.isEmpty(custom3))
						cols.add("custom3");
					if (!Util.isEmpty(custom4))
						cols.add("custom4");

					// populate query with a single row
					qv = engine.getCreationUtil().createQuery(cols.toArray(new String[cols.size()]), 1, "query");
					// body
					qv.setAt("key", 1, key);
					key = "key";

					// body
					qv.setAt("body", 1, body);
					body = "body";

					// title
					if (!Util.isEmpty(title)) {
						qv.setAt("title", 1, title);
						title = "title";
					}

					// urlpath
					if (!Util.isEmpty(urlpath)) {
						qv.setAt("urlpath", 1, urlpath);
						urlpath = "urlpath";
					}

					// custom1
					if (!Util.isEmpty(custom1)) {
						qv.setAt("custom1", 1, custom1);
						custom1 = "custom1";
					}
					// custom2
					if (!Util.isEmpty(custom2)) {
						qv.setAt("custom2", 1, custom2);
						custom2 = "custom2";
					}
					// custom3
					if (!Util.isEmpty(custom3)) {
						qv.setAt("custom3", 1, custom3);
						custom3 = "custom3";
					}
					// custom4
					if (!Util.isEmpty(custom4)) {
						qv.setAt("custom4", 1, custom4);
						custom4 = "custom4";
					}
				} else
					qv = engine.getCastUtil().toQuery(pc.getVariable(query));

				QueryColumn keyColumn = qv.getColumn(key);

				String[] strBodies = engine.getListUtil()
						.toStringArrayTrim(engine.getListUtil().toArrayRemoveEmpty(body, ","));
				QueryColumn[] bodyColumns = new QueryColumn[strBodies.length];
				for (int i = 0; i < bodyColumns.length; i++) {
					bodyColumns[i] = qv.getColumn(strBodies[i]);
				}

				ir = indexCustom(id, getValue(qv, title), keyColumn, bodyColumns, language, getValue(qv, urlpath),
						getValue(qv, custom1), getValue(qv, custom2), getValue(qv, custom3), getValue(qv, custom4));
			}
			createIndex(si);
			return ir;
		} finally {
			unlock(l);
		}
	}

	private QueryColumn getColumnEL(Query query, String column) {
		if (Util.isEmpty(column))
			return null;
		QueryColumn c = query.getColumn(column, null);

		return c;
	}

	private Object getValue(Query query, String column) {
		if (Util.isEmpty(column))
			return null;
		QueryColumn c = query.getColumn(column, null);
		if (c == null)
			return column;
		return c;
	}

	@Override
	public final IndexResult indexFile(String id, String title, Resource res, String language) throws SearchException {
		IndexResult ir = _indexFile(id, title, res, language);
		changeLastUpdate();
		return ir;
	}

	/**
	 * updates a collection with a file
	 * 
	 * @param id
	 * @param title
	 * @param file
	 * @param language
	 * @throws SearchException
	 */
	protected abstract IndexResult _indexFile(String id, String title, Resource file, String language)
			throws SearchException;

	@Override
	public final IndexResult indexPath(String id, String title, Resource dir, String[] extensions, boolean recurse,
			String language) throws SearchException {
		IndexResult ir = _indexPath(id, title, dir, extensions, recurse, language);
		changeLastUpdate();
		return ir;
	}

	/**
	 * updates a collection with a path
	 * 
	 * @param id
	 * @param title
	 * @param dir
	 * @param recurse
	 * @param extensions
	 * @param language
	 * @throws SearchException
	 */
	protected abstract IndexResult _indexPath(String id, String title, Resource dir, String[] extensions,
			boolean recurse, String language) throws SearchException;

	@Override
	public final IndexResult indexURL(String id, String title, URL url, String[] extensions, boolean recurse,
			String language, long timeout) throws SearchException {
		IndexResult ir = _indexURL(id, title, url, extensions, recurse, language, timeout);
		changeLastUpdate();
		return ir;
	}

	/**
	 * updates a collection with a url
	 * 
	 * @param id
	 * @param title
	 * @param recurse
	 * @param extensions
	 * @param url
	 * @param language
	 * @throws SearchException
	 */
	protected abstract IndexResult _indexURL(String id, String title, URL url, String[] extensions, boolean recurse,
			String language, long timeout) throws SearchException;

	protected abstract IndexResult _indexURL(String id, String title, URL url, String[] extensions, boolean recurse,
			String language) throws SearchException;

	@Override
	public final IndexResult indexCustom(String id, QueryColumn title, QueryColumn keyColumn, QueryColumn[] bodyColumns,
			String language, QueryColumn custom1, QueryColumn custom2, QueryColumn custom3, QueryColumn custom4)
			throws SearchException {
		IndexResult ir = _indexCustom(id, title, keyColumn, bodyColumns, language, null, custom1, custom2, custom3,
				custom4);
		changeLastUpdate();
		return ir;
	}

	public final IndexResult indexCustom(String id, QueryColumn title, QueryColumn keyColumn, QueryColumn[] bodyColumns,
			String language, QueryColumn urlPath, QueryColumn custom1, QueryColumn custom2, QueryColumn custom3,
			QueryColumn custom4) throws SearchException {
		IndexResult ir = _indexCustom(id, title, keyColumn, bodyColumns, language, null, custom1, custom2, custom3,
				custom4);
		changeLastUpdate();
		return ir;
	}

	public final IndexResult indexCustom(String id, Object title, QueryColumn keyColumn, QueryColumn[] bodyColumns,
			String language, Object urlpath, Object custom1, Object custom2, Object custom3, Object custom4)
			throws SearchException {
		IndexResult ir = _indexCustom(id, title, keyColumn, bodyColumns, language, urlpath, custom1, custom2, custom3,
				custom4);
		changeLastUpdate();
		return ir;
	}

	public final IndexResult deleteCustom(String id, QueryColumn keyColumn) throws SearchException {
		IndexResult ir = _deleteCustom(id, keyColumn);
		changeLastUpdate();
		return ir;
	}

	protected abstract IndexResult _deleteCustom(String id, QueryColumn keyColumn) throws SearchException;

	/**
	 * updates a collection with a custom
	 * 
	 * @param id
	 * @param title
	 *            Title for the Index
	 * @param keyColumn
	 *            Key Column
	 * @param bodyColumns
	 *            Body Column Array
	 * @param language
	 *            Language for index
	 * @param custom1
	 * @param custom2
	 * @param custom3
	 * @param custom4
	 * @throws SearchException
	 */
	// protected abstract IndexResult _indexCustom(String id,QueryColumn title,
	// QueryColumn keyColumn, QueryColumn[] bodyColumns, String language,
	// QueryColumn custom1, QueryColumn custom2, QueryColumn custom3, QueryColumn
	// custom4) throws SearchException;
	protected abstract IndexResult _indexCustom(String id, Object title, QueryColumn keyColumn,
			QueryColumn[] bodyColumns, String language, Object urlpath, Object custom1, Object custom2, Object custom3,
			Object custom4) throws SearchException;

	/**
	 * @param index
	 * @throws SearchException
	 */
	private void createIndex(SearchIndex index) throws SearchException {
		Iterator<String> it = indexes.keySet().iterator();
		SearchIndex otherIndex = null;

		while (it.hasNext()) {
			Object key = it.next();
			if (key.equals(index.getId())) {
				otherIndex = indexes.get(key);
				break;
			}
		}

		Element collElement = searchEngine.getCollectionElement(name);

		// Insert
		if (otherIndex == null) {
			addIndex(index);
			collElement.appendChild(searchEngine.toElement(index));
		}
		// Update
		else {
			addIndex(index);
			Element el = searchEngine.getIndexElement(collElement, index.getId());
			searchEngine.setAttributes(el, index);
		}
		changeLastUpdate();
	}

	/**
	 * @param index
	 */
	@Override
	public void addIndex(SearchIndex index) {
		indexes.put(index.getId(), index);
	}

	@Override
	public final String getLanguage() {
		return language;
	}

	@Override
	public final IndexResult purge() throws SearchException {
		Lock l = lock();
		try {
			indexes.clear();
			IndexResult ir = _purge();
			searchEngine.purgeCollection(this);
			changeLastUpdate();
			return ir;
		} finally {
			unlock(l);
		}
	}

	/**
	 * purge a collection
	 * 
	 * @throws SearchException
	 */
	protected abstract IndexResult _purge() throws SearchException;

	@Override
	public final IndexResult delete() throws SearchException {
		Lock l = lock();
		try {
			IndexResult ir = _delete();
			searchEngine.removeCollection(this);
			return ir;
		} finally {
			unlock(l);
		}
	}

	/**
	 * delete the collection from a file
	 * 
	 * @throws SearchException
	 */
	protected abstract IndexResult _delete() throws SearchException;

	@Override
	public final IndexResult deleteIndex(PageContext pc, String key, short type, String queryName)
			throws SearchException {
		// if(queryName==null) queryName="";
		Key k;

		if (type == SearchIndex.TYPE_CUSTOM) {
			// delete all when no key is defined
			if (Util.isEmpty(key, true))
				return deleteIndexNotCustom(pc, key, type, queryName);

			try {
				Query qv;
				if (!Util.isEmpty(queryName)) {
					k = engine.getCastUtil().toKey(key);
					qv = engine.getCastUtil().toQuery(pc.getVariable(queryName));
				} else {
					k = engine.getCastUtil().toKey("id");
					Key[] cols = new Key[] { k };
					String[] types = new String[] { "VARCHAR" };
					qv = engine.getCreationUtil().createQuery(cols, 1, "query");
					qv.setAtEL(k, 1, key);
				}

				QueryColumn keyColumn = qv.getColumn(k);
				return deleteCustom("custom", keyColumn);
			} catch (PageException pe) {
				throw new SearchException(pe);
			}
		}
		return deleteIndexNotCustom(pc, key, type, queryName);

	}

	public final IndexResult deleteIndexNotCustom(PageContext pc, String key, short type, String queryName)
			throws SearchException {
		Iterator<String> it = indexes.keySet().iterator();
		while (it.hasNext()) {
			String id = it.next();
			if (id.equals(SearchIndexImpl.toId(type, key, queryName))) {
				SearchIndex index = indexes.get(id);

				IndexResult ir = _deleteIndex(index.getId());
				Element indexEl = searchEngine.getIndexElement(searchEngine.getCollectionElement(name), index.getId());
				if (indexEl != null)
					indexEl.getParentNode().removeChild(indexEl);
				changeLastUpdate();
				return ir;
			}
		}
		return new IndexResultImpl(0, 0, 0);
	}

	/**
	 * delete a Index from collection
	 * 
	 * @param id
	 *            id ofthe Index to delete
	 * @throws SearchException
	 */
	protected abstract IndexResult _deleteIndex(String id) throws SearchException;

	@Override
	public final Resource getPath() {
		return path;
	}

	@Override
	public DateTime getCreated() {
		return created;
	}

	@Override
	public final DateTime getLastUpdate() {
		return lastUpdate;
	}

	@Override
	public final String getName() {
		return name;
	}

	@Override
	public final Log getLogger() {
		return log;
	}

	@Override
	public final SearchEngine getSearchEngine() {
		return searchEngine;
	}

	/**
	 * change the last update attribute and store it
	 * 
	 * @throws SearchException
	 */
	private void changeLastUpdate() throws SearchException {
		lastUpdate = engine.getCreationUtil().createDateTime(System.currentTimeMillis());
		searchEngine.store();
	}

	@Override
	public Object created() {
		return created;
	}

	public abstract SearchResulItem[] _search(SearchData data, String criteria, String language, short type,
			int startrow, int maxrow, String categoryTree, String[] category) throws SearchException;

	@Override
	public final int search(SearchData data, Query qry, String criteria, String language, short type, int startrow,
			int maxrow, String categoryTree, String[] categories) throws SearchException, PageException {
		int len = qry.getRecordcount();
		SearchResulItem[] records;
		boolean hasRowHandling = false;

		Lock l = lock();
		try {
			records = _search(data, criteria, language, type, startrow, maxrow, categoryTree, categories);
		} finally {
			unlock(l);
		}

		// Startrow
		if (!hasRowHandling && startrow > 1) {

			if (startrow > records.length) {
				return startrow - records.length;
			}
			int start = startrow - 1;

			SearchResulItem[] tmpRecords = new SearchResulItem[records.length - start];
			for (int i = start; i < records.length; i++) {
				tmpRecords[i - start] = records[i];
			}
			records = tmpRecords;
			startrow = 1;
		}

		if (records != null && records.length > 0) {
			int to = (!hasRowHandling && maxrow > -1 && len + records.length > maxrow) ? maxrow - len : records.length;
			qry.addRow(to);

			Creation creator = engine.getCreationUtil();

			Key _title = creator.createKey("title");
			Key _custom1 = creator.createKey("custom1");
			Key _custom2 = creator.createKey("custom2");
			Key _custom3 = creator.createKey("custom3");
			Key _custom4 = creator.createKey("custom4");
			Key _categoryTree = creator.createKey("categoryTree");
			Key _category = creator.createKey("category");
			Key _type = creator.createKey("type");
			Key _author = creator.createKey("author");
			Key _size = creator.createKey("size");
			Key _summary = creator.createKey("summary");
			Key _context = creator.createKey("context");
			Key _score = creator.createKey("score");
			Key _key = creator.createKey("key");
			Key _url = creator.createKey("url");
			Key _collection = creator.createKey("collection");
			Key _rank = creator.createKey("rank");

			String title;
			String custom1;
			String custom2;
			String custom3;
			String custom4;
			String url;
			SearchResulItem record;
			SearchIndex si;
			for (int y = 0; y < to; y++) {

				int row = len + y + 1;
				record = records[y];
				si = indexes.get(record.getId());

				title = record.getTitle();
				custom1 = record.getCustom1();
				custom2 = record.getCustom2();
				custom3 = record.getCustom3();
				custom4 = record.getCustom4();
				url = record.getUrl();

				qry.setAt(_title, row, title);
				qry.setAt(_custom1, row, custom1);
				qry.setAt(_custom2, row, custom2);
				qry.setAt(_custom3, row, custom3);
				qry.setAt(_custom4, row, custom4);
				qry.setAt(_categoryTree, row, record.getCategoryTree());
				qry.setAt(_category, row, record.getCategory());
				qry.setAt(_type, row, record.getMimeType());
				qry.setAt(_author, row, record.getAuthor());
				qry.setAt(_size, row, record.getSize());

				qry.setAt(_summary, row, record.getSummary());
				qry.setAt(_context, row, ((SearchResulItemImpl) record).getContext());
				// qry.setAt("context", row, record.getContextSummary());
				qry.setAt(_score, row, Double.valueOf(record.getScore()));
				qry.setAt(_key, row, record.getKey());
				qry.setAt(_url, row, url);
				qry.setAt(_collection, row, getName());
				qry.setAt(_rank, row, Double.valueOf(row));
				String rootPath, file;
				String urlPath;
				if (si != null) {
					switch (si.getType()) {
					case SearchIndex.TYPE_PATH:
						rootPath = si.getKey();
						rootPath = rootPath.replace(FILE_ANTI_SEPERATOR, FILE_SEPERATOR);
						file = record.getKey();
						file = file.replace(FILE_ANTI_SEPERATOR, FILE_SEPERATOR);
						qry.setAt(_url, row, toURL(si.getUrlpath(),
								engine.getStringUtil().replace(file, rootPath, "", true, false)));

						break;
					case SearchIndex.TYPE_URL:
						rootPath = si.getKey();
						urlPath = si.getUrlpath();
						try {
							rootPath = getDirectory(si.getKey());
						} catch (MalformedURLException e) {
						}
						if (Util.isEmpty(urlPath))
							urlPath = rootPath;
						file = record.getKey();
						qry.setAt(_url, row,
								toURL(urlPath, engine.getStringUtil().replace(file, rootPath, "", true, false)));

						break;
					case SearchIndex.TYPE_CUSTOM:
						qry.setAt(_url, row, url);
						break;
					default:
						qry.setAt(_url, row, toURL(si.getUrlpath(), url));
						break;
					}

					if (Util.isEmpty(title))
						qry.setAt(_title, row, si.getTitle());
					if (Util.isEmpty(custom1))
						qry.setAt(_custom1, row, si.getCustom1());
					if (Util.isEmpty(custom2))
						qry.setAt(_custom2, row, si.getCustom2());
					if (Util.isEmpty(custom3))
						qry.setAt(_custom3, row, si.getCustom3());
					if (Util.isEmpty(custom4))
						qry.setAt(_custom4, row, si.getCustom4());

				}
			}
		}
		return startrow;
	}

	public static String getDirectory(String strUrl) throws MalformedURLException {
		HTTPUtil http = CFMLEngineFactory.getInstance().getHTTPUtil();
		URL url = new URL(strUrl);
		String path = url.getPath();
		int slashIndex = path.lastIndexOf('/');
		int dotIndex = path.lastIndexOf('.');
		// no dot
		if (dotIndex == -1) {
			if (path.endsWith("/"))
				return http.removeUnecessaryPort(url).toExternalForm();
			return http.removeUnecessaryPort(new URL(url.getProtocol(), url.getHost(), url.getPort(), path + "/"))
					.toExternalForm();
		}
		if (slashIndex > dotIndex) {
			path = path.substring(0, dotIndex);
			slashIndex = path.lastIndexOf('/');
		}

		return http
				.removeUnecessaryPort(
						new URL(url.getProtocol(), url.getHost(), url.getPort(), path.substring(0, slashIndex + 1)))
				.toExternalForm();
	}

	private static String toURL(String url, String path) {
		if (Util.isEmpty(url))
			return path;
		if (Util.isEmpty(path))
			return url;

		url = url.replace('\\', '/');
		path = path.replace('\\', '/');
		if (path.startsWith("/"))
			path = path.substring(1);
		if (url.endsWith("/"))
			url = url.substring(0, url.length() - 1);

		if (CFMLEngineFactory.getInstance().getStringUtil().startsWithIgnoreCase(path, url))
			return path;
		return url + "/" + path;
	}

	protected SearchIndex[] getIndexes() {
		Iterator<Entry<String, SearchIndex>> it = indexes.entrySet().iterator();
		int len = indexes.size();
		SearchIndex[] rtn = new SearchIndex[len];
		int count = 0;
		while (it.hasNext()) {
			rtn[count++] = it.next().getValue();
		}
		return rtn;
	}

	private Lock lock() throws SearchException {
		try {
			return lock.lock(getId(), LOCK_TIMEOUT);
			// manager.lock(LockManager.TYPE_EXCLUSIVE,getId(),LOCK_TIMEOUT,ThreadLocalPageContext.get().getId());
		} catch (Exception e) {
			throw new SearchException(e);
		}

	}

	private void unlock(Lock l) {
		lock.unlock(l);
		// manager.unlock(ThreadLocalPageContext.get().getId());
	}

	private String getId() {
		return path.getRealResource(name).getAbsolutePath();
	}

	@Override
	public Query getIndexesAsQuery() {
		Iterator<Entry<String, SearchIndex>> it = indexes.entrySet().iterator();

		final String v = "VARCHAR";
		Query query = null;
		String[] cols = new String[] { "categories", "categoryTree", "custom1", "custom2", "custom3", "custom4",
				"extensions", "key", "language", "query", "title", "urlpath", "type" };
		String[] types = new String[] { v, v, v, v, v, v, v, v, v, v, v, v, v };
		try {
			query = engine.getCreationUtil().createQuery(cols, types, 0, "query");
		} catch (PageException e) {
			query = engine.getCreationUtil().createQuery(cols, 0, "query");
		}

		Entry<String, SearchIndex> entry;
		SearchIndex index;
		int row = 0;
		while (it.hasNext()) {
			query.addRow();
			row++;
			entry = it.next();
			index = entry.getValue();
			if (index == null)
				continue;
			try {

				query.setAt("categories", row, engine.getListUtil().toList(index.getCategories(), ""));
				query.setAt("categoryTree", row, index.getCategoryTree());

				query.setAt("custom1", row, index.getCustom1());
				query.setAt("custom2", row, index.getCustom2());
				query.setAt("custom3", row, index.getCustom3());
				query.setAt("custom4", row, index.getCustom4());

				query.setAt("extensions", row, engine.getListUtil().toList(index.getExtensions(), ","));
				query.setAt("key", row, index.getKey());
				query.setAt("language", row, index.getLanguage());
				query.setAt("query", row, index.getQuery());
				query.setAt("title", row, index.getTitle());
				query.setAt("urlpath", row, index.getUrlpath());
				query.setAt("type", row, SearchIndexImpl.toStringTypeEL(index.getType()));

			} catch (PageException pe) {
			}
		}
		return query;
	}

}