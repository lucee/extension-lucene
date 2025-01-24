package org.lucee.extension.search.lucene.docs;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.lucene.document.Document;
import org.lucee.extension.search.lucene.html.HTMLParser;
import org.xml.sax.SAXException;

import lucee.commons.io.res.Resource;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.exp.PageException;

/** A utility for making Lucene Documents for HTML documents. */

public final class HTMLDocument {

	public static Document getDocument(Resource res, String charset) throws PageException, IOException {

		HTMLParser parser = new HTMLParser();
		try {
			parser.parse(res, charset);
		} catch (SAXException e) {
			throw CFMLEngineFactory.getInstance().getCastUtil().toPageException(e);
		}

		return DocumentSupport.add(createContent(parser), res);
	}

	public static Document getDocument(StringBuffer content, Reader reader, boolean closeReader)
			throws PageException, IOException {
		CFMLEngine e = CFMLEngineFactory.getInstance();

		try {
			HTMLParser parser = new HTMLParser();
			String str = e.getIOUtil().toString(reader);
			if (content != null)
				content.append(str);

			parser.parse(new StringReader(str));
			Document doc = createContent(parser);

			doc.add(FieldUtil.UnIndexed("size", e.getCastUtil().toString(str.length())));
			return doc;

		} catch (SAXException ex) {
			throw e.getCastUtil().toPageException(ex);
		} finally {
			if (closeReader)
				Util.closeEL(reader);
		}

	}

	private static Document createContent(HTMLParser parser) throws PageException, IOException {

		Document doc = DocumentSupport.createDocument(parser.getSummary(), parser.getContent(), "text/html");

		FieldUtil.setTitle(doc, parser.getTitle());

		if (parser.hasKeywords()) {
			FieldUtil.setKeywords(doc, parser.getKeywords());
		}
		if (parser.hasAuthor()) {
			FieldUtil.setAuthor(doc, parser.getAuthor());
		}
		if (parser.hasCustom1()) {
			FieldUtil.setCustom(doc, parser.getCustom1(), 1);
		}
		if (parser.hasCustom2()) {
			FieldUtil.setCustom(doc, parser.getCustom2(), 2);
		}
		if (parser.hasCustom3()) {
			FieldUtil.setCustom(doc, parser.getCustom3(), 3);
		}
		if (parser.hasCustom4()) {
			FieldUtil.setCustom(doc, parser.getCustom4(), 4);
		}
		return doc;
	}

	private HTMLDocument() {
	}
}
