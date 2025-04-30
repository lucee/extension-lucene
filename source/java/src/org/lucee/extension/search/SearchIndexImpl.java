package org.lucee.extension.search;

import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.search.SearchException;
import lucee.runtime.search.SearchIndex;

/**
 */
public final class SearchIndexImpl implements SearchIndex {

	/**
	 * Field <code>TYPE_FILE</code>
	 */
	public static final short TYPE_FILE = 0;
	/**
	 * Field <code>TYPE_PATH</code>
	 */
	public static final short TYPE_PATH = 1;
	/**
	 * Field <code>TYPE_CUSTOM</code>
	 */
	public static final short TYPE_CUSTOM = 2;
	/**
	 * Field <code>TYPE_URL</code>
	 */
	public static final short TYPE_URL = 3;

	private String id;
	private String title;
	private String key;
	private short type;
	private String[] extensions;
	private String language;
	private String urlpath;
	private String custom1;
	private String custom2;
	private String query;
	private String custom3;
	private String custom4;
	private String categoryTree;
	private String[] categories;
	private final CFMLEngine engine;

	/**
	 * @param title
	 * @param id
	 * @param key
	 * @param type
	 * @param query
	 * @param extensions
	 * @param language
	 * @param urlpath
	 * @param custom1
	 * @param custom2
	 * @param custom3
	 * @param custom4
	 */
	protected SearchIndexImpl(String id, String title, String key, short type, String query, String[] extensions,
			String language, String urlpath, String categoryTree, String[] categories, String custom1, String custom2,
			String custom3, String custom4) {
		engine = CFMLEngineFactory.getInstance();
		this.title = title;
		this.id = id;
		this.key = key;
		this.type = type;
		this.query = query;
		this.extensions = extensions;
		this.language = SearchUtil.translateLanguage(language);
		this.urlpath = urlpath;
		this.categoryTree = categoryTree;
		this.categories = trim(categories);
		this.custom1 = custom1;
		this.custom2 = custom2;
		this.custom3 = custom3;
		this.custom4 = custom4;
	}

	private static String[] trim(String[] arr) {
		for (int i = 0; i < arr.length; i++) {
			arr[i] = arr[i].trim();
		}
		return arr;
	}

	/**
	 * @param title
	 * @param key
	 * @param type
	 * @param query
	 * @param extensions
	 * @param language
	 * @param urlpath
	 * @param custom1
	 * @param custom2
	 * @param custom3
	 * @param custom4
	 */
	protected SearchIndexImpl(String title, String key, short type, String query, String[] extensions, String language,
			String urlpath, String categoryTree, String[] categories, String custom1, String custom2, String custom3,
			String custom4) {
		engine = CFMLEngineFactory.getInstance();

		this.title = title;
		this.key = key;
		this.type = type;
		this.query = query;
		this.extensions = extensions;
		this.language = SearchUtil.translateLanguage(language);
		this.urlpath = urlpath;
		this.categoryTree = categoryTree;
		this.categories = categories;
		this.custom1 = custom1;
		this.custom2 = custom2;
		this.custom3 = custom3;
		this.custom4 = custom4;
		this.id = toId(type, key, query);
	}

	/**
	 * cast string type to short
	 * 
	 * @param type
	 *            type to cast
	 * @return casted type
	 * @throws SearchException
	 */
	public static short toType(String type) throws SearchException {
		type = type.toLowerCase().trim();
		if (type.equals("custom"))
			return SearchIndex.TYPE_CUSTOM;
		else if (type.equals("query"))
			return SearchIndex.TYPE_CUSTOM;
		else if (type.equals("file"))
			return SearchIndex.TYPE_FILE;
		else if (type.equals("path"))
			return SearchIndex.TYPE_PATH;
		else if (type.equals("url"))
			return SearchIndex.TYPE_URL;
		else
			throw new SearchException("invalid value for attribute type [" + type + "]");
	}

