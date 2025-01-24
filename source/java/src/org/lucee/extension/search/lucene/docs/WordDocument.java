package org.lucee.extension.search.lucene.docs;

import java.io.IOException;
import java.io.InputStream;

import org.apache.lucene.document.Document;
import org.textmining.text.extraction.WordExtractor;

import lucee.commons.io.res.Resource;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.exp.PageException;

/** A utility for making Lucene Documents from a File. */

public final class WordDocument {

	private WordDocument() {
	}

	public static Document getDocument(Resource res) throws IOException, PageException {
		return DocumentSupport.add(getDocument(null, res.getInputStream(), true), res);
	}

	public static Document getDocument(StringBuffer content, InputStream is, boolean closeStream)
			throws IOException, PageException {
		CFMLEngine en = CFMLEngineFactory.getInstance();

		WordExtractor extractor = new WordExtractor();
		String contents;
		try {
			contents = extractor.extractText(is);
			if (content != null)
				content.append(contents);

			return DocumentSupport.createDocument(null, contents, "application/msword");

		} catch (Exception e) {
			throw en.getCastUtil().toPageException(e);
		} finally {
			if (closeStream)
				Util.closeEL(is);
		}

	}
}
