package org.lucee.extension.search.lucene;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.br.BrazilianAnalyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.cn.ChineseAnalyzer;
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
import org.lucee.extension.search.lucene.util.CommonUtil;

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
			analyzer = new StandardAnalyzer(CommonUtil.VERSION);
		else if (language.equals("german"))
			analyzer = new GermanAnalyzer(CommonUtil.VERSION);
		else if (language.equals("russian"))
			analyzer = new RussianAnalyzer(CommonUtil.VERSION);
		else if (language.equals("dutch"))
			analyzer = new DutchAnalyzer(CommonUtil.VERSION);
		else if (language.equals("french"))
			analyzer = new FrenchAnalyzer(CommonUtil.VERSION);
		else if (language.equals("norwegian"))
			analyzer = new NorwegianAnalyzer(CommonUtil.VERSION);
		else if (language.equals("portuguese"))
			analyzer = new PortugueseAnalyzer(CommonUtil.VERSION);
		else if (language.equals("spanish"))
			analyzer = new SpanishAnalyzer(CommonUtil.VERSION);
		else if (language.equals("brazilian"))
			analyzer = new BrazilianAnalyzer(CommonUtil.VERSION);
		else if (language.equals("chinese"))
			analyzer = new ChineseAnalyzer();
		else if (language.startsWith("czech"))
			analyzer = new CzechAnalyzer(CommonUtil.VERSION);
		else if (language.equals("greek"))
			analyzer = new GreekAnalyzer(CommonUtil.VERSION);
		else if (language.equals("thai"))
			analyzer = new ThaiAnalyzer(CommonUtil.VERSION);
		else if (language.equals("japanese"))
			analyzer = new CJKAnalyzer(CommonUtil.VERSION);
		else if (language.equals("korean"))
			analyzer = new CJKAnalyzer(CommonUtil.VERSION);
		else if (language.equals("italian"))
			analyzer = new ItalianAnalyzer(CommonUtil.VERSION);
		else if (language.equals("danish"))
			analyzer = new DanishAnalyzer(CommonUtil.VERSION);
		else if (language.equals("norwegian"))
			analyzer = new NorwegianAnalyzer(CommonUtil.VERSION);
		else if (language.equals("finnish"))
			analyzer = new FinnishAnalyzer(CommonUtil.VERSION);
		else if (language.equals("swedish"))
			analyzer = new SwedishAnalyzer(CommonUtil.VERSION);
		else if (language.equals("hungarian"))
			analyzer = new HungarianAnalyzer(CommonUtil.VERSION);
		else if (language.equals("turkish"))
			analyzer = new TurkishAnalyzer(CommonUtil.VERSION);

		else {// TODO add more
			throw new SearchException("No Language Analyzer for Lanuage " + language + " found");
		}
		analyzers.put(language, analyzer);
		return analyzer;
	}
}
