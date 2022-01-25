package org.lucee.extension.search.lucene.util;

import java.lang.reflect.Method;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.exp.PageException;

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
}
