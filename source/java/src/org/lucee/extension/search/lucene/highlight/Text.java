package org.lucee.extension.search.lucene.highlight;

public class Text {
	public final String text;
	public final int start;
	public final int end;

	public Text(String text, int start, int end) {
		this.text = text;
		this.start = start;
		this.end = end;
	}

	public int length() {
		return text.length();
	}

	public static Text merge(Text l, Text r) {
		return new Text(l.text + r.text, l.start, r.end);
	}
}
