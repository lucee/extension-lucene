package org.lucee.extension.search.lucene.docs;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.document.Document;

import lucee.commons.io.res.Resource;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.exp.PageException;

/** A utility for making Lucene Documents from a File. */

public final class FileDocument {

	// private static final char FILE_SEPARATOR =
	// System.getProperty("file.separator").charAt(0);

	private FileDocument() {
	}

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
		String content = e.getIOUtil().toString(res, e.getCastUtil().toCharset(charset));
		return DocumentSupport.add(DocumentSupport.createDocument(null, content, "text/plain"), res);
	}

	public static Document getDocument(StringBuffer content, Reader r, boolean closeReader)
			throws IOException, PageException {
		CFMLEngine e = CFMLEngineFactory.getInstance();

		try {
			String contents = e.getIOUtil().toString(r);
			if (content != null)
				content.append(contents);

			return DocumentSupport.createDocument(null, contents, "text/plain");
		} finally {
			if (closeReader)
				Util.closeEL(r);
		}
	}

}
