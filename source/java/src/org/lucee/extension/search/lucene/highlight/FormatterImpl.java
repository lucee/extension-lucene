package org.lucee.extension.search.lucene.highlight;

import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.TokenGroup;

/**
 * Simple {@link Formatter} implementation to highlight terms with a pre and
 * post tag.
 */
public class FormatterImpl implements Formatter {

	private String tagName;

	public FormatterImpl(String tagName) {
		this.tagName = tagName;
	}

	/** Default constructor uses HTML: &lt;B&gt; tags to markup terms. */
	public FormatterImpl() {
		this("b");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.lucene.search.highlight.Formatter#highlightTerm(java.lang.String,
	 * org.apache.lucene.search.highlight.TokenGroup)
	 */
	@Override
	public String highlightTerm(String originalText, TokenGroup tokenGroup) {
		float score = tokenGroup.getTotalScore();
		if (score <= 0) {
			return originalText;
		}

		// Allocate StringBuilder with the right number of characters from the
		// beginning, to avoid char[] allocations in the middle of appends.
		StringBuilder returnBuffer = new StringBuilder(originalText.length() + (tagName.length() * 2) + 14);
		returnBuffer.append('<').append(tagName).append(" score=\"").append(score).append("\">");
		returnBuffer.append(originalText);
		returnBuffer.append("</").append(tagName).append('>');
		return returnBuffer.toString();
	}
}
