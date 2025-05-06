package org.lucee.extension.search;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.lucee.extension.search.lucene.util.CommonUtil;
import org.lucee.extension.search.lucene.util.XMLUtil;

import lucee.commons.io.res.Resource;
import lucee.commons.io.res.ResourceProvider;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.config.Config;
import lucee.runtime.exp.PageException;
import lucee.runtime.search.SearchCollection;
import lucee.runtime.search.SearchEngine;
import lucee.runtime.search.SearchException;
import lucee.runtime.search.SearchIndex;
import lucee.runtime.type.Array;
import lucee.runtime.type.Collection;
import lucee.runtime.type.Collection.Key;
import lucee.runtime.type.Query;
import lucee.runtime.type.Struct;
import lucee.runtime.type.dt.DateTime;
import lucee.runtime.util.Cast;

/**
 * 
 */
public abstract class SearchEngineSupport implements SearchEngine {

	private Resource searchFile;
	private Resource searchDir;
	final Map<String, SearchCollection> collections;
	protected Config config;
	private CFMLEngine engine;
	private Struct root;

	public SearchEngineSupport() {
		engine = CFMLEngineFactory.getInstance();
		collections = new ConcurrentHashMap<>();
	}

	@Override
	public void init(lucee.runtime.config.Config config, Resource searchDir) throws IOException, SearchException {
		this.config = config;
		this.searchDir = searchDir;

		this.searchFile = searchDir.getRealResource("search.json");

		// no json
		if (!searchFile.exists() || searchFile.length() == 0) {

			// do we have xml?
			Resource searchFileXML = searchDir.getRealResource("search.xml");
			if (searchFileXML.exists() && searchFileXML.length() > 0) {
				root = XMLUtil.importXML(config, searchFileXML);
			} else {
				root = engine.getCreationUtil().createStruct();
			}
			store(false);
		} else {
			try {
				root = engine.getCastUtil()
						.fromJsonStringToStruct(engine.getIOUtil().toString(searchFile, CommonUtil.UTF8));
			} catch (PageException e) {
				throw CommonUtil.toSearchException(e);
			}
		}
		try {
			readCollections(config, root);
		} catch (PageException e) {
			throw CommonUtil.toSearchException(e);
		}
	}

	private void readCollections(Config config, Struct root) throws SearchException, PageException {
		Array arrColl = engine.getCastUtil().toArray(root.get("collections", null), null);
		if (arrColl != null) {
			Iterator<Object> it = arrColl.valueIterator();
			Struct sctColl;
			while (it.hasNext()) {
				sctColl = engine.getCastUtil().toStruct(it.next(), null);

				if (sctColl != null) {
					readCollection(config, sctColl);
				}
			}
		}
	}

	private final void readCollection(Config config, Struct sctColl) throws SearchException, PageException {
		SearchCollection sc;
		Cast cast = engine.getCastUtil();
		DateTime last = cast.toDateTime(sctColl.get("lastUpdate", null), engine.getThreadTimeZone(), null);
		if (last == null) {
			last = engine.getCreationUtil().now();
		}

		DateTime cre = cast.toDateTime(sctColl.get("created", null), engine.getThreadTimeZone(), null);
		if (cre == null) {
			cre = engine.getCreationUtil().now();
		}

		ResourceProvider frp = engine.getResourceUtil().getFileResourceProvider();

		sc = _readCollection(

				cast.toString(sctColl.get("name")),

				frp.getResource(cast.toString(sctColl.get("path"))),

				cast.toString(sctColl.get("language", null), null),

				last, cre,

				cast.toString(sctColl.get("mode", null), null),

				cast.toString(sctColl.get("embedding", null), null),

				cast.toDoubleValue(sctColl.get("ratio", null), 0.5D)

		);
		collections.put(sc.getName().toLowerCase(), sc);

		// Indexes
		Array arrIdx = engine.getCastUtil().toArray(sctColl.get("indexes", null), null);
		if (arrIdx != null && arrIdx.size() > 0) {
			Iterator<Object> it = arrIdx.valueIterator();
			Struct sctIdx;
			while (it.hasNext()) {
				sctIdx = cast.toStruct(it.next(), null);
				if (sctIdx != null) {
					readIndex(sc, sctIdx);
				}
			}
		}
	}

	protected void readIndex(SearchCollection sc, Struct el) throws SearchException {
		// Index
		SearchIndex si = new SearchIndexImpl(sc, _attr(el, "id"), _attr(el, "title"), _attr(el, "key"),
				SearchIndexImpl.toType(_attr(el, "type")), _attr(el, "query"),
				engine.getListUtil().toStringArray(_attr(el, "extensions"), ","), _attr(el, "language"),
				_attr(el, "urlpath"), _attr(el, "categoryTree"),
				engine.getListUtil().toStringArray(_attr(el, "category"), ","), _attr(el, "custom1"),
				_attr(el, "custom2"), _attr(el, "custom3"), _attr(el, "custom4"));
		sc.addIndex(si);
	}

