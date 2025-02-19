package org.lucee.extension.search.lucene;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.lucee.extension.search.lucene.docs.FieldUtil;
import org.lucee.extension.search.lucene.docs.FileDocument;
import org.lucee.extension.search.lucene.docs.HTMLDocument;
import org.lucee.extension.search.lucene.docs.PDFDocument;
import org.lucee.extension.search.lucene.docs.WordDocument;

import lucee.commons.io.res.ContentType;
import lucee.commons.io.res.Resource;
import lucee.commons.net.http.HTTPResponse;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.exp.PageException;
import lucee.runtime.util.IO;

/**
 * creates a matching Document Object to given File
 */
public final class DocumentUtil {

	public static Document toDocument(StringBuffer content, String root, URL url, HTTPResponse method)
			throws IOException, PageException {
		CFMLEngine e = CFMLEngineFactory.getInstance();
		IO io = e.getIOUtil();

		if (method.getStatusCode() != 200)
			return null;

		// get type and charset
		Document doc = null;
		ContentType ct = method.getContentType();
		long len = method.getContentLength();
		String charset = ct == null ? "iso-8859-1" : ct.getCharset();

		Runtime rt = Runtime.getRuntime();
		if (len > rt.freeMemory()) {
			Runtime.getRuntime().gc();
			if (len > rt.freeMemory())
				return null;
		}

		// print.err("url:"+url+";chr:"+charset+";type:"+type);

		if (ct == null || ct.getMimeType() == null) {
		}
		// HTML
		else if (ct.getMimeType().indexOf("text/html") != -1) {
			Reader r = io.getReader(method.getContentAsStream(), e.getCastUtil().toCharset(charset));
			doc = HTMLDocument.getDocument(content, r, true);
		}
		// PDF
		else if (ct.getMimeType().indexOf("application/pdf") != -1) {
			InputStream is = io.toBufferedInputStream(method.getContentAsStream());
			doc = PDFDocument.getDocument(content, is, true);
		}
		// DOC
		else if (ct.getMimeType().equals("application/msword")) {
			InputStream is = io.toBufferedInputStream(method.getContentAsStream());
			doc = WordDocument.getDocument(content, is, true);
		}
		// Plain
		else if (ct.getMimeType().indexOf("text/plain") != -1) {
			Reader r = io
					.toBufferedReader(io.getReader(method.getContentAsStream(), e.getCastUtil().toCharset(charset)));
			doc = FileDocument.getDocument(content, r, true);
		}

		if (doc != null) {
			String strPath = url.toExternalForm();

			doc.add(FieldUtil.UnIndexed("url", strPath));
			doc.add(FieldUtil.UnIndexed("key", strPath));
			doc.add(FieldUtil.UnIndexed("path", strPath));
		}

		return doc;

	}

	/**
	 * translate the file to a Document Object
	 * 
	 * @param file
	 * @return
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws PageException
	 */
	public static Document toDocument(Resource file, String url, String charset) throws IOException, PageException {
		CFMLEngine e = CFMLEngineFactory.getInstance();

		String ext = e.getResourceUtil().getExtension(file, null);

		Document doc = null;
		if (ext != null) {
			ext = ext.toLowerCase();
			// HTML
			if (ext.equals("cfm") || ext.equals("htm") || ext.equals("html") || ext.equals("cfml") || ext.equals("php")
					|| ext.equals("asp") || ext.equals("aspx")) {
				doc = HTMLDocument.getDocument(file, charset);
			}
			// PDF
			else if (ext.equals("pdf")) {
				doc = PDFDocument.getDocument(file);
			}
			// DOC
			else if (ext.equals("doc")) {
				doc = WordDocument.getDocument(file);
			}
		} else {
			ContentType ct = e.getResourceUtil().getContentType(file);
			String type = ct.getMimeType();
			String c = ct.getCharset();
			if (c != null)
				charset = c;

			if (type == null) {
				// No-op
			}
			// HTML
			else if (type.equals("text/html")) {
				doc = HTMLDocument.getDocument(file, charset);
			}
			// PDF
			else if (type.equals("application/pdf")) {
				doc = PDFDocument.getDocument(file);
			}
			// DOC
			else if (type.equals("application/msword")) {
				doc = WordDocument.getDocument(file);
			}
		}

		if (doc == null)
			doc = FileDocument.getDocument(file, charset);

		// Add additional metadata fields
		String strPath = file.getPath().replace('\\', '/');
		String strName = strPath.substring(strPath.lastIndexOf('/'));

		doc.add(FieldUtil.UnIndexed("url", strName));
		doc.add(FieldUtil.UnIndexed("key", strPath));
		doc.add(FieldUtil.UnIndexed("path", file.getPath()));
		doc.add(FieldUtil.UnIndexed("size", e.getCastUtil().toString(file.length())));
		doc.add(new StringField("modified",
				DateTools.timeToString(file.lastModified(), DateTools.Resolution.MILLISECOND), Field.Store.YES));

		return doc;
	}

}