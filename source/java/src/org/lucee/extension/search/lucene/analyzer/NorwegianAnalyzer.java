package org.lucee.extension.search.lucene.analyzer;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;


/**
 * <p>Analyzer for Norwegian language</p>
 * <p><a href="NorwegianAnalyzer.java.html"><i>View Source</i></a></p>
 * <p/>
 *
 * @author Andrey Grebnev <a href="mailto:andrey.grebnev@blandware.com">&lt;andrey.grebnev@blandware.com&gt;</a>
 * @version $Revision: 1.3 $ $Date: 2005/02/24 19:51:22 $
 */
public final class NorwegianAnalyzer extends Analyzer {

	public static final char SYMBOL_A_RING = "\u00e5".charAt(0);
	private static SnowballAnalyzer analyzer;

	private String NORWEGIAN_STOP_WORDS[] = {
		"og", "i", "er", "det", "som", ""+SYMBOL_A_RING, "til", "p"+SYMBOL_A_RING, "for", "av", "at", "med", "har", "en", "om", "du", "de",
		"ikke", "no", "vi", "jeg", "kan", "den", "eller", "seg", "men", "et", "dei", "skal", "ein", "blir", "s"+SYMBOL_A_RING,
		"vil", "fra", "var", "alle", "andre", "dette", "hva", SYMBOL_A_RING+"r", "bla"
	};

	/**
	 * Creates new instance of SpanishAnalyzer
	 */
	public NorwegianAnalyzer() {
		analyzer = new SnowballAnalyzer("Norwegian", NORWEGIAN_STOP_WORDS);
	}

	public NorwegianAnalyzer(String stopWords[]) {
		analyzer = new SnowballAnalyzer("Norwegian", stopWords);
	}

	@Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
		return analyzer.tokenStream(fieldName, reader);
	}
}