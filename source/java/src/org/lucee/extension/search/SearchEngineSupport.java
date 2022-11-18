package org.lucee.extension.search;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import javax.xml.transform.stream.StreamResult;

import lucee.commons.io.res.Resource;
import lucee.commons.io.res.ResourceProvider;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.config.Config;
import lucee.runtime.exp.PageException;
import lucee.runtime.search.SearchCollection;
import lucee.runtime.search.SearchEngine;
import lucee.runtime.search.SearchException;
import lucee.runtime.search.SearchIndex;
import lucee.runtime.type.Collection.Key;
import lucee.runtime.type.Query;
import lucee.runtime.type.Struct;
import lucee.runtime.type.dt.DateTime;

import org.lucee.extension.search.lucene.util.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * 
 */
public abstract class SearchEngineSupport implements SearchEngine {
	
	private static final String DEFAULT_SEARCH_XML="<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>\n<search>\n</search>";

	private Resource searchFile;
	private Resource searchDir;
	private Document doc;
	final Struct collections;
	protected Config config;
	private CFMLEngine engine;
	
	public SearchEngineSupport(){
		engine=CFMLEngineFactory.getInstance();
		collections=engine.getCreationUtil().createStruct();
	}
	
	@Override
	public void init(lucee.runtime.config.Config config,Resource searchDir) throws IOException, SearchException {
		this.config=config;
		this.searchDir=searchDir;
		this.searchFile=searchDir.getRealResource("search.xml");
		if(!searchFile.exists() || searchFile.length()==0) createSearchFile(searchFile);
		//DOMParser parserq = new DOMParser();
		InputStream is=null;
	    try {
			is = engine.getIOUtil().toBufferedInputStream(searchFile.getInputStream());
	        InputSource source = new InputSource(is);
	    	//parser.parse(source);
	    	doc=XMLUtil.parse(source, null, false);
			
	    }
	    catch (PageException e) {
			throw new SearchException(e);
		}
	    finally {
	    	engine.getIOUtil().closeSilent(is);
	    }
    	//doc = parser.getDocument();
    	    	
        
    	readCollections(config);
	} 
	
	@Override
	public final SearchCollection getCollectionByName(String name) throws SearchException {
		Object o=collections.get(name.toLowerCase(),null);
		if(o!=null)return (SearchCollection) o; 
		throw new SearchException("collection "+name+" is undefined");
	}
	
	@Override
	public final Query getCollectionsAsQuery() {
        final String v="VARCHAR";
        Query query=null;
        String[] cols = new String[]{"external","language","mapped","name","online","path","registered","lastmodified","categories","charset","created",
        							 "size","doccount"};
        String[] types = new String[]{"BOOLEAN",v,"BOOLEAN",v,"BOOLEAN",v,v,"DATE","BOOLEAN",v,"OBJECT","DOUBLE","DOUBLE"};
        try {
            query=engine.getCreationUtil().createQuery(cols,types, collections.size(),"query");
        } catch (PageException e) {
            query=engine.getCreationUtil().createQuery(cols, collections.size(),"query");
        }
        
        //Collection.Key[] keys = collections.keys();
	    Iterator<Object> it = collections.valueIterator();
	    int i=-1;
        while(it.hasNext()) {
	        i++;
        	try {
		        SearchCollection coll = (SearchCollection) it.next();
                query.setAt("external",i+1,Boolean.FALSE);
                query.setAt("charset",i+1,"UTF-8");
                query.setAt("created",i+1,coll.created());
                
                query.setAt("categories",i+1,Boolean.TRUE);
		        query.setAt("language",i+1,coll.getLanguage());
		        query.setAt("mapped",i+1,Boolean.FALSE);
		        query.setAt("name",i+1,coll.getName());
		        query.setAt("online",i+1,Boolean.TRUE);
		        query.setAt("path",i+1,coll.getPath().getAbsolutePath());
		        query.setAt("registered",i+1,"CF");
		        query.setAt("lastmodified",i+1,coll.getLastUpdate());
		        query.setAt("size",i+1,Double.valueOf(coll.getSize()));
		        query.setAt("doccount",i+1,Double.valueOf(coll.getDocumentCount())); 
	        }
		    catch(PageException pe) {}
	    }
		return query;
	}
	
