package org.lucee.extension.search.lucene.docs;

import java.io.IOException;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;

import lucee.commons.io.res.Resource;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.exp.PageException;

public class DocumentSupport {
	private static final int SUMMERY_SIZE = 200;
	private static final char FILE_SEPARATOR = System.getProperty("file.separator").charAt(0);

	protected static Document createDocument(String summary, String content, String mimeType)
			throws IOException, PageException {
		CFMLEngine e = CFMLEngineFactory.getInstance();
		boolean indexSummary = true;
		if (Util.isEmpty(summary, true)) {
			summary = max(content, SUMMERY_SIZE, "");
			indexSummary = false;
		}
		// make a new, empty document
		Document doc = new Document();
		FieldUtil.setMimeType(doc, mimeType);
		FieldUtil.setSummary(doc, summary, indexSummary);
		FieldUtil.setRaw(doc, content);
		doc.add(FieldUtil.Text("size", e.getCastUtil().toString(content.length())));
		// doc.add(FieldUtil.UnIndexed("raw", content));

		// Create a custom FieldType for the "contents" field
		FieldType fieldType = new FieldType();
		fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
		fieldType.setTokenized(true);
		fieldType.setStored(true);
		// Enables term vectors
		fieldType.setStoreTermVectors(true);
		fieldType.setStoreTermVectorPositions(true);
		fieldType.setStoreTermVectorOffsets(true);
		fieldType.freeze();

		doc.add(FieldUtil.UnIndexed("size", e.getCastUtil().toString(content.length())));
		doc.add(new Field("contents", content, fieldType));
		return doc;
	}

	public static Document add(Document doc, Resource res) {
		doc.add(FieldUtil.UnIndexed("path", res.getPath()));
		doc.add(FieldUtil.Text("uid", uid(res), false));
		return doc;
	}

	public static String uid(Resource f) {
		return f.getPath().replace(FILE_SEPARATOR, '\u0000') + '\u0000'
				+ DateTools.timeToString(f.lastModified(), DateTools.Resolution.MILLISECOND);

	}

	public static String uid2url(String uid) {
		String url = uid.replace('\u0000', '/'); // replace nulls with slashes
		return url.substring(0, url.lastIndexOf('/')); // remove date from end
	}

	public static String max(String content, int max, String dotDotDot) {
		if (content == null)
			return null;
		if (content.length() <= max)
			return content;

		return content.substring(0, max) + dotDotDot;
	}

}
