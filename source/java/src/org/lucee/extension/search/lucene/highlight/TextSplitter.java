package org.lucee.extension.search.lucene.highlight;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextSplitter {
	private static final Pattern SENTENCE_PATTERN = Pattern.compile("[.!?]");

	private int maxSize = 500;

	public TextSplitter() {
	}

	public TextSplitter(int maxSize) {
		this.maxSize = maxSize;
	}

	public List<Text> split(String text) {

		int tol = (int) (maxSize * 0.2D);
		int maxLength = maxSize + tol;
		int minLength = maxSize - tol;

		if (text == null || text.isEmpty()) {
			return new ArrayList<>();
		}

		List<Text> positions = new ArrayList<>();

		Matcher matcher = SENTENCE_PATTERN.matcher(text);

		int startPosition = 0;
		Text last = null, sen;
		while (matcher.find()) {
			// The end position is where the terminator starts
			int endPosition = matcher.start();
			sen = new Text(text.substring(startPosition, endPosition + 1), startPosition, endPosition + 1);
			// no last, just set it
			if (last == null) {
				last = sen;
			}
			// last already has the necessary size
			else if (last.length() >= maxSize) {
				positions.add(last);
				last = sen;
			}
			// last + current has the necessary size
			else if ((last.length() + sen.length()) >= maxSize) {
				positions.add(Text.merge(last, sen));
				last = null;
			}
			// still to short keep it
			else {
				last = Text.merge(last, sen);
			}

			// Start of next sentence is after the newline
			startPosition = matcher.end();
		}

		// Add the last part if there's remaining text
		if (startPosition < text.length()) {
			if (last == null) {
				last = new Text(text.substring(startPosition), startPosition, text.length() - 1);
			} else {
				last = new Text(last.text + text.substring(startPosition), last.start, text.length() - 1);
			}
		}
		if (last != null) {
			if (last.length() < minLength) {
				Text tmp = positions.get(positions.size() - 1);
				Text both = Text.merge(tmp, last);
				if (both.length() > maxLength) {
					positions.add(last);
				} else {
					positions.set(positions.size() - 1, both);
				}
			} else {
				positions.add(last);
			}
		}
		return positions;
	}

	public int sentenceTerminatorIndex(String text, int fromIndex) {
		// Regex pattern: [.!?] matches any sentence terminator
		// \s* matches zero or more whitespace characters (spaces, tabs)
		// \n matches a newline
		String pattern = "[.!?]\\s*\\n";
		Pattern regex = Pattern.compile(pattern);
		Matcher matcher = regex.matcher(text);

		// Start searching from fromIndex
		matcher.region(fromIndex, text.length());

		// Find the next match
		if (matcher.find()) {
			// Return the index of the sentence terminator (not including the
			// spaces/newline)
			return matcher.start();
		}

		return -1;
	}

	public int sentenceTerminatorIndexOld(String text, int fromIndex, boolean paragraph) {
		int index = Integer.MAX_VALUE;

		int tmp = text.indexOf('.', fromIndex);
		if (tmp != -1 && tmp < index)
			index = tmp;

		tmp = text.indexOf('?', fromIndex);
		if (tmp != -1 && tmp < index)
			index = tmp;

		tmp = text.indexOf('!', fromIndex);
		if (tmp != -1 && tmp < index)
			index = tmp;

		if (index == Integer.MAX_VALUE)
			return -1;
		return index;
	}

	public static void main(String[] args) {
		// Sample text with paragraphs
		final String text = "The red fox is one of nature's most adaptable creatures. In the dense forest, "
				+ "a clever fox makes its home beneath an old oak tree! Every evening, this fox emerges "
				+ "to hunt for mice and rabbits. The local farmers often spot the fox near their chicken coops, "
				+ "though this particular fox seems more interested in the field mice. Urban areas have their own "
				+ "fox populations too. A mother fox was recently seen raising her cubs in the city park.\n\n "
				+ "Scientists studying fox behavior have noted that urban foxes are becoming increasingly comfortable "
				+ "around humans. The fox's distinctive red coat and bushy tail make it easily recognizable. "
				+ "In winter, the fox's coat becomes thicker and even more luxurious. Young fox cubs begin learning "
				+ "to hunt at about three months old. The mother fox teaches them essential hunting skills.\n\n "
				+ "Arctic fox species are different from red foxes, changing their coat color with the seasons. "
				+ "But our local red fox maintains its rusty color throughout the year. The fox's keen sense of smell "
				+ "and excellent hearing make it a superb hunter. Sometimes, a fox can be seen playing with its prey "
				+ "before making the final catch. The playful nature of the fox has been documented in many studies.";

		System.out.println(text.length());
		TextSplitter ps = new TextSplitter(500);
		for (Text s : ps.split(text)) {
			System.out.println("---- " + s.length() + " ----");
			System.out.println(s.text);
		}
		System.out.println(text.length());

	}
}
