package org.lucee.extension.search.lucene.highlight;

import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.TokenGroup;

public class TextFormatter implements Formatter {

	private Formatter formatter;
	private float score = 0f;

	public TextFormatter(Formatter formatter) {
		this.formatter = formatter;
	}

	@Override
	public String highlightTerm(String originalText, TokenGroup tokenGroup) {
		if (tokenGroup.getTotalScore() <= 0) {
			return originalText;
		}
		score += tokenGroup.getTotalScore();
		return formatter.highlightTerm(originalText, tokenGroup);
	}

	public void reset() {
		score = 0f;
	}

	public float getScore() {
		return score;
	}

}