	private String _attr(Struct sctIdx, String attr) {
		return engine.getStringUtil().emptyIfNull(engine.getCastUtil().toString(sctIdx.get(attr, null), null));
	}

	@Override
	public final SearchCollection getCollectionByName(String name) throws SearchException {
		SearchCollection sc = collections.get(name.toLowerCase());
		if (sc != null)
			return sc;
		throw new SearchException("collection " + name + " is undefined");
	}

	@Override
	public final Query getCollectionsAsQuery() {
		final String v = "VARCHAR";
		Query query = null;
		String[] cols = new String[] { "external", "language", "mapped", "name", "online", "path", "registered",
				"lastmodified", "categories", "charset", "created", "size", "doccount", "mode", "embedding", "ratio" };
		String[] types = new String[] { "BOOLEAN", v, "BOOLEAN", v, "BOOLEAN", v, v, "DATE", "BOOLEAN", v, "OBJECT",
				"DOUBLE", "DOUBLE", v };
		try {
			query = engine.getCreationUtil().createQuery(cols, types, collections.size(), "query");
		} catch (PageException e) {
			query = engine.getCreationUtil().createQuery(cols, collections.size(), "query");
		}

		// Collection.Key[] keys = collections.keys();
		int i = -1;
		SearchCollectionSupport scs;
		for (SearchCollection coll : collections.values()) {
			i++;
			try {
				query.setAt("external", i + 1, Boolean.FALSE);
				query.setAt("charset", i + 1, "UTF-8");
				query.setAt("created", i + 1, coll.created());

				query.setAt("categories", i + 1, Boolean.TRUE);
				query.setAt("language", i + 1, coll.getLanguage());
				query.setAt("mapped", i + 1, Boolean.FALSE);
				query.setAt("name", i + 1, coll.getName());
				query.setAt("online", i + 1, Boolean.TRUE);
				query.setAt("path", i + 1, coll.getPath().getAbsolutePath());
				query.setAt("registered", i + 1, "CF");
				query.setAt("lastmodified", i + 1, coll.getLastUpdate());
				query.setAt("size", i + 1, Double.valueOf(coll.getSize()));
				query.setAt("doccount", i + 1, Double.valueOf(coll.getDocumentCount()));

				if (coll instanceof SearchCollectionSupport) {
					scs = (SearchCollectionSupport) coll;
					query.setAt("mode", i + 1, scs.getMode());
					query.setAt("embedding", i + 1, scs.getEmbedding());
					query.setAt("ratio", i + 1, scs.getRatio());
				}

			} catch (PageException pe) {
			}
		}
		return query;
	}

	@Override
	public final SearchCollection createCollection(String name, Resource path, String language, boolean allowOverwrite)
			throws SearchException {
		SearchCollection coll = _createCollection(name, path, language, null, null, 0.5D);
		coll.create();
		addCollection(coll, allowOverwrite);
		return coll;
	}

	// FUTURE add to interface
	public final SearchCollection createCollection(String name, Resource path, String language, String mode,
			String embedding, double ratio, boolean allowOverwrite) throws SearchException {
		SearchCollection coll = _createCollection(name, path, language, mode, embedding, ratio);
		coll.create();
		addCollection(coll, allowOverwrite);
		return coll;
	}

	/**
	 * Creates a new Collection, will be invoked by createCollection
	 * 
	 * @param name
	 *            The Name of the Collection
	 * @param path
	 *            the path to store
	 * @param language
	 *            The language of the collection
	 * @return New SearchCollection
	 * @throws SearchException
	 */
	protected abstract SearchCollection _createCollection(String name, Resource path, String language, String mode,
			String embedding, double ratio) throws SearchException;

	/**
	 * adds a new Collection to the storage
	 * 
	 * @param collection
	 * @param allowOverwrite
	 *            if allowOverwrite is false and a collection already exist -> throw
	 *            Exception
	 * @throws SearchException
	 * @throws IOException
	 */
	private final synchronized void addCollection(SearchCollection collection, boolean allowOverwrite)
			throws SearchException {
		SearchCollection o = collections.get(collection.getName());
		if (!allowOverwrite && o != null)
			throw new SearchException("there is already a collection with name " + collection.getName());
		collections.put(collection.getName().toLowerCase(), collection);
		// update
		if (o != null) {
			setAttributes(getCollectionStruct(collection.getName()), collection);
		}
		// create
		else {
			Array arrColl = engine.getCastUtil().toArray(root.get("collections", null), null);
			if (arrColl == null) {
				arrColl = engine.getCreationUtil().createArray();
				root.setEL("collections", arrColl);
			}
			arrColl.appendEL(toStruct(collection));
		}
		store(true);
	}

