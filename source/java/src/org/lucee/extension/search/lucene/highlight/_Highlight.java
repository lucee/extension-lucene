package org.lucee.extension.search.lucene.highlight;

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;

import lucee.loader.util.Util;

public class _Highlight {

	public static String createContextSummary(Object highlighter, Analyzer analyzer, String text, int maxNumFragments,
			String defaultValue) throws IOException, InvalidTokenOffsetsException {
		// try {
		if (!(highlighter instanceof Highlighter) || analyzer == null || Util.isEmpty(text))
			return defaultValue;

		TokenStream tokenStream = analyzer.tokenStream("", new StringReader(text));
		return ((Highlighter) highlighter).getBestFragments(tokenStream, text, maxNumFragments, "...");

	}

	public static Object createHighlighter(Query query, String highlightBegin, String highlightEnd) {

		return new Highlighter(
				// new SimpleHTMLFormatter("<span class=\"matching-term\">","</span>"),
				new SimpleHTMLFormatter(highlightBegin, highlightEnd), new QueryScorer(query));

	}

}
