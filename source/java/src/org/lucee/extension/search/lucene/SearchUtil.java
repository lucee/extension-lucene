package org.lucee.extension.search.lucene;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ar.ArabicAnalyzer;
import org.apache.lucene.analysis.bg.BulgarianAnalyzer;
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
import org.apache.lucene.analysis.sv.SwedishAnalyzer;
import org.apache.lucene.analysis.th.ThaiAnalyzer;
import org.apache.lucene.analysis.tr.TurkishAnalyzer;
import org.apache.lucene.util.Version;

import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
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

		Version version = Version.LUCENE_4_10_4;

		try {
			switch (language) {
			case "arabic":
				analyzer = new ArabicAnalyzer();
				break;
			case "brazilian":
				analyzer = new BrazilianAnalyzer();
				break;
			case "bulgarian":
				analyzer = new BulgarianAnalyzer();
				break;
			case "chinese":
				analyzer = new ChineseAnalyzer();
				break;
			case "czech":
				analyzer = new CzechAnalyzer();
				break;
			case "danish":
				analyzer = new DanishAnalyzer();
				break;
			case "dutch":
				analyzer = new DutchAnalyzer();
				break;
			case "english":
				analyzer = new StandardAnalyzer();
				break;
			case "finnish":
				analyzer = new FinnishAnalyzer();
				break;
			case "french":
				analyzer = new FrenchAnalyzer();
				break;
			case "german":
				analyzer = new GermanAnalyzer();
				break;
			case "greek":
				analyzer = new GreekAnalyzer();
				break;
			case "hungarian":
				analyzer = new HungarianAnalyzer();
				break;
			case "italian":
				analyzer = new ItalianAnalyzer();
				break;
			case "japanese":
			case "korean":
				analyzer = new CJKAnalyzer();
				break;
			case "norwegian":
				analyzer = new NorwegianAnalyzer();
				break;
			case "portuguese":
				analyzer = new PortugueseAnalyzer();
				break;
			case "russian":
				analyzer = new RussianAnalyzer();
				break;
			case "spanish":
				analyzer = new SpanishAnalyzer();
				break;
			case "swedish":
				analyzer = new SwedishAnalyzer();
				break;
			case "thai":
				analyzer = new ThaiAnalyzer();
				break;
			case "turkish":
				analyzer = new TurkishAnalyzer();
				break;
			default:
				analyzer = loadCustomAnalyzer(language, version);
				break;
			}

			analyzers.put(language, analyzer);
			return analyzer;
		} catch (Exception e) {
			SearchException se = new SearchException("Failed to create analyzer for language: " + language);
			se.initCause(e);
			throw se;
		}
	}

	private static Analyzer loadCustomAnalyzer(String language, Version version) throws SearchException {
		CFMLEngine engine = CFMLEngineFactory.getInstance();
		String clazzName = "org.apache.lucene.analysis.el."
				+ engine.getStringUtil().ucFirst(language.trim().toLowerCase()) + "Analyzer";

		Object o = engine.getClassUtil().loadInstance(clazzName, version);
		if (o == null) {
			clazzName = "org.opencfmlfoundation.search.lucene.analyzer."
					+ engine.getStringUtil().ucFirst(language.trim().toLowerCase()) + "Analyzer";
			o = engine.getClassUtil().loadInstance(clazzName, version);
		}

		if (o instanceof Analyzer)
			return (Analyzer) o;
		else if (o == null)
			throw new SearchException("No Language Analyzer for Language " + language + " found");
		else
			throw new SearchException("Can't create Language Analyzer for Language " + language + ", Analyzer ["
					+ clazzName + "] is of invalid type");
	}
}