	@Override
	public final SearchCollection createCollection(String name,Resource path, String language, boolean allowOverwrite) throws SearchException {
	    SearchCollection coll = _createCollection(name,path,language);
	    coll.create();
	    addCollection(coll,allowOverwrite);
	    return coll;
	}
	
	/**
	 * Creates a new Collection, will be invoked by createCollection
	 * @param name The Name of the Collection
	 * @param path the path to store
	 * @param language The language of the collection
	 * @return New SearchCollection
	 * @throws SearchException
	 */
	protected abstract SearchCollection _createCollection(String name,Resource path, String language) throws SearchException;
	
	/**
	 * adds a new Collection to the storage
	 * @param collection
	 * @param allowOverwrite if allowOverwrite is false and a collection already exist -> throw Exception
	 * @throws SearchException
	 */
	private final synchronized void addCollection(SearchCollection collection, boolean allowOverwrite) throws SearchException {
	    Object o = collections.get(collection.getName(),null);
	    if(!allowOverwrite && o!=null)
		    throw new SearchException("there is already a collection with name "+collection.getName());
		collections.setEL(collection.getName(),collection);
		// update
		if(o!=null) {
		    setAttributes(getCollectionElement(collection.getName()),collection);
		}
		// create
		else {
		    doc.getDocumentElement().appendChild(toElement(collection));
		}
		store();
	}

	/**
	 * removes a Collection from the storage
	 * @param collection Collection to remove
	 * @throws SearchException
	 */
	protected final synchronized void removeCollection(SearchCollection collection)
			throws SearchException {
	    removeCollection(collection.getName());
	    _removeCollection(collection);
	}
    
    /**
     * removes a Collection from the storage
     * @param collection Collection to remove
     * @throws SearchException
     */
    protected abstract void _removeCollection(SearchCollection collection) throws SearchException;
    
    /**
     * removes a Collection from the storage
     * @param name Name of the Collection to remove
     * @throws SearchException
     */
    private final synchronized void removeCollection(String name) throws SearchException {
        try {
            collections.remove(engine.getCastUtil().toKey(name));
            doc.getDocumentElement().removeChild(getCollectionElement(name));
            store();
        } 
        catch (PageException e) {
            throw new SearchException("can't remove collection "+name+", collection doesn't exist");
        }
    }
    
    
    /**
     * purge a Collection 
     * @param collection Collection to purge
     * @throws SearchException
     */
    protected final synchronized void purgeCollection(SearchCollection collection)
            throws SearchException {
        
        purgeCollection(collection.getName());
    }
    
    /**
     * purge a Collection
     * @param name Name of the Collection to purge
     * @throws SearchException
     */
    private final synchronized void purgeCollection(String name) throws SearchException {
        
            //Map map=(Map)collections.get(name);
            //if(map!=null)map.clear();
            Element parent = getCollectionElement(name);
            NodeList list=parent.getChildNodes();
            int len=list.getLength();
            for(int i=len-1;i>=0;i--) {
                parent.removeChild(list.item(i));
            }
            //doc.getDocumentElement().removeChild(getCollectionElement(name));
            store();
        
    }

	@Override
	public Resource getDirectory() {
		return searchDir;
	}

    /**
     * return XML Element matching collection name
     * @param name
     * @return matching XML Element
     */
    protected final Element getCollectionElement(String name) {
        Element root = doc.getDocumentElement();
        NodeList children = root.getChildNodes();
        int len=children.getLength();
        for(int i=0;i<len;i++) {
            Node n=children.item(i);
            if(n instanceof Element && n.getNodeName().equals("collection")) {
                Element el = (Element)n;
                if(el.getAttribute("name").equalsIgnoreCase(name)) return el;
            }
        }
        return null;
    }

