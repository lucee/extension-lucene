package org.lucee.extension.search.lucene.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import lucee.commons.io.res.Resource;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.config.Config;
import lucee.runtime.exp.PageException;
import lucee.runtime.search.SearchCollection;
import lucee.runtime.search.SearchException;
import lucee.runtime.type.Array;
import lucee.runtime.type.Struct;
import lucee.runtime.type.dt.DateTime;

public class XMLUtil {

	public static void writeTo(Node node, Result res, boolean omitXMLDecl, boolean indent, String publicId,
			String systemId, String encoding) throws PageException {
		try {
			Transformer t = XMLUtil.getTransformerFactory().newTransformer();
			t.setOutputProperty(OutputKeys.INDENT, indent ? "yes" : "no");
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitXMLDecl ? "yes" : "no");
			// t.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "2");

			// optional properties
			if (!Util.isEmpty(publicId, true))
				t.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, publicId);
			if (!Util.isEmpty(systemId, true))
				t.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, systemId);
			if (!Util.isEmpty(encoding, true))
				t.setOutputProperty(OutputKeys.ENCODING, encoding);

			t.transform(new DOMSource(node), res);
		} catch (Exception e) {
			throw CFMLEngineFactory.getInstance().getCastUtil().toPageException(e);
		}
	}

	public static TransformerFactory getTransformerFactory() {
		return TransformerFactory.newInstance();
	}

	public static final Document parse(InputSource xml, InputSource validator, boolean isHtml) throws PageException {
		// FUTURE use interface from loader
		try {
			Class<?> clazz = CFMLEngineFactory.getInstance().getClassUtil().loadClass("lucee.runtime.text.xml.XMLUtil");
			Method method = clazz.getMethod("parse",
					new Class[] { InputSource.class, InputSource.class, boolean.class });
			return (Document) method.invoke(null, new Object[] { xml, validator, isHtml });
		} catch (Exception e) {
			throw CFMLEngineFactory.getInstance().getCastUtil().toPageException(e);
		}
	}

	public static Struct importXML(lucee.runtime.config.Config config, Resource searchFileXML)
			throws IOException, SearchException {
		CFMLEngine engine = CFMLEngineFactory.getInstance();

		// DOMParser parserq = new DOMParser();
		InputStream is = null;
		Document doc;
		try {
			is = engine.getIOUtil().toBufferedInputStream(searchFileXML.getInputStream());
			InputSource source = new InputSource(is);
			// parser.parse(source);
			doc = XMLUtil.parse(source, null, false);

		} catch (PageException e) {
			throw CommonUtil.toSearchException(e);
		} finally {
			engine.getIOUtil().closeSilent(is);
		}
		// doc = parser.getDocument();
		return toJson(config, doc);
	}

	private static Struct toJson(Config config, Document doc) throws SearchException {
		CFMLEngine engine = CFMLEngineFactory.getInstance();
		Struct sctRoot = engine.getCreationUtil().createStruct(Struct.TYPE_LINKED);
		Array arrColl = engine.getCreationUtil().createArray();
		sctRoot.setEL("collections", arrColl);
		Element root = doc.getDocumentElement();
		NodeList children = root.getChildNodes();
		int len = children.getLength();
		for (int i = 0; i < len; i++) {
			Node n = children.item(i);
			if (n instanceof Element && n.getNodeName().equals("collection")) {
				readCollection(engine, config, arrColl, (Element) n);
			}
		}
		return sctRoot;
	}

	private static final void readCollection(CFMLEngine engine, Config config, Array arrColl, Element el)
			throws SearchException {
		SearchCollection sc;

		Struct sctColl = engine.getCreationUtil().createStruct(Struct.TYPE_LINKED);
		arrColl.appendEL(sctColl);

		// lastUpdate
		DateTime last = engine.getCastUtil().toDateTime(el.getAttribute("lastUpdate"), engine.getThreadTimeZone(),
				null);
		if (last == null) {
			last = engine.getCreationUtil().now();
		}
		sctColl.setEL("lastUpdate", last);

		DateTime cre = engine.getCastUtil().toDateTime(el.getAttribute("created"), engine.getThreadTimeZone(), null);
		if (cre == null) {
			cre = engine.getCreationUtil().now();
		}
		sctColl.setEL("created", cre);

		// language
		String lang = el.getAttribute("language");
		if (!Util.isEmpty(lang, true)) {
			sctColl.setEL("language", lang.trim());
		}

		// name
		String name = el.getAttribute("name");
		if (!Util.isEmpty(name, true)) {
			sctColl.setEL("name", name.trim());
		}

		// path
		String path = el.getAttribute("path");
		if (!Util.isEmpty(path, true)) {
			sctColl.setEL("path", path.trim());
		}

		// embedding
		String embedding = el.getAttribute("embedding");
		if (!Util.isEmpty(embedding, true)) {
			sctColl.setEL("embedding", embedding.trim());
		}

		// Indexes
		NodeList children = el.getChildNodes();
		int len = children.getLength();
		if (len > 0) {
			Array arrIdx = engine.getCreationUtil().createArray();
			sctColl.setEL("indexes", arrIdx);
			for (int i = 0; i < len; i++) {
				Node n = children.item(i);
				if (n instanceof Element && n.getNodeName().equals("index")) {
					readIndex(engine, arrIdx, (Element) n);
				}
			}
		}
	}

	protected static void readIndex(CFMLEngine engine, Array arrIdx, Element el) throws SearchException {
		// Index
		Struct sctIdx = engine.getCreationUtil().createStruct(Struct.TYPE_LINKED);
		arrIdx.appendEL(sctIdx);
		// id
		String id = el.getAttribute("id");
		if (!Util.isEmpty(id, true)) {
			sctIdx.setEL("id", id.trim());
		}
		// title
		String title = el.getAttribute("title");
		if (!Util.isEmpty(title, true)) {
			sctIdx.setEL("title", title.trim());
		}
		// key
		String key = el.getAttribute("key");
		if (!Util.isEmpty(key, true)) {
			sctIdx.setEL("key", key.trim());
		}
		// type
		String type = el.getAttribute("type");
		if (!Util.isEmpty(type, true)) {
			sctIdx.setEL("type", type.trim());
		}
		// query
		String query = el.getAttribute("query");
		if (!Util.isEmpty(query, true)) {
			sctIdx.setEL("query", query.trim());
		}
		// extensions
		String extensions = el.getAttribute("extensions");
		if (!Util.isEmpty(extensions, true)) {
			sctIdx.setEL("extensions", extensions.trim());
		}
		// language
		String language = el.getAttribute("language");
		if (!Util.isEmpty(language, true)) {
			sctIdx.setEL("language", language.trim());
		}
		// urlpath
		String urlpath = el.getAttribute("urlpath");
		if (!Util.isEmpty(urlpath, true)) {
			sctIdx.setEL("urlpath", urlpath.trim());
		}
		// categoryTree
		String categoryTree = el.getAttribute("categoryTree");
		if (!Util.isEmpty(categoryTree, true)) {
			sctIdx.setEL("categoryTree", categoryTree.trim());
		}
		// category
		String category = el.getAttribute("category");
		if (!Util.isEmpty(category, true)) {
			sctIdx.setEL("category", category.trim());
		}
		// custom1
		String custom1 = el.getAttribute("custom1");
		if (!Util.isEmpty(custom1, true)) {
			sctIdx.setEL("custom1", custom1.trim());
		}
		// custom2
		String custom2 = el.getAttribute("custom2");
		if (!Util.isEmpty(custom2, true)) {
			sctIdx.setEL("custom2", custom2.trim());
		}
		// custom3
		String custom3 = el.getAttribute("custom3");
		if (!Util.isEmpty(custom3, true)) {
			sctIdx.setEL("custom3", custom3.trim());
		}
		// custom4
		String custom4 = el.getAttribute("custom4");
		if (!Util.isEmpty(custom4, true)) {
			sctIdx.setEL("custom4", custom4.trim());
		}

	}

}
