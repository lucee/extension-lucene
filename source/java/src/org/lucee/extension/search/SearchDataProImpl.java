package org.lucee.extension.search;

import lucee.runtime.search.SearchDataPro;

/**
 * Extended SearchData that implements SearchDataPro for Lucee 7.0.3.29+.
 * This class is only loaded when SearchDataPro exists in the classloader.
 */
public class SearchDataProImpl extends SearchDataImpl implements SearchDataPro {

	public SearchDataProImpl(int suggestionMax) {
		super(suggestionMax);
	}

}