    public Element getIndexElement(Element collElement, String id) {
        NodeList children = collElement.getChildNodes();
        int len=children.getLength();
        for(int i=0;i<len;i++) {
            Node n=children.item(i);
            if(n instanceof Element && n.getNodeName().equals("index")) {
                Element el = (Element)n;
                if(el.getAttribute("id").equals(id)) return el;
            }
        }
        return null;
    }

    /**
     * translate a collection object to a XML Element
     * @param coll Collection to translate
     * @return XML Element
     */
    private final Element toElement(SearchCollection coll) {
        Element el = doc.createElement("collection");
        setAttributes(el,coll);   
        return el;
    }

    /**
     * translate a collection object to a XML Element
     * @param index Index to translate
     * @return XML Element
     * @throws SearchException
     */
    protected final Element toElement(SearchIndex index) throws SearchException {
        Element el = doc.createElement("index");
        setAttributes(el,index);   
        return el;
    }
    
    /**
     * sets all attributes in XML Element from Search Collection
     * @param el
     * @param coll
     */
    private final void setAttributes(Element el,SearchCollection coll) {
        if(el==null) return;
        setAttribute(el,"language",coll.getLanguage());
        setAttribute(el,"name",coll.getName());
        
    	String value = coll.getLastUpdate().castToString(null);
        if(value!=null)setAttribute(el,"lastUpdate",value);
        value=coll.getCreated().castToString(null);
        if(value!=null)setAttribute(el,"created",value);
        
        setAttribute(el,"path",coll.getPath().getAbsolutePath());
    }
    
    /**
     * sets all attributes in XML Element from Search Index
     * @param el
     * @param index
     * @throws SearchException
     */
    protected final void setAttributes(Element el,SearchIndex index) throws SearchException {
        if(el==null) return;
        setAttribute(el,"categoryTree",index.getCategoryTree());
        setAttribute(el,"category",engine.getListUtil().toList(index.getCategories(),","));
        setAttribute(el,"custom1",index.getCustom1());
        setAttribute(el,"custom2",index.getCustom2());
        setAttribute(el,"custom3",index.getCustom3());
        setAttribute(el,"custom4",index.getCustom4());
        setAttribute(el,"id",index.getId());
        setAttribute(el,"key",index.getKey());
        setAttribute(el,"language",index.getLanguage());
        setAttribute(el,"title",index.getTitle());
        setAttribute(el,"extensions",engine.getListUtil().toList(index.getExtensions(),","));
        setAttribute(el,"type",SearchIndexImpl.toStringType(index.getType()));
        setAttribute(el,"urlpath",index.getUrlpath());
        setAttribute(el,"query",index.getQuery());
    }

	/**
	 * helper method to set a attribute
     * @param el
     * @param name
     * @param value
     */
    private void setAttribute(Element el, String name, String value) {
        if(value!=null)el.setAttribute(name,value);
    }

    /**
	 * read in collections
     * @param config 
	 * @throws SearchException
     */
    private void readCollections(Config config) throws SearchException {
        Element root = doc.getDocumentElement();
        NodeList children = root.getChildNodes();
        int len=children.getLength();
        for(int i=0;i<len;i++) {
            Node n=children.item(i);
            if(n instanceof Element && n.getNodeName().equals("collection")) {
                readCollection(config,(Element)n);
            }
        }
    }

