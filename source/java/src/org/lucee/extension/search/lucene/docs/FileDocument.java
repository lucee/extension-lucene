package org.lucee.extension.search.lucene.docs;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;

import lucee.commons.io.res.Resource;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.exp.PageException;

/** A utility for making Lucene Documents from a File. */

public final class FileDocument {

	// private static final char FILE_SEPARATOR =
	// System.getProperty("file.separator").charAt(0);
	private static final int SUMMERY_SIZE = 200;

	/**
	 * Makes a document for a File.
	 * <p>
	 * The document has three fields:
	 * <ul>
	 * <li><code>path</code>--containing the pathname of the file, as a stored,
	 * tokenized field;
	 * <li><code>modified</code>--containing the last modified date of the file as a
	 * keyword field as encoded by
	 * <a href="lucene.document.DateField.html">DateField</a>; and
	 * <li><code>contents</code>--containing the full contents of the file, as a
	 * Reader field;
	 * 
	 * @param res
	 * @return matching document
	 * @throws IOException
	 * @throws PageException
	 */
	public static Document getDocument(Resource res, String charset) throws IOException, PageException {
		CFMLEngine e = CFMLEngineFactory.getInstance();

		// make a new, empty document
		Document doc = new Document();
		doc.add(FieldUtil.UnIndexed("mime-type", "text/plain"));

		String content = e.getIOUtil().toString(res, e.getCastUtil().toCharset(charset));
		FieldUtil.setRaw(doc, content);
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

		doc.add(new Field("contents", content, fieldType));
		// doc.add(new TextField("contents", content.toLowerCase(), Field.Store.YES));
		doc.add(FieldUtil.UnIndexed("summary", WordDocument.max(content, SUMMERY_SIZE, "")));
		return doc;
	}

	public static Document getDocument(StringBuffer content, Reader r) throws IOException {
		CFMLEngine e = CFMLEngineFactory.getInstance();

		// make a new, empty document
		Document doc = new Document();
		FieldUtil.setMimeType(doc, "text/plain");
		//
		String contents = e.getIOUtil().toString(r);
		if (content != null)
			content.append(contents);
		doc.add(FieldUtil.UnIndexed("size", e.getCastUtil().toString(contents.length())));
		FieldUtil.setContent(doc, contents);
		FieldUtil.setRaw(doc, contents);
		FieldUtil.setSummary(doc, WordDocument.max(contents, SUMMERY_SIZE, ""), false);
		return doc;
	}

	private FileDocument() {
	}
}
