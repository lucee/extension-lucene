package org.lucee.extension.search.lucene.docs;

import java.io.IOException;
import java.io.InputStream;

import org.apache.lucene.document.Document;

import lucee.commons.io.res.Resource;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.exp.PageException;
import lucee.runtime.util.IO;

/** A utility for making Lucene Documents from a File. */

public final class PlainDocument {
    
    private static final int SUMMERY_SIZE=20;

    //private static final char FILE_SEPARATOR = System.getProperty("file.separator").charAt(0);
  /** Makes a document for a File.
    <p>
    The document has three fields:
    <ul>
    <li><code>path</code>--containing the pathname of the file, as a stored,
    tokenized field;
    <li><code>modified</code>--containing the last modified date of the file as
    a keyword field as encoded by <a
    href="lucene.document.DateField.html">DateField</a>; and
    <li><code>contents</code>--containing the full contents of the file, as a
    Reader field;
 * @param f
 * @return matching document
 * @throws IOException
 * @throws PageException 
    */
  public static Document Document(Resource f,String charset)
       throws IOException, PageException {
	 
    // make a new, empty document
    Document doc = new Document();
    
    doc.add(FieldUtil.UnIndexed("path", f.getPath()));
    CFMLEngine en = CFMLEngineFactory.getInstance();
    IO io = en.getIOUtil();
    InputStream is = null;
    try {
    	is=io.toBufferedInputStream(f.getInputStream());
    	String content=io.toString(is,en.getCastUtil().toCharset(charset));
    	FieldUtil.setMimeType(doc, "text/plain");
    	FieldUtil.setRaw(doc,content);
    	FieldUtil.setContent(doc, content);
    	//doc.add(FieldUtil.Text("contents", content.toLowerCase()));
    	FieldUtil.setSummary(doc, WordDocument.max(content,SUMMERY_SIZE,""),false);
    }
    finally {
    	io.closeSilent(is);
    }
    
    //Reader reader = new BufferedReader(new InputStreamReader(is));
   

    // return the document
    return doc;
  }

  private PlainDocument() {}
}
    