    /**
     * read in a single collection element
     * @param config 
     * @param el
     * @throws SearchException
     */
    private final void readCollection(Config config, Element el) throws SearchException {
        SearchCollection sc;
        //try {
            // Collection
            DateTime last = engine.getCastUtil().toDateTime(el.getAttribute("lastUpdate"),engine.getThreadTimeZone(),null);
            if(last==null)last=engine.getCreationUtil().now();
            DateTime cre = engine.getCastUtil().toDateTime(el.getAttribute("created"),engine.getThreadTimeZone(),null);
            if(cre==null)cre=engine.getCreationUtil().now();
            ResourceProvider frp = engine.getResourceUtil().getFileResourceProvider();
            sc =_readCollection(
                    el.getAttribute("name"),
                    frp.getResource(el.getAttribute("path")),
                    el.getAttribute("language"),
                    last,cre
            );
            collections.setEL((sc.getName()),sc);
            
            // Indexes
            NodeList children = el.getChildNodes();
            int len=children.getLength();
            for(int i=0;i<len;i++) {
                Node n=children.item(i);
                if(n instanceof Element && n.getNodeName().equals("index")) {
                    readIndex(sc,(Element)n);
                }
            }
        /*} 
        catch (PageException e) {
            throw new SearchException(e);
        }*/
    }

    /**
     * read in a single Index
     * @param sc
     * @param el
     * @throws SearchException
     * @throws PageException
     */
    protected void readIndex(SearchCollection sc, Element el) throws SearchException {
            // Index
            SearchIndex si=new SearchIndexImpl(
                    _attr(el,"id"),
                    _attr(el,"title"),
                    _attr(el,"key"),
                    SearchIndexImpl.toType(_attr(el,"type")),
                    _attr(el,"query"),
                    engine.getListUtil().toStringArray(_attr(el,"extensions"),","),
                    _attr(el,"language"),
                    _attr(el,"urlpath"),
                    _attr(el,"categoryTree"),
                    engine.getListUtil().toStringArray(_attr(el,"category"),","),
                    _attr(el,"custom1"),
                    _attr(el,"custom2"),
                    _attr(el,"custom3"),
                    _attr(el,"custom4"));
           sc.addIndex(si);
    }

  private String _attr(Element el, String attr) {
        return engine.getStringUtil().emptyIfNull(el.getAttribute(attr));
    }

      /**
     * read in a existing collection
     * @param name
     * @param parh
     * @param language
     * @param count
     * @param lastUpdate
     * @param created 
     * @return SearchCollection
     * @throws SearchException
     */
    protected abstract SearchCollection _readCollection(String name, Resource parh, String language, DateTime lastUpdate, DateTime created) throws SearchException;

	/**
     * store loaded data to xml file
	 * @throws SearchException
     */
    protected final synchronized void store() throws SearchException {
        //Collection.Key[] keys=collections.keys();
        Iterator<Key> it = collections.keyIterator();
        Key k;
    	while(it.hasNext()) {
    		k=it.next();
            Element collEl = getCollectionElement(k.getString());
            SearchCollection sc = getCollectionByName(k.getString());
            setAttributes(collEl,sc);  
        }
    	
        //OutputFormat format = new OutputFormat(doc, null, true);
		//format.setLineSeparator("\r\n");
		//format.setLineWidth(72);
		OutputStream os=null;
		try {
			os=engine.getIOUtil().toBufferedOutputStream(searchFile.getOutputStream());
			StreamResult result = new StreamResult(os);
			XMLUtil.writeTo(doc.getDocumentElement(), result, false, true, null, null, null);
			//XMLSerializer serializer = new XMLSerializer(os, format);
			//serializer.serialize(doc.getDocumentElement());
		} catch (Exception e) {
		    throw new SearchException(e);
		}
		finally {
			engine.getIOUtil().closeSilent(os);
		}
    }

    /**
	 * if no search xml exist create a empty one
	 * @param searchFile 
	 * @throws IOException
	 */
	private final static void createSearchFile(Resource searchFile) throws IOException {
		CFMLEngine e = CFMLEngineFactory.getInstance();
		if(searchFile.isFile())searchFile.createFile(true);
		InputStream in = new ByteArrayInputStream(DEFAULT_SEARCH_XML.getBytes());
		//e.getClass().getResourceAsStream("/resource/search/default.xml");
		e.getIOUtil().copy(in,searchFile,true);
	}
    
    @Override
    public abstract String getDisplayName();

	public Config getConfig() { 
		return config;
	}
}