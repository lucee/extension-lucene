package org.lucee.extension.search.lucene.query;



public final class Literal implements Op {

	// Lucene special characters that need escaping in user input
	private static final String LUCENE_SPECIAL = "+-&&||!(){}[]^\"~:\\/";

	String literal;
	String modifier = "";
	boolean quoted = false;
	boolean hasSuggestion = false;

	public Literal(String literal) {
		this.literal=literal;
	}

	public Literal(String literal, boolean quoted) {
		this.literal=literal;
		this.quoted=quoted;
	}

	@Override
	public String toString() {
		if (quoted)
			return modifier + "\"" + literal + "\"";
		if (hasSuggestion)
			return modifier + literal;
		return modifier + escapeLuceneSpecialChars(literal);
	}

	public void setSuggestion(String suggestion) {
		this.literal = "<suggestion>" + suggestion + "</suggestion>";
		this.hasSuggestion = true;
	}

	public void setModifier(String modifier) {
		this.modifier=modifier;
	}

	/**
	 * Escape Lucene special characters in user input so they don't
	 * break the QueryParser. Preserves * and ? for wildcards when
	 * they appear mid-word or trailing (e.g. test*, te?t) but escapes
	 * them when standalone or leading.
	 */
	private static String escapeLuceneSpecialChars(String input) {
		if (input == null || input.isEmpty()) return input;

		StringBuilder sb = new StringBuilder(input.length() + 4);
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (c == '*' || c == '?') {
				// preserve wildcards that are part of a word (e.g. test*, te?t)
				// escape standalone or leading wildcards
				if (i == 0) {
					sb.append('\\');
				}
				sb.append(c);
			} else if (LUCENE_SPECIAL.indexOf(c) >= 0) {
				sb.append('\\');
				sb.append(c);
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}
}