	/**
	 * removes a Collection from the storage
	 * 
	 * @param collection
	 *            Collection to remove
	 * @throws SearchException
	 */
	protected final synchronized void removeCollection(SearchCollection collection) throws SearchException {
		removeCollection(collection.getName());
		_removeCollection(collection);
	}

	public Struct removeIndexStruct(Struct sctColl, String id) {
		Array arrIdx = engine.getCastUtil().toArray(sctColl.get("indexes", null), null);
		if (id == null || arrIdx == null)
			return null;

		Struct sctIdx;
		Iterator<Entry<Key, Object>> it = arrIdx.entryIterator();
		Entry<Key, Object> e;
		Collection.Key key = null;
		while (it.hasNext()) {
			e = it.next();
			sctIdx = engine.getCastUtil().toStruct(e.getValue(), null);
			if (sctIdx != null) {
				if (id.equals(engine.getCastUtil().toString(sctIdx.get("id", null), null))) {
					key = e.getKey();
					break;
				}
			}
		}
		if (key != null) {
			arrIdx.remove(key, null);
		}
		return null;
	}

	/**
	 * removes a Collection from the storage
	 * 
	 * @param collection
	 *            Collection to remove
	 * @throws SearchException
	 */
	protected abstract void _removeCollection(SearchCollection collection) throws SearchException;

	/**
	 * removes a Collection from the storage
	 * 
	 * @param name
	 *            Name of the Collection to remove
	 * @throws SearchException
	 */
	protected final void removeCollection(String name) throws SearchException {
		try {
			// remove collection itself
			collections.remove(name.toLowerCase());

			Array arrColl = engine.getCastUtil().toArray(root.get("collections", null), null);
			if (name != null && arrColl != null) {

				Struct sctColl;
				Iterator<Entry<Key, Object>> it = arrColl.entryIterator();
				Entry<Key, Object> e;
				Collection.Key index = null;
				while (it.hasNext()) {
					e = it.next();
					sctColl = engine.getCastUtil().toStruct(e.getValue(), null);
					if (sctColl != null) {
						if (name.equalsIgnoreCase(engine.getCastUtil().toString(sctColl.get("name", null), null))) {
							index = e.getKey();
							break;
						}
					}
				}
				if (index != null) {
					arrColl.remove(index);
				}

			}
			store(true);
		} catch (PageException e) {
			throw new SearchException("can't remove collection " + name + ", collection doesn't exist");
		}
	}

	/**
	 * purge a Collection
	 * 
	 * @param collection
	 *            Collection to purge
	 * @throws SearchException
	 * @throws IOException
	 */
	protected final synchronized void purgeCollection(SearchCollection collection) throws SearchException {
		purgeCollection(collection.getName());
	}

	/**
	 * purge a Collection
	 * 
	 * @param name
	 *            Name of the Collection to purge
	 * @throws SearchException
	 * @throws IOException
	 */
	private final synchronized void purgeCollection(String name) throws SearchException {
		Struct sctColl = getCollectionStruct(name);
		sctColl.remove("indexes", null);
		store(true);
	}

	@Override
	public Resource getDirectory() {
		return searchDir;
	}

	/**
	 * return Struct matching collection name
	 * 
	 * @param name
	 * @return matching collection struct
	 */
	protected final Struct getCollectionStruct(String name) {
		Array arrColl = engine.getCastUtil().toArray(root.get("collections", null), null);
		if (name == null || arrColl == null)
			return null;

		Struct sctColl;
		Iterator<Object> it = arrColl.valueIterator();
		while (it.hasNext()) {
			sctColl = engine.getCastUtil().toStruct(it.next(), null);

			if (sctColl != null) {
				if (name.equalsIgnoreCase(engine.getCastUtil().toString(sctColl.get("name", null), null))) {
					return sctColl;
				}
			}
		}
		return null;
	}

	public Struct getIndexStruct(Struct sctColl, String id) {
		Array arrIdx = engine.getCastUtil().toArray(sctColl.get("indexes", null), null);
		if (id == null || arrIdx == null)
			return null;

		Struct sctIdx;
		Iterator<Object> it = arrIdx.valueIterator();
		while (it.hasNext()) {
			sctIdx = engine.getCastUtil().toStruct(it.next(), null);
			if (sctIdx != null) {
				if (id.equals(engine.getCastUtil().toString(sctIdx.get("id", null), null)))
					return sctIdx;
			}
		}
		return null;
	}

	/**
	 * translate a collection object to a struct
	 */
	private final Struct toStruct(SearchCollection coll) {
		Struct sctColl = engine.getCreationUtil().createStruct();
		setAttributes(sctColl, coll);
		return sctColl;
	}

	/**
	 * translate a index object to a struct
	 */
	protected final Struct toStruct(SearchIndex index) throws SearchException {
		Struct sctIdx = engine.getCreationUtil().createStruct();
		setAttributes(sctIdx, index);
		return sctIdx;
	}

	/**
	 * sets all attributes from Search Collection
	 * 
	 * @param el
	 * @param coll
	 */
	private final void setAttributes(Struct el, SearchCollection coll) {
		if (el == null)
			return;

		setAttribute(el, "language", coll.getLanguage());
		setAttribute(el, "name", coll.getName());

		if (coll instanceof SearchCollectionSupport) {
			if (!Util.isEmpty(((SearchCollectionSupport) coll).getMode())) {
				setAttribute(el, "mode", ((SearchCollectionSupport) coll).getMode());
			}
			if (!Util.isEmpty(((SearchCollectionSupport) coll).getEmbedding())) {
				setAttribute(el, "embedding", ((SearchCollectionSupport) coll).getEmbedding());
			}
			setAttribute(el, "ratio", CFMLEngineFactory.getInstance().getCastUtil()
					.toString(((SearchCollectionSupport) coll).getRatio(), "0.5"));

		}
		String value = coll.getLastUpdate().castToString(null);
		if (value != null)
			setAttribute(el, "lastUpdate", value);
		value = coll.getCreated().castToString(null);
		if (value != null)
			setAttribute(el, "created", value);

		setAttribute(el, "path", coll.getPath().getAbsolutePath());
	}

	/**
	 * sets all attributes in struct from Search Index
	 * 
	 * @param el
	 * @param index
	 * @throws SearchException
	 */
	protected final void setAttributes(Struct el, SearchIndex index) throws SearchException {
		if (el == null)
			return;
		setAttribute(el, "categoryTree", index.getCategoryTree());
		setAttribute(el, "category", engine.getListUtil().toList(index.getCategories(), ","));
		setAttribute(el, "custom1", index.getCustom1());
		setAttribute(el, "custom2", index.getCustom2());
		setAttribute(el, "custom3", index.getCustom3());
		setAttribute(el, "custom4", index.getCustom4());
		setAttribute(el, "id", index.getId());
		setAttribute(el, "key", index.getKey());
		setAttribute(el, "language", index.getLanguage());
		setAttribute(el, "title", index.getTitle());
		setAttribute(el, "extensions", engine.getListUtil().toList(index.getExtensions(), ","));
		setAttribute(el, "type", SearchIndexImpl.toStringType(index.getType()));
		setAttribute(el, "urlpath", index.getUrlpath());
		setAttribute(el, "query", index.getQuery());
	}

	/**
	 * helper method to set a attribute
	 * 
	 * @param el
	 * @param name
	 * @param value
	 */
	private void setAttribute(Struct el, String name, String value) {
		if (value != null) {
			el.setEL(name, value);
		}
	}

	/**
	 * read in a single Index
	 * 
	 * @param sc
	 * @param el
	 * @throws SearchException
	 * @throws PageException
	 */

	/**
	 * read in a existing collection
	 * 
	 * @param name
	 * @param parh
	 * @param language
	 * @param count
	 * @param lastUpdate
	 * @param created
	 * @return SearchCollection
	 * @throws SearchException
	 */
	protected abstract SearchCollection _readCollection(String name, Resource parh, String language,
			DateTime lastUpdate, DateTime created, String mode, String embedding, double ratio) throws SearchException;

	/**
	 * store loaded data to xml file
	 * 
	 * @throws SearchException
	 * @throws IOException
	 */
	protected final synchronized void store(boolean updateData) throws SearchException {
		if (updateData) {
			SearchCollection sc;
			for (Entry<String, SearchCollection> e : collections.entrySet()) {
				Struct sctColl = getCollectionStruct(e.getKey().toLowerCase());
				sc = e.getValue();
				setAttributes(sctColl, sc);
			}
		}
		try {
			String raw = engine.getCastUtil().fromStructToJsonString(root, false);
			engine.getIOUtil().write(searchFile, raw, false, CommonUtil.UTF8);

		} catch (Exception e) {
			throw CommonUtil.toSearchException(e);
		}
	}

	@Override
	public abstract String getDisplayName();

	public Config getConfig() {
		return config;
	}

}