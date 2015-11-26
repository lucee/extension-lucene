package org.lucee.extension.search;

import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.search.SuggestionItem;
import lucee.runtime.type.Array;
import lucee.runtime.util.Creation;

public class SuggestionItemImpl implements SuggestionItem {

	final Array keywords;
	final Array keywordsScore;
	
	public SuggestionItemImpl(String[] arr) {
		Creation c = CFMLEngineFactory.getInstance().getCreationUtil();
		keywords=c.createArray();
		keywordsScore=c.createArray();
		
		
		add(arr);
	}

	public void add(String[] arr) {
		for(int i=0;i<arr.length;i++) {
			keywords.appendEL(arr[i]);
			keywordsScore.appendEL(new Double(99-i));
		}
	}
	
	@Override
	public Array getKeywords() {
		return keywords;
	}

	@Override
	public Array getKeywordScore() {
		return keywordsScore;
	}
}
