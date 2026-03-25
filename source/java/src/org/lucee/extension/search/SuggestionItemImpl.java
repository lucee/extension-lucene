package org.lucee.extension.search;

import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.search.SuggestionItem;
import lucee.runtime.type.Array;
import lucee.runtime.util.Creation;

public class SuggestionItemImpl implements SuggestionItem {

	final Array keywords;
	final Array keywordsScore;

	public SuggestionItemImpl(String[] arr, double[] scores) {
		Creation c = CFMLEngineFactory.getInstance().getCreationUtil();
		keywords=c.createArray();
		keywordsScore=c.createArray();

		add(arr, scores);
	}

	public void add(String[] arr, double[] scores) {
		for(int i=0;i<arr.length;i++) {
			keywords.appendEL(arr[i]);
			keywordsScore.appendEL(Double.valueOf(Math.round(scores[i] * 100)));
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
