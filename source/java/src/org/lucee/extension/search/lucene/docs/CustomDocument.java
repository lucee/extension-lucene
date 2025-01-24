package org.lucee.extension.search.lucene.docs;

import java.io.IOException;

import org.apache.lucene.document.Document;

import lucee.runtime.exp.PageException;

/** A utility for making Lucene Documents from a File. */

public final class CustomDocument {

	/**
	 * @param title
	 * @param key
	 * @param content
	 * @param custom1
	 * @param custom2
	 * @param custom3
	 * @param custom4
	 * @return Document
	 * @throws IOException
	 * @throws PageException
	 */
	public static Document getDocument(String title, String key, String content, String urlpath, String custom1,
			String custom2, String custom3, String custom4) throws PageException, IOException {

		Document doc = DocumentSupport.createDocument(null, content, "text/plain");

		doc.add(FieldUtil.Text("key", key));

		FieldUtil.setTitle(doc, title);
		FieldUtil.setURL(doc, urlpath);
		FieldUtil.setCustom(doc, custom1, 1);
		FieldUtil.setCustom(doc, custom2, 2);
		FieldUtil.setCustom(doc, custom3, 3);
		FieldUtil.setCustom(doc, custom4, 4);

		return doc;
	}

	private CustomDocument() {
	}

}
