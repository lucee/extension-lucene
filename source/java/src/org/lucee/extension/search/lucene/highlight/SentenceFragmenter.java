package org.lucee.extension.search.lucene.highlight;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.search.highlight.Fragmenter;

public class SentenceFragmenter implements Fragmenter {

	private static final Pattern SENTENCE_END = Pattern.compile("[.!?]\\s+");
	private static final Pattern CODE_BLOCK = Pattern.compile("\\{.*?\\}", Pattern.DOTALL);
	private static final Pattern PARAGRAPH = Pattern.compile("\n\\s*\n");

	private final int maxBlockSize;
	private final int maxSentenceSize;

	private final List<String> fragments = new ArrayList<>();
	private int currentFragmentIndex = 0;
	private int currentFragmentOffset = 0;

	public SentenceFragmenter(int size) {
		this.maxBlockSize = size;
		this.maxSentenceSize = size;
	}

	public SentenceFragmenter(int maxBlockSize, int maxSentenceSize) {
		this.maxBlockSize = maxBlockSize;
		this.maxSentenceSize = maxSentenceSize;
	}

	@Override
	public void start(String originalText, TokenStream tokenStream) {
		fragments.clear();
		currentFragmentIndex = 0;
		currentFragmentOffset = 0;

		// Split text into code blocks or paragraphs
		Matcher codeBlockMatcher = CODE_BLOCK.matcher(originalText);
		int lastEnd = 0;

		while (codeBlockMatcher.find()) {
			// Add text before the code block as fragments
			String beforeCode = originalText.substring(lastEnd, codeBlockMatcher.start()).trim();
			if (!beforeCode.isEmpty()) {
				splitIntoBlocksOrSentences(beforeCode);
			}

			// Add the code block as a single fragment
			fragments.add(codeBlockMatcher.group());
			lastEnd = codeBlockMatcher.end();
		}

		// Add any remaining text after the last code block
		String remainingText = originalText.substring(lastEnd).trim();
		if (!remainingText.isEmpty()) {
			splitIntoBlocksOrSentences(remainingText);
		}
	}

	@Override
	public boolean isNewFragment() {
		// Check if the current token offset has moved past the current fragment
		if (currentFragmentIndex >= fragments.size()) {
			return false;
		}

		String currentFragment = fragments.get(currentFragmentIndex);
		if (currentFragmentOffset >= currentFragment.length()) {
			currentFragmentIndex++;
			currentFragmentOffset = 0;
			return true;
		}

		return false;
	}

	private void splitIntoBlocksOrSentences(String text) {
		// Split into paragraphs
		Matcher paragraphMatcher = PARAGRAPH.matcher(text);
		int lastEnd = 0;

		while (paragraphMatcher.find()) {
			String paragraph = text.substring(lastEnd, paragraphMatcher.start()).trim();
			if (!paragraph.isEmpty()) {
				addParagraphOrSentences(paragraph);
			}
			lastEnd = paragraphMatcher.end();
		}

		// Add remaining text as a paragraph or sentences
		String remainingText = text.substring(lastEnd).trim();
		if (!remainingText.isEmpty()) {
			addParagraphOrSentences(remainingText);
		}
	}

	private void addParagraphOrSentences(String paragraph) {
		if (paragraph.length() > maxBlockSize) {
			// Split paragraph into sentences if it's too large
			Matcher sentenceMatcher = SENTENCE_END.matcher(paragraph);
			int lastEnd = 0;

			while (sentenceMatcher.find()) {
				String sentence = paragraph.substring(lastEnd, sentenceMatcher.end()).trim();
				if (!sentence.isEmpty()) {
					fragments.add(sentence);
				}
				lastEnd = sentenceMatcher.end();
			}

			// Add remaining text as a sentence
			String remainingSentence = paragraph.substring(lastEnd).trim();
			if (!remainingSentence.isEmpty()) {
				fragments.add(remainingSentence);
			}
		} else {
			// Add the paragraph as a single fragment
			fragments.add(paragraph);
		}
	}
}
