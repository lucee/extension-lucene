package org.lucee.extension.search.lucene;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.br.BrazilianAnalyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.cz.CzechAnalyzer;
import org.apache.lucene.analysis.da.DanishAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.el.GreekAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.fi.FinnishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.hu.HungarianAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.apache.lucene.analysis.no.NorwegianAnalyzer;
import org.apache.lucene.analysis.pt.PortugueseAnalyzer;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.sv.SwedishAnalyzer;
import org.apache.lucene.analysis.th.ThaiAnalyzer;
import org.apache.lucene.analysis.tr.TurkishAnalyzer;

import lucee.runtime.search.SearchException;

public final class SearchUtil {

	private static Map<String, Analyzer> analyzers = new ConcurrentHashMap<>();

	public static Analyzer getAnalyzer(String language) throws SearchException {

		if (language == null)
			language = "english";
		else
			language = language.toLowerCase().trim();
		language = org.lucee.extension.search.SearchUtil.translateLanguage(language);

		Analyzer analyzer = analyzers.get(language);
		if (analyzer != null)
			return analyzer;
		StandardTokenizer st = null;

		if (language.equals("english"))
			analyzer = new StandardAnalyzer();
		else if (language.equals("german"))
			analyzer = new GermanAnalyzer();
		else if (language.equals("russian"))
			analyzer = new RussianAnalyzer();
		else if (language.equals("dutch"))
			analyzer = new DutchAnalyzer();
		else if (language.equals("french"))
			analyzer = new FrenchAnalyzer();
		else if (language.equals("norwegian"))
			analyzer = new NorwegianAnalyzer();
		else if (language.equals("portuguese"))
			analyzer = new PortugueseAnalyzer();
		else if (language.equals("spanish"))
			analyzer = new SpanishAnalyzer();
		else if (language.equals("brazilian"))
			analyzer = new BrazilianAnalyzer();
		else if (language.startsWith("czech"))
			analyzer = new CzechAnalyzer();
		else if (language.equals("greek"))
			analyzer = new GreekAnalyzer();
		else if (language.equals("thai"))
			analyzer = new ThaiAnalyzer();
		else if (language.equals("japanese"))
			analyzer = new CJKAnalyzer();
		else if (language.equals("korean"))
			analyzer = new CJKAnalyzer();
		else if (language.equals("italian"))
			analyzer = new ItalianAnalyzer();
		else if (language.equals("danish"))
			analyzer = new DanishAnalyzer();
		else if (language.equals("norwegian"))
			analyzer = new NorwegianAnalyzer();
		else if (language.equals("finnish"))
			analyzer = new FinnishAnalyzer();
		else if (language.equals("swedish"))
			analyzer = new SwedishAnalyzer();
		else if (language.equals("hungarian"))
			analyzer = new HungarianAnalyzer();
		else if (language.equals("turkish"))
			analyzer = new TurkishAnalyzer();

		else {// TODO add more
			throw new SearchException("No Language Analyzer for Lanuage " + language + " found");
		}
		analyzers.put(language, analyzer);
		return analyzer;
	}
}
