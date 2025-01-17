package org.lucee.extension.search.lucene.docs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Date;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;

import lucee.commons.io.res.Resource;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.util.IO;

/**
 * This class is used to create a document for the lucene search engine. This
 * should easily plug into the IndexHTML or IndexFiles that comes with the
 * lucene project. This class will populate the following fields.
 * <table>
 * <tr>
 * <td>Lucene Field Name</td>
 * <td>Description</td>
 * </tr>
 * <tr>
 * <td>path</td>
 * <td>File system path if loaded from a file</td>
 * </tr>
 * <tr>
 * <td>url</td>
 * <td>URL to PDF document</td>
 * </tr>
 * <tr>
 * <td>contents</td>
 * <td>Entire contents of PDF document, indexed but not stored</td>
 * </tr>
 * <tr>
 * <td>summary</td>
 * <td>First 500 characters of content</td>
 * </tr>
 * <tr>
 * <td>modified</td>
 * <td>The modified date/time according to the url or path</td>
 * </tr>
 * <tr>
 * <td>uid</td>
 * <td>A unique identifier for the Lucene document.</td>
 * </tr>
 * <tr>
 * <td>CreationDate</td>
 * <td>From PDF meta-data if available</td>
 * </tr>
 * <tr>
 * <td>Creator</td>
 * <td>From PDF meta-data if available</td>
 * </tr>
 * <tr>
 * <td>Keywords</td>
 * <td>From PDF meta-data if available</td>
 * </tr>
 * <tr>
 * <td>ModificationDate</td>
 * <td>From PDF meta-data if available</td>
 * </tr>
 * <tr>
 * <td>Producer</td>
 * <td>From PDF meta-data if available</td>
 * </tr>
 * <tr>
 * <td>Subject</td>
 * <td>From PDF meta-data if available</td>
 * </tr>
 * <tr>
 * <td>Trapped</td>
 * <td>From PDF meta-data if available</td>
 * </tr>
 * </table>
 *
 */
public final class PDFDocument {
	private static final char FILE_SEPARATOR = System.getProperty("file.separator").charAt(0);
	private static final int SUMMERY_SIZE = 200;

	/**
	 * private constructor because there are only static methods.
	 */
	private PDFDocument() {
		// utility class should not be instantiated
	}

	/**
	 * This will get a lucene document from a PDF file.
	 * 
	 * @param is
	 *            The stream to read the PDF from.
	 * @return The lucene document.
	 * @throws IOException
	 *             If there is an error parsing or indexing the document.
	 */
	public static Document getDocument(StringBuffer content, InputStream is) {
		Document document = new Document();
		addContent(content, document, is, false);
		return document;
	}

	/**
	 * This will get a lucene document from a PDF file.
	 * 
	 * @param res
	 *            The file to get the document for.
	 * @return The lucene document.
	 * @throws IOException
	 *             If there is an error parsing or indexing the document.
	 */
	public static Document getDocument(Resource res) {
		CFMLEngine en = CFMLEngineFactory.getInstance();
		IO io = en.getIOUtil();

		Document document = new Document();
		FieldUtil.setMimeType(document, "application/pdf");
		// document.add(FieldUtil.UnIndexed("mime-type", "application/pdf"));
		document.add(FieldUtil.UnIndexed("path", res.getPath()));

		String uid = res.getPath().replace(FILE_SEPARATOR, '\u0000') + "\u0000"
				+ DateTools.timeToString(res.lastModified(), DateTools.Resolution.MILLISECOND);
		document.add(FieldUtil.Text("uid", uid, false));

		// Add the uid as a field, so that index can be incrementally maintained.
		// This field is not stored with document, it is indexed, but it is not
		// tokenized prior to indexing.
		// document.add(new Field("uid", uid, Field.Store.NO,Field.Index.UN_TOKENIZED));
		// document.add(new Field("uid", uid, false, true, false));

		addContent(null, document, res);
		return document;
	}

	private static void addContent(StringBuffer content, Document document, Resource res) {
		if (!(res instanceof File)) {
			try {
				addContent(content, document, res.getInputStream(), true);
			} catch (IOException e) {
			}
			return;
		}

		org.apache.pdfbox.pdmodel.PDDocument pdfDocument = null;
		try {
			pdfDocument = Loader.loadPDF((File) res);

			if (pdfDocument.isEncrypted()) {
				pdfDocument.close();
				// Just try using the default password and move on
				pdfDocument = Loader.loadPDF((File) res, "");
			}
			_addContent(content, document, pdfDocument);

		} catch (IOException ioe) {
			// TODO Log
		} finally {
			if (pdfDocument != null) {
				try {
					pdfDocument.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static void addContent(StringBuffer content, Document document, InputStream is, boolean closeStream) {
		org.apache.pdfbox.pdmodel.PDDocument pdfDocument = null;
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			Util.copy(is, baos, closeStream, true);
			byte[] barr = baos.toByteArray();
			pdfDocument = Loader.loadPDF(barr);

			if (pdfDocument.isEncrypted()) {
				pdfDocument.close();
				// Just try using the default password and move on
				pdfDocument = Loader.loadPDF(barr, "");
			}
			_addContent(content, document, pdfDocument);
		} catch (IOException ioe) {
			// TODO Log
		} finally {
			if (pdfDocument != null) {
				try {
					pdfDocument.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static void _addContent(StringBuffer content, Document document,
			org.apache.pdfbox.pdmodel.PDDocument pdfDocument) throws IOException {

		// create a writer where to append the text content.
		StringWriter writer = new StringWriter();
		PDFTextStripper stripper = new PDFTextStripper();
		stripper.writeText(pdfDocument, writer);

		// Note: the buffer to string operation is costless;
		// the char array value of the writer buffer and the content string
		// is shared as long as the buffer content is not modified, which will
		// not occur here.
		String contents = writer.getBuffer().toString();
		if (content != null)
			content.append(contents);

		FieldUtil.setRaw(document, contents);
		FieldUtil.setContent(document, contents);
		FieldUtil.setSummary(document, WordDocument.max(contents, SUMMERY_SIZE, ""), false);

		PDDocumentInformation info = pdfDocument.getDocumentInformation();
		if (info.getAuthor() != null) {
			FieldUtil.setAuthor(document, info.getAuthor());
		}
		if (info.getCreationDate() != null) {
			Date date = info.getCreationDate().getTime();
			if (date.getTime() >= 0) {
				document.add(FieldUtil.Text("CreationDate",
						DateTools.timeToString(date.getTime(), DateTools.Resolution.MILLISECOND)));
			}
		}
		if (info.getCreator() != null) {
			document.add(FieldUtil.Text("Creator", info.getCreator()));
		}
		if (info.getKeywords() != null) {
			FieldUtil.setKeywords(document, info.getKeywords());
		}
		if (info.getModificationDate() != null) {
			Date date = info.getModificationDate().getTime();
			if (date.getTime() >= 0) {
				document.add(FieldUtil.Text("ModificationDate",
						DateTools.timeToString(date.getTime(), DateTools.Resolution.MILLISECOND)));
			}
		}
		if (info.getProducer() != null) {
			document.add(FieldUtil.Text("Producer", info.getProducer()));
		}
		if (info.getSubject() != null) {
			document.add(FieldUtil.Text("Subject", info.getSubject()));
		}
		if (info.getTitle() != null) {
			FieldUtil.setTitle(document, info.getTitle());
		}
		if (info.getTrapped() != null) {
			document.add(FieldUtil.Text("Trapped", info.getTrapped()));
		}

	}
}