	/**
	 * cast short type to string
	 * 
	 * @param type
	 *            type to cast
	 * @return casted type
	 * @throws SearchException
	 */
	public static String toStringType(short type) throws SearchException {
		if (type == SearchIndex.TYPE_CUSTOM)
			return "custom";
		else if (type == SearchIndex.TYPE_FILE)
			return "file";
		else if (type == SearchIndex.TYPE_PATH)
			return "path";
		else if (type == SearchIndex.TYPE_URL)
			return "url";
		else
			throw new SearchException("invalid value for attribute type [" + type + "]");

	}

	/**
	 * cast short type to string
	 * 
	 * @param type
	 *            type to cast
	 * @return casted type
	 * @throws SearchException
	 */
	public static String toStringTypeEL(short type) {
		if (type == SearchIndex.TYPE_CUSTOM)
			return "custom";
		else if (type == SearchIndex.TYPE_FILE)
			return "file";
		else if (type == SearchIndex.TYPE_PATH)
			return "path";
		else if (type == SearchIndex.TYPE_URL)
			return "url";
		else
			return "custom";

	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof SearchIndex))
			return false;
		SearchIndex other = (SearchIndex) o;

		return (other.getKey().equals(key) && other.getType() == type);
	}

	/**
	 * @return Returns the custom1.
	 */
	@Override
	public String getCustom1() {
		return custom1;
	}

	/**
	 * @return Returns the custom2.
	 */
	@Override
	public String getCustom2() {
		return custom2;
	}

	/**
	 * @return Returns the custom3.
	 */
	@Override
	public String getCustom3() {
		return custom3;
	}

	/**
	 * @return Returns the custom4.
	 */
	@Override
	public String getCustom4() {
		return custom4;
	}

	/**
	 * @return Returns the extensions.
	 */
	@Override
	public String[] getExtensions() {
		return extensions;
	}

	/**
	 * @return Returns the key.
	 */
	@Override
	public String getKey() {
		return key;
	}

	/**
	 * @return Returns the language.
	 */
	@Override
	public String getLanguage() {
		return language;
	}

	/**
	 * @return Returns the title.
	 */
	@Override
	public String getTitle() {
		return title;
	}

	/**
	 * @return Returns the type.
	 */
	@Override
	public short getType() {
		return type;
	}

	/**
	 * @return Returns the id.
	 */
	@Override
	public String getId() {
		return id;
	}

	/**
	 * @param id
	 *            The id to set. / public void setId(String id) { this.id = id; }
	 */

	/**
	 * @return Returns the urlpath.
	 */
	@Override
	public String getUrlpath() {
		return urlpath;
	}

	/**
	 * @return Returns the query.
	 */
	@Override
	public String getQuery() {
		return query;
	}

	@Override
	public String toString() {
		return "lucee.runtime.search.SearchIndex(id:" + id + ";title:" + title + ";key:" + key + ";type:"
				+ toStringTypeEL(type) + ";language:" + language + ";urlpath:" + urlpath + ";query:" + query
				+ ";categoryTree:" + categoryTree + ";categories:" + engine.getListUtil().toList(categories, ",")
				+ ";custom1:" + custom1 + ";custom2:" + custom2 + ";custom3:" + custom3 + ";custom4:" + custom4 + ";)";
	}

	/**
	 * @param type
	 * @param key
	 * @param queryName
	 * @return id from given data
	 */
	public static String toId(short type, String key, String queryName) {
		if (type == SearchIndex.TYPE_CUSTOM)
			return "custom";
		CFMLEngine en = CFMLEngineFactory.getInstance();
		return toStringTypeEL(type) + "-" + en.getSystemUtil().hash64b(key + null);
		// null is for backward compatibility to older collections
	}

	/**
	 * @return the categories
	 */
	@Override
	public String[] getCategories() {
		return categories;
	}

	/**
	 * @return the categoryTree
	 */
	@Override
	public String getCategoryTree() {
		return categoryTree;
	}
}