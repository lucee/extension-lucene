package org.lucee.extension.search.lucene.query;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.lucee.extension.search.lucene.util.CommonUtil;

/**
 * @deprecated no longer in use The simple query is the default query type and
 *             is appropriate for the vast majority of searches. When entering
 *             text on a search form, you perform a simple query by entering a
 *             word or comma-delimited strings, with optional wildcard
 *             characters. Verity treats each comma as a logical OR. If you omit
 *             the commas, Verity treats the expression as a phrase.
 */
@Deprecated
public final class Simple {
	private String OR = "or";
	private String AND = "and";
	private String NOT = "not";
	private char QUOTER = '"';
	private String FIELD = "contents";

	private static final short TYPE_TERMAL = 0;
	private static final short TYPE_WILDCARD = 1;
	private static final short TYPE_PREFIX = 2;
	private static final short TYPE_FUZZY = 3;
	private static final short TYPE_PHRASE = 4;

	private Analyzer analyzer;

	private Map results = new WeakHashMap();

	/**
	 * constructor of the class
	 * 
	 * @param analyzer
	 */
	public Simple(Analyzer analyzer) {
		this.analyzer = analyzer;

	}

	/**
	 * parse given string query
	 * 
	 * @param criteria
	 * @return matching Query
	 * @throws IOException
	 */
	public Query parse(String criteria) throws IOException {
		Query qry = (Query) results.get(criteria);
		if (qry != null)
			return qry;

		// remove operators at start
		if (criteria.length() > 0) {
			char first = criteria.charAt(0);
			// start with operator
			while (first == '*' || first == '~' || first == '?') {
				criteria = criteria.substring(1);
				if (criteria.length() == 0)
					break;
				first = criteria.charAt(0);
			}
		}

		// make never foud query if quey is empty
		if (criteria.length() == 0) {
			BooleanQuery.Builder bool = new BooleanQuery.Builder();
			bool.add(new TermQuery(new Term(FIELD, "dshnuiaslfspfhsadhfisd")), BooleanClause.Occur.MUST_NOT);
			BooleanQuery query = bool.build();
			results.put(criteria, query);
			return query;
		}

		ParserString ps = new ParserString(criteria);
		qry = orOp(ps);
		results.put(criteria, qry);
		return qry;
	}

	private Query orOp(ParserString ps) throws IOException {
		Query query = andOp(ps);
		ps.removeSpace();

		// OR
		while (ps.isValidIndex() && ps.forwardIfCurrent(OR) || ps.forwardIfCurrent(',')) {
			ps.removeSpace();
			BooleanQuery.Builder bool = new BooleanQuery.Builder();

			bool.add(query, BooleanClause.Occur.SHOULD);
			bool.add(andOp(ps), BooleanClause.Occur.SHOULD);
			query = bool.build();
		}
		return query;
	}

	private Query andOp(ParserString ps) throws IOException {
		Query query = notOp(ps);
		ps.removeSpace();

		// AND
		while (ps.isValidIndex() && ps.forwardIfCurrent(AND)) {
			ps.removeSpace();
			BooleanQuery.Builder bool = new BooleanQuery.Builder();

			bool.add(query, BooleanClause.Occur.MUST);
			bool.add(notOp(ps), BooleanClause.Occur.MUST);
			query = bool.build();
		}
		return query;
	}

	private Query notOp(ParserString ps) throws IOException {
		// NOT
		if (ps.isValidIndex() && ps.forwardIfCurrent(NOT)) {
			ps.removeSpace();
			BooleanQuery.Builder bool = new BooleanQuery.Builder();
			bool.add(clip(ps), BooleanClause.Occur.MUST_NOT);
			return bool.build();
		}
		return clip(ps);
	}

	private Query clip(ParserString ps) throws IOException {
		// ()
		if (ps.isValidIndex() && ps.forwardIfCurrent('(')) {
			Query query = orOp(ps);
			ps.removeSpace();
			ps.forwardIfCurrent(')');
			ps.removeSpace();
			return query;
		}
		return literal(ps);
	}

	private Query literal(ParserString ps) throws IOException {
		_Term term = term(ps);
		ps.removeSpace();
		while (ps.isValidIndex() && !ps.isCurrent(',') && !ps.isCurrent(OR) && !ps.isCurrent(AND)
				&& !ps.isCurrent(')')) {
			term.append(term(ps));
			ps.removeSpace();
		}
		return term.toQuery();
	}

	private _Term term(ParserString ps) {
		short type = TYPE_TERMAL;
		ps.removeSpace();
		StringBuffer sb = new StringBuffer();
		boolean inside = false;
		char c = 0;
		while (ps.isValidIndex() && ((c = ps.getCurrentLower()) != ' ' && c != ',' && c != ')' || inside)) {
			ps.next();
			if (c == QUOTER) {
				inside = !inside;
				type = TYPE_PHRASE;
				continue;
			}
			sb.append(c);
			if (!inside) {
				if (type == TYPE_PREFIX)
					type = TYPE_WILDCARD;
				if (type == TYPE_TERMAL && c == '*')
					type = TYPE_PREFIX;
				if (c == '?')
					type = TYPE_WILDCARD;
				if (type == TYPE_TERMAL && c == '~') {
					type = TYPE_FUZZY;
					break;
				}
			}
		}
		return new _Term(type, sb.toString());

	}

	class _Term {
		private short type;
		private String content;

		private _Term(short type, String content) {
			this.type = type;
			this.content = content;
		}

		private void append(_Term term) {
			content += ' ' + term.content;
			type = TYPE_PHRASE;
		}

		private Query toQuery() throws IOException {
			if (type == TYPE_FUZZY)
				return toFuzzyQuery();
			else if (type == TYPE_WILDCARD)
				return new WildcardQuery(toTerm());
			else if (type == TYPE_PREFIX)
				return toPrefixQuery();
			else if (type == TYPE_PHRASE)
				return toPhraseQuery();
			return new TermQuery(toTerm());
		}

		private FuzzyQuery toFuzzyQuery() {
			String c = toContent();
			return new FuzzyQuery(new Term(FIELD, c.substring(0, c.length() - 1)));
		}

		private PrefixQuery toPrefixQuery() {
			String c = toContent();
			return new PrefixQuery(new Term(FIELD, c.substring(0, c.length() - 1)));
		}

		private PhraseQuery toPhraseQuery() throws IOException {
			TokenStream source = analyzer.tokenStream(FIELD, new StringReader(content));
			List<String> terms = new ArrayList<>();

			try {
				// Reset the stream - required in Lucene 3.x
				source.reset();

				// Use AttributeSource instead of Token
				CharTermAttribute termAtt = source.addAttribute(CharTermAttribute.class);

				// Collect terms
				while (source.incrementToken()) {
					terms.add(termAtt.toString());
				}

				// End the stream
				source.end();

			} finally {
				CommonUtil.closeSilently(source);
				try {
					source.close();
				} catch (IOException e) {
					// ignore
				}
			}

			// Create phrase query
			PhraseQuery.Builder builder = new PhraseQuery.Builder();
			builder.setSlop(0); // Set the slop

			for (String term : terms) {
				builder.add(new Term(FIELD, term)); // Add each term
			}

			return builder.build();
		}

		private String toContent() {
			return content;
		}

		private Term toTerm() {
			return new Term(FIELD, toContent());
		}

		@Override
		public String toString() {
			return toContent();
		}
	}
}