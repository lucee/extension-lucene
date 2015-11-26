package org.lucee.extension.search.lucene.docs;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import lucee.loader.util.Util;

public class FieldUtil {

	public static Field UnIndexed(String name, String value) {
		return new Field(name,value,Field.Store.YES,Field.Index.NO);
	}

	public static Field Text(String name, String value) {//print.out("text:"+name);
		return new Field(name,value,Field.Store.YES,Field.Index.ANALYZED);
	}

	public static Field Text(String name, String value,boolean store) {
		return new Field(name,value,store?Field.Store.YES:Field.Store.NO,Field.Index.ANALYZED);
	}

	public static void setTitle(Document doc, String title) {
		if(!Util.isEmpty(title))		doc.add(Text("title", title));
	}

	public static void setSummary(Document doc, String summary,boolean index) {
		if(!Util.isEmpty(summary))	doc.add(index?Text("summary",summary):UnIndexed("summary",summary));
	}

	public static void setKeywords(Document doc, String keywords) {
		if(!Util.isEmpty(keywords))	doc.add(Text("keywords", keywords));
	}

	public static void setAuthor(Document doc, String author) {
		if(!Util.isEmpty(author))		doc.add(Text("author", author));
	}

	public static void setURL(Document doc, String urlpath) {
		if(!Util.isEmpty(urlpath))		doc.add(Text("url", urlpath));
	}
	public static void setCustom(Document doc, String custom, int index) {
		if(!Util.isEmpty(custom))		doc.add(Text("custom"+index, custom));
	}

	public static void setContent(Document doc, String content) {
		if(!Util.isEmpty(content))	doc.add(Text("contents", content));
	}

	public static void setRaw(Document doc, String raw) {
		//doc.add(new Field("raw",raw,Field.Store.YES,Field.Index.NO));
	}

	public static void setMimeType(Document doc, String mimeType) {
		if(!Util.isEmpty(mimeType))	doc.add(FieldUtil.UnIndexed("mime-type", mimeType));
	}

	
}
