package org.lucee.extension.search.lucene.highlight;

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;

import lucee.loader.util.Util;

public class Highlight {

	public static String createContextSummary(Highlighter highlighter, Analyzer analyzer, String text,
			int maxNumFragments, int maxLength, String defaultValue) throws IOException, InvalidTokenOffsetsException {
		if (maxNumFragments <= 0 || analyzer == null || Util.isEmpty(text))
			return defaultValue;
		System.err.println("ooooooooooooooooooooooooooooo");
		System.err.println("maxNumFragments:" + maxNumFragments);
		String res = defaultValue;
		try {

			TokenStream tokenStream = analyzer.tokenStream("contents", new StringReader(text));
			res = highlighter.getBestFragments(tokenStream, text, maxNumFragments, "...\n").trim();
			System.err.println(res);

		} catch (Throwable t) {
			if (t instanceof ThreadDeath)
				throw (ThreadDeath) t;
		}

		/*
		 * System.err.println("sssssssssssssssssssssssssssssssss"); String[] fragments =
		 * highlighter.getBestFragments(analyzer, "contents", text, maxNumFragments);
		 * for (String f : fragments) { System.err.println(f);
		 * System.err.println("------------------------------------"); }
		 */
		return res;
	}

	public static Highlighter createHighlighter(Query query, String highlightBegin, String highlightEnd,
			int fragmentSize) {
		QueryScorer scorer = new QueryScorer(query);
		Highlighter highlighter = new Highlighter(new SimpleHTMLFormatter(highlightBegin, highlightEnd), scorer);
		// Highlighter highlighter = new Highlighter(new FormatterImpl("b"), scorer);

		highlighter.setTextFragmenter(new SimpleFragmenter(fragmentSize));
		// highlighter.setTextFragmenter(new SimpleSpanFragmenter(scorer,
		// fragmentSize));
		// highlighter.setTextFragmenter(new SentenceFragmenter(fragmentSize));

		return highlighter;
	}

}
