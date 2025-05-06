package org.lucee.extension.search.lucene;

import org.lucee.extension.search.SearchDataImpl;
import org.lucee.extension.search.SearchEngineSupport;

import lucee.commons.io.res.Resource;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.search.SearchCollection;
import lucee.runtime.search.SearchData;
import lucee.runtime.search.SearchException;
import lucee.runtime.type.dt.DateTime;

/**
 * 
 */
public final class LuceneSearchEngine extends SearchEngineSupport {

	public LuceneSearchEngine() {
	}

	@Override
	public SearchCollection _createCollection(String name, Resource path, String language, String mode,
			String embedding, double ratio) throws SearchException {
		DateTime now = CFMLEngineFactory.getInstance().getCreationUtil().createDateTime(System.currentTimeMillis());
		return new LuceneSearchCollection(this, name, path, language, now, now, true, mode, embedding, ratio);
	}

	@Override
	public void _removeCollection(SearchCollection collection) throws SearchException {
		// throw new SearchException("Lucene Search Engine not implemeted");
	}

	@Override
	public SearchCollection _readCollection(String name, Resource path, String language, DateTime lastUpdate,
			DateTime created, String mode, String embedding, double ratio) throws SearchException {
		// throw new SearchException("Lucene Search Engine not implemeted");
		return new LuceneSearchCollection(this, name, path, language, lastUpdate, created, true, mode, embedding,
				ratio);
	}

	@Override
	public String getDisplayName() {
		return "Lucene Search Engine";
	}

	@Override
	public SearchData createSearchData(int suggestionMax) {
		return new SearchDataImpl(suggestionMax);
	}

}