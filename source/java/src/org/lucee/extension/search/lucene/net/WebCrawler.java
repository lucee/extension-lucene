package org.lucee.extension.search.lucene.net;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.lucee.extension.search.lucene.DocumentUtil;

import lucee.commons.io.log.Log;
import lucee.commons.net.http.HTTPResponse;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.exp.PageException;

/**
 * 
 */
public final class WebCrawler {

	private static final String[] EXTENSIONS;

	static {
		List<String> list = new ArrayList<String>();
		list.add("cfm");
		list.add("cfml");
		list.add("htm");
		list.add("html");
		list.add("dbm");
		list.add("dbml");
		EXTENSIONS = list.toArray(new String[list.size()]);
	}

	private Log log;

	public WebCrawler(Log log) {
		this.log = log;
	}

	public void parse(IndexWriter writer, URL current, String[] extensions, boolean recurse, long timeout)
			throws IOException, PageException {
		translateExtension(extensions);
		if (extensions == null || extensions.length == 0)
			extensions = EXTENSIONS;
		_parse(log, writer, null, current, new ArrayList<URL>(), extensions, recurse, 0, timeout);
	}

	private static URL translateURL(URL url) throws MalformedURLException {

		CFMLEngine e = CFMLEngineFactory.getInstance();
		// print.out(url.toExternalForm());
		String path = url.getPath();
		int dotIndex = path.lastIndexOf('.');
		// no dot
		if (dotIndex == -1) {
			if (path.endsWith("/"))
				return e.getHTTPUtil().removeUnecessaryPort(url);

			return e.getHTTPUtil().removeUnecessaryPort(new URL(url.getProtocol(), url.getHost(), url.getPort(),
					path + "/" + e.getStringUtil().emptyIfNull(url.getQuery())));
		}
		// print.out("rem:"+HTTPUtil.removeRef(url));
		return e.getHTTPUtil().removeUnecessaryPort(url);
	}

	private void translateExtension(String[] extensions) {
		for (int i = 0; i < extensions.length; i++) {
			if (extensions[i].startsWith("*."))
				extensions[i] = extensions[i].substring(2);
			else if (extensions[i].startsWith("."))
				extensions[i] = extensions[i].substring(1);
		}
	}

	/**
	 * @param writer
	 * @param current
	 * @param content
	 * @throws IOException
	 * @throws PageException
	 */

	private static Document toDocument(StringBuffer content, IndexWriter writer, String root, URL current, long timeout)
			throws IOException, PageException {
		HTTPResponse rsp = CFMLEngineFactory.getInstance().getHTTPUtil().get(current, null, null, (int) timeout, null,
				"RailoBot", null, -1, null, null, null);
		// HTTPResponse rsp = HTTPEngine.get(current, null, null,
		// timeout,HTTPEngine.MAX_REDIRECT, null, "RailoBot", null, null);
		Document doc = DocumentUtil.toDocument(content, root, current, rsp);

		return doc;
	}

	protected static void _parse(Log log, IndexWriter writer, String root, URL current, List<URL> urlsDone,
			String[] extensions, boolean recurse, int deep, long timeout) throws IOException, PageException {

		StringBuffer content = _parseItem(log, writer, root, current, urlsDone, extensions, recurse, deep, timeout);
		if (content != null)
			_parseChildren(log, content, writer, root, current, urlsDone, extensions, recurse, deep, timeout);
	}

	public static StringBuffer _parseItem(Log log, IndexWriter writer, String root, URL url, List<URL> urlsDone,
			String[] extensions, boolean recurse, int deep, long timeout) throws IOException, PageException {
		try {
			url = translateURL(url);
			if (urlsDone.contains(url.toExternalForm()))
				return null;
			urlsDone.add(url);

			StringBuffer content = new StringBuffer();
			Document doc = toDocument(content, writer, root, url, timeout);

			if (doc == null)
				return null;
			if (writer != null)
				writer.addDocument(doc);

			// Test
			/*
			 * Resource dir = ResourcesImpl.getFileResourceProvider().getResource(
			 * "/Users/mic/Temp/leeway3/"); if(!dir.isDirectory())dir.mkdirs(); Resource
			 * file=dir.getRealResource(url.toExternalForm().replace("/", "_"));
			 * IOUtil.write(file, content.toString(), "UTF-8", false);
			 */

			info(log, url.toExternalForm());
			return content;
		} catch (IOException ioe) {
			error(log, url.toExternalForm(), ioe);
			throw ioe;
		} catch (PageException pe) {
			error(log, url.toExternalForm(), pe);
			throw pe;
		}
	}

	protected static void _parseChildren(Log log, StringBuffer content, IndexWriter writer, String root, URL base,
			List<URL> urlsDone, String[] extensions, boolean recurse, int deep, long timeout) throws IOException {

		if (recurse) {
			List<URL> urls = CFMLEngineFactory.getInstance().getHTMLUtil().getURLS(content.toString(), base);

			// loop through all children
			int len = urls.size();
			List childIndexer = len > 1 ? new ArrayList() : null;
			ChildrenIndexer ci;

			for (int i = 0; i < len; i++) {
				URL url = urls.get(i);
				url = translateURL(url);

				if (urlsDone.contains(url.toExternalForm()))
					continue;

				String protocol = url.getProtocol().toLowerCase();
				String file = url.getPath();
				if ((protocol.equals("http") || protocol.equals("https")) && validExtension(extensions, file)
						&& base.getHost().equalsIgnoreCase(url.getHost())) {
					try {
						ci = new ChildrenIndexer(log, writer, root, url, urlsDone, extensions, recurse, deep + 1,
								timeout);

						childIndexer.add(ci);
						ci.start();
					} catch (Throwable t) {
						if (t instanceof ThreadDeath)
							throw (ThreadDeath) t;
					}
				}
			}

			if (childIndexer != null && !childIndexer.isEmpty()) {
				Iterator it = childIndexer.iterator();
				while (it.hasNext()) {
					ci = (ChildrenIndexer) it.next();
					if (ci.isAlive()) {
						try {
							ci.join(timeout);

						} catch (InterruptedException e) {
							// print.printST(e);
						}
					}
					// timeout exceptionif(ci.isAlive()) throw new IOException("timeout occur while
					// invoking page ["+ci.url+"]");

					if (ci.isAlive()) {
						ci.interrupt();
						if (log != null)
							log.error("WebCrawler",
									"timeout [" + timeout + " ms] occur while invoking page [" + ci.url + "]");
					}
				}

				// print.out("exe child");
				it = childIndexer.iterator();
				while (it.hasNext()) {
					ci = (ChildrenIndexer) it.next();
					// print.out("exec-child:"+ci.url);
					// print.out(content);
					if (ci.content != null)
						_parseChildren(log, ci.content, writer, root, ci.url, urlsDone, extensions, recurse, deep,
								timeout);
				}

			}

			urls.clear();
		}
		// print.out("end:"+base);
	}

	/*
	 * protected static void _sssparse(IndexWriter writer, String root, URL current,
	 * List urlsDone, String[] extensions, boolean recurse,int deep,long timeout)
	 * throws IOException { current=translateURL(current);
	 * print.out("start:"+current); if(urlsDone.contains(current.toExternalForm()))
	 * return;
	 * 
	 * HttpMethod method = HTTPUtil.invoke(current, null, null, -1, null,
	 * "RailoBot", null, -1, null, null, null); StringBuffer content=new
	 * StringBuffer(); Document doc = DocumentUtil.toDocument(content,root,current,
	 * method);
	 * 
	 * urlsDone.add(current.toExternalForm()); if(doc==null) return;
	 * if(writer!=null)writer.addDocument(doc);
	 * 
	 * 
	 * if(recurse) { List urls = htmlUtil.getURLS(content.toString(),current);
	 * 
	 * // loop through all children int len=urls.size(); List childIndexer=len>1?new
	 * ArrayList():null; ChildrenIndexer ci; for(int i=0;i<len;i++) { URL url=(URL)
	 * urls.get(i); String protocol=url.getProtocol().toLowerCase(); String
	 * file=url.getPath(); if((protocol.equals("http") || protocol.equals("https"))
	 * && validExtension(extensions,file) &&
	 * current.getHost().equalsIgnoreCase(url.getHost())) {
	 * 
	 * //_parse(writer,root,url,urlsDone,extensions,recurse,deep+1);
	 * 
	 * try { if(len==1 ||
	 * true)_parse(writer,root,url,urlsDone,extensions,recurse,deep+1,timeout); else
	 * { ci=new ChildrenIndexer(writer,root,url,urlsDone,extensions,recurse,deep+1);
	 * ci.start(); childIndexer.add(ci); } } catch(Throwable t) {if(t instanceof
	 * ThreadDeath) throw (ThreadDeath)t; } } }
	 * 
	 * if(!childIndexer.isEmpty()){ Iterator it = childIndexer.iterator();
	 * while(it.hasNext()) { ci=(ChildrenIndexer) it.next(); if(ci.isAlive()) { try
	 * { ci.join(20*1000); } catch (InterruptedException e) {} } } }
	 * 
	 * 
	 * urls.clear(); } print.out("end:"+current); }
	 */

	private static boolean validExtension(String[] extensions, String file) {
		CFMLEngine e = CFMLEngineFactory.getInstance();
		String ext = e.getResourceUtil().getExtension(file, "");
		ext = e.getListUtil().first(ext, "/", true);

		if (e.getStringUtil().isEmpty(ext))
			return true;
		for (int i = 0; i < extensions.length; i++) {
			if (ext.equalsIgnoreCase(extensions[i]))
				return true;
		}
		return false;
	}

	private static void info(Log log, String doc) {
		if (log == null)
			return;
		log.log(Log.LEVEL_INFO, "Webcrawler", "invoke " + doc);
	}

	private static void error(Log log, String doc, Exception e) {
		if (log == null)
			return;
		log.error("Webcrawler", "invoke " + doc + ":", e);
	}
}

class ChildrenIndexer extends Thread {
	protected IndexWriter writer;
	protected String root;
	protected URL url;
	protected List urlsDone;
	protected String[] extensions;
	protected boolean recurse;
	protected int deep;
	protected StringBuffer content;
	private long timeout;
	private Log log;

	public ChildrenIndexer(Log log, IndexWriter writer, String root, URL url, List urlsDone, String[] extensions,
			boolean recurse, int deep, long timeout) {
		this.writer = writer;
		this.root = root;
		this.url = url;
		this.urlsDone = urlsDone;
		this.extensions = extensions;
		this.recurse = recurse;
		this.deep = deep;
		this.timeout = timeout;
		this.log = log;
	}

	@Override
	public void run() {
		try {
			// WebCrawler._parse(writer, root, url, urlsDone, extensions, recurse, deep);

			this.content = WebCrawler._parseItem(log, writer, root, url, urlsDone, extensions, recurse, deep,
					timeout + 1);

		} catch (Exception e) {
		}
	}

}