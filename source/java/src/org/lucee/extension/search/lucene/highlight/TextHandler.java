package org.lucee.extension.search.lucene.highlight;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.NullFragmenter;
import org.apache.lucene.search.highlight.QueryScorer;

public class TextHandler {

	public static List<ScoredParagraph> findBestTexts(Query query, Formatter formatter, String text, int max, int size)
			throws IOException, InvalidTokenOffsetsException {
		StandardAnalyzer analyzer = new StandardAnalyzer();
		TextFormatter paragraphFormatter = new TextFormatter(formatter);
		// Create highlighter
		QueryScorer scorer = new QueryScorer(query);

		Highlighter highlighter = new Highlighter(paragraphFormatter, scorer);

		// Important: Set fragmenter with size larger than any paragraph
		highlighter.setTextFragmenter(new NullFragmenter());

		// Split text into paragraphs
		TextSplitter splitter = new TextSplitter(size);
		List<Text> texts = splitter.split(text);

		// Create a priority queue to store the best matching paragraphs
		PriorityQueue<ScoredParagraph> bestParagraphs = new PriorityQueue<>();
		// Score each paragraph

		for (int i = 0; i < texts.size(); i++) {
			paragraphFormatter.reset();
			// Get highlighted text and score for this paragraph
			Text original = texts.get(i);
			String highlighted = highlighter.getBestFragment(analyzer, "content", original.text);
			if (highlighted == null)
				highlighted = original.text;
			if (highlighted != null) { // if there was a match
				bestParagraphs.add(new ScoredParagraph(original, highlighted, paragraphFormatter.getScore(), i));
			}
		}

		// Print the best matching paragraphs
		List<ScoredParagraph> list = new ArrayList<>();
		for (int i = 0; i < max && !bestParagraphs.isEmpty(); i++) {
			ScoredParagraph sp = bestParagraphs.poll();
			// if (sp.score == 0)break;
			list.add(sp);
		}

		// sort list
		Collections.sort(list, Comparator.comparingInt(sp -> sp.originalIndex));

		return list;
	}

	// Helper class to store paragraph information
	public static class ScoredParagraph implements Comparable<ScoredParagraph> {
		public final Text original;
		public final String highlighted;
		public final float score;
		public final int originalIndex;

		public ScoredParagraph(Text original, String highlighted, float score, int originalIndex) {
			this.original = original;
			this.highlighted = highlighted;
			this.score = score;
			this.originalIndex = originalIndex;
		}

		@Override
		public int compareTo(ScoredParagraph other) {
			return Float.compare(other.score, this.score); // Higher scores first
		}
	}
}
