package org.lucee.extension.search.lucene;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.br.BrazilianAnalyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.cn.ChineseAnalyzer;
import org.apache.lucene.analysis.cz.CzechAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.el.GreekAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.th.ThaiAnalyzer;
import org.apache.lucene.util.Version;
import org.lucee.extension.search.lucene.analyzer.DanishAnalyzer;
import org.lucee.extension.search.lucene.analyzer.ItalianAnalyzer;
import org.lucee.extension.search.lucene.analyzer.NorwegianAnalyzer;
import org.lucee.extension.search.lucene.analyzer.PortugueseAnalyzer;
import org.lucee.extension.search.lucene.analyzer.SpanishAnalyzer;

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
		StandardTokenizer st = null;

		if (language.equals("english"))
			analyzer = new StandardAnalyzer(Version.LUCENE_36);
		else if (language.equals("german"))
			analyzer = new GermanAnalyzer(Version.LUCENE_36);
		else if (language.equals("russian"))
			analyzer = new RussianAnalyzer(Version.LUCENE_36);
		else if (language.equals("dutch"))
			analyzer = new DutchAnalyzer(Version.LUCENE_36);
		else if (language.equals("french"))
			analyzer = new FrenchAnalyzer(Version.LUCENE_36);
		else if (language.equals("norwegian"))
			analyzer = new NorwegianAnalyzer();
		else if (language.equals("portuguese"))
			analyzer = new PortugueseAnalyzer();
		else if (language.equals("spanish"))
			analyzer = new SpanishAnalyzer();
		else if (language.equals("brazilian"))
			analyzer = new BrazilianAnalyzer(Version.LUCENE_36);
		else if (language.equals("chinese"))
			analyzer = new ChineseAnalyzer();
		else if (language.startsWith("czech"))
			analyzer = new CzechAnalyzer(Version.LUCENE_36);
		else if (language.equals("greek"))
			analyzer = new GreekAnalyzer(Version.LUCENE_36);
		else if (language.equals("thai"))
			analyzer = new ThaiAnalyzer(Version.LUCENE_36);
		else if (language.equals("japanese"))
			analyzer = new CJKAnalyzer(Version.LUCENE_36);
		else if (language.equals("korean"))
			analyzer = new CJKAnalyzer(Version.LUCENE_36);
		else if (language.equals("italian"))
			analyzer = new ItalianAnalyzer();
		else if (language.equals("danish"))
			analyzer = new DanishAnalyzer();
		else if (language.equals("norwegian"))
			analyzer = new NorwegianAnalyzer();
		else if (language.equals("finnish"))
			analyzer = new SnowballAnalyzer(Version.LUCENE_36, "Finnish");
		else if (language.equals("swedish"))
			analyzer = new SnowballAnalyzer(Version.LUCENE_36, "Swedish");
		else if (language.equals("hungarian"))
			analyzer = new SnowballAnalyzer(Version.LUCENE_36, "Hungarian");
		else if (language.equals("turkish"))
			analyzer = new SnowballAnalyzer(Version.LUCENE_36, "Turkish");

		else {
			CFMLEngine engine = CFMLEngineFactory.getInstance();
			String clazzName = "org.apache.lucene.analysis.el."
					+ engine.getStringUtil().ucFirst(language.trim().toLowerCase()) + "Analyzer;";
			Object o = engine.getClassUtil().loadInstance(clazzName, (Object) null);
			if (o == null) {
				clazzName = "org.opencfmlfoundation.search.lucene.analyzer."
						+ engine.getStringUtil().ucFirst(language.trim().toLowerCase()) + "Analyzer";
				o = engine.getClassUtil().loadInstance(clazzName, (Object) null);// Class.orName(clazzName).newInstance();
			}
			if (o instanceof Analyzer)
				analyzer = (Analyzer) o;
			else if (o == null)
				throw new SearchException("No Language Analyzer for Lanuage " + language + " found");
			else
				throw new SearchException("can't create Language Analyzer for Lanuage " + language + ", Analyzer ["
						+ clazzName + "] is of invalid type");
		}
		analyzers.put(language, analyzer);
		return analyzer;
	}
}
