package org.lucee.extension.search;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import lucee.runtime.search.SearchData;

// FUTURE diese klasse entfernen, dient nur daszu die suggestion durchzuschleusen
public class SearchDataImpl implements SearchData {

	private Map suggestion = null;
	private int suggestionMax;
	private String suggestionQuery;
	private int recordsSearched;
	private HashMap<String, Object> attrs;

	public SearchDataImpl(int suggestionMax) {
		this(new HashMap(), suggestionMax);
	}

	public SearchDataImpl(Map suggestion, int suggestionMax) {
		this.suggestion = suggestion;
		this.suggestionMax = suggestionMax;
	}

	@Override
	public Map getSuggestion() {
		return suggestion;
	}

	@Override
	public int getSuggestionMax() {
		return suggestionMax;
	}

	@Override
	public void setSuggestionQuery(String suggestionQuery) {
		this.suggestionQuery = suggestionQuery;
	}

	/**
	 * @return the suggestionQuery
	 */
	@Override
	public String getSuggestionQuery() {
		return suggestionQuery;
	}

	@Override
	public int addRecordsSearched(int count) {
		recordsSearched += count;
		return recordsSearched;
	}

	@Override
	public int getRecordsSearched() {
		return recordsSearched;
	}

	public void setAddionalAttribute(String name, Object value) {
		if (attrs == null)
			attrs = new HashMap<String, Object>();
		attrs.put(name, value);
	}

	public Map<String, Object> getAddionalAttributes() {
		return attrs;
	}

	public void dumpAddionalAttributes() {
		if (attrs != null) {
			System.err.println("--- addional attributes ---");
			for (Entry<String, Object> e : attrs.entrySet()) {
				System.err.println("- " + e.getKey() + ":" + e.getValue());
			}
		}
	}
}