package org.lucee.extension.search.lucene.highlight;

import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.TokenGroup;

public class HTMLFormatterWithScore implements Formatter {

	public static final String DEFAULT_PRE_TAG = "<B>";
	public static final String DEFAULT_POST_TAG = "</B>";

	private String preTag;
	private String postTag;
	private String tagName;

	public HTMLFormatterWithScore(String preTag, String postTag) {
		this.preTag = preTag;
		this.postTag = postTag;

		if (preTag.startsWith("<") && preTag.endsWith(">") && postTag.startsWith("</") && postTag.endsWith(">")) {
			String tmpPre = preTag.substring(1, preTag.length() - 1);
			String tmpPost = postTag.substring(2, postTag.length() - 1);
			if (tmpPre.equalsIgnoreCase(tmpPost) && tmpPost.length() > 0) {
				this.tagName = tmpPost;
			}

		}

	}

	/** Default constructor uses HTML: &lt;B&gt; tags to markup terms. */
	public HTMLFormatterWithScore() {
		this(DEFAULT_PRE_TAG, DEFAULT_POST_TAG);
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
		if (tokenGroup.getTotalScore() <= 0) {
			return originalText;
		}

		if (tagName != null) {
			StringBuilder returnBuffer = new StringBuilder(originalText.length() + (tagName.length() * 2) + 14);
			returnBuffer.append("<").append(tagName).append(" score=\"" + tokenGroup.getTotalScore() + "\">");
			returnBuffer.append(originalText);
			returnBuffer.append("</").append(tagName).append(">");
			return returnBuffer.toString();
		}

		// Allocate StringBuilder with the right number of characters from the
		// beginning, to avoid char[] allocations in the middle of appends.
		StringBuilder returnBuffer = new StringBuilder(preTag.length() + originalText.length() + postTag.length());
		returnBuffer.append(preTag);
		returnBuffer.append(originalText);
		returnBuffer.append(postTag);
		return returnBuffer.toString();
	}
}
