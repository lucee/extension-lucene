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
import lucee.loader.util.Util;
import lucee.runtime.exp.PageException;

public final class PDFDocument {

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
	 * @throws PageException
	 * @throws IOException
	 *             If there is an error parsing or indexing the document.
	 */
	public static Document getDocument(StringBuffer content, InputStream is, boolean closeStream)
			throws PageException, IOException {
		return addContent(content, is, closeStream);
	}

	/**
	 * This will get a lucene document from a PDF file.
	 * 
	 * @param res
	 *            The file to get the document for.
	 * @return The lucene document.
	 * @throws PageException
	 * @throws IOException
	 *             If there is an error parsing or indexing the document.
	 */
	public static Document getDocument(Resource res) throws PageException, IOException {
		System.err.println("pdf: " + res);
		if (!(res instanceof File)) {
			return DocumentSupport.add(addContent(null, res.getInputStream(), true), res);
		}

		org.apache.pdfbox.pdmodel.PDDocument pdfDocument = null;
		try {
			pdfDocument = Loader.loadPDF((File) res);

			if (pdfDocument.isEncrypted()) {
				pdfDocument.close();
				// Just try using the default password and move on
				pdfDocument = Loader.loadPDF((File) res, "");
			}
			return DocumentSupport.add(_addContent(null, pdfDocument), res);

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

	private static Document addContent(StringBuffer content, InputStream is, boolean closeStream)
			throws PageException, IOException {
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
			return _addContent(content, pdfDocument);

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

	private static Document _addContent(StringBuffer content, org.apache.pdfbox.pdmodel.PDDocument pdfDocument)
			throws IOException, PageException {

		// create a writer where to append the text content.
		StringWriter writer = new StringWriter();
		PDFTextStripper stripper = new PDFTextStripper();
		stripper.writeText(pdfDocument, writer);

		// stripper.setLineSeparator("\n");
		// stripper.setStartPage(1);
		// stripper.setEndPage(5);// this mean that it will index the first 5 pages only
		// String conteddnts = stripper.getText(pdfDocument);

		// Note: the buffer to string operation is costless;
		// the char array value of the writer buffer and the content string
		// is shared as long as the buffer content is not modified, which will
		// not occur here.
		String contents = writer.getBuffer().toString();
		System.err.println(contents);

		if (content != null)
			content.append(contents);

		Document document = DocumentSupport.createDocument(null, contents, "application/pdf");

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
		return document;
	}

	/*
	 * private void addToIndex(org.apache.pdfbox.pdmodel.PDDocument pddDocument)
	 * throws Exception {
	 * 
	 * PDFTextStripper textStripper = new PDFTextStripper(); for (int pageNo = 1;
	 * pageNo <= pddDocument.getNumberOfPages(); pageNo++) {
	 * textStripper.setStartPage(pageNo); textStripper.setEndPage(pageNo); String
	 * pageContent = textStripper.getText(pddDocument); //
	 * System.out.println(pageContent); Document doc = new Document();
	 * 
	 * // Add the page number doc.add(new Field("pagenumber",
	 * Integer.toString(pageNo), Field.Store.YES, Field.Index.ANALYZED));
	 * doc.add(new Field("content", pageContent, Field.Store.NO,
	 * Field.Index.ANALYZED)); doc.add(new Field("SOURCE",
	 * source.unRooted(this.root), Field.Store.YES, Field.Index.ANALYZED));
	 * documents.add(doc); getIndexWriter().addDocument(doc); } pddDocument.close();
	 * }
	 */
}