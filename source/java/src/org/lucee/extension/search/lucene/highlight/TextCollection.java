package org.lucee.extension.search.lucene.highlight;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.lucee.extension.search.lucene.highlight.TextHandler.ScoredParagraph;

import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.PageContext;
import lucee.runtime.dump.DumpData;
import lucee.runtime.dump.DumpProperties;
import lucee.runtime.dump.DumpTable;
import lucee.runtime.exp.PageException;
import lucee.runtime.op.Castable;
import lucee.runtime.type.Collection;
import lucee.runtime.type.Query;
import lucee.runtime.type.Struct;
import lucee.runtime.type.dt.DateTime;
import lucee.runtime.util.Creation;

public class TextCollection implements Castable, Struct {

	private static final long serialVersionUID = 5596686334305935344L;
	private List<ScoredParagraph> bestParagraphs;
	private String delimeter;
	private String text;
	private Struct data;
	private int length;
	private String content;

	public TextCollection(String content, List<ScoredParagraph> bestParagraphs, String delimeter, int length) {
		this.content = content;
		this.bestParagraphs = bestParagraphs;
		this.delimeter = delimeter;
		this.length = length;

	}

	private Struct data() {
		CFMLEngine engine = CFMLEngineFactory.getInstance();
		Creation creator = engine.getCreationUtil();
		if (data == null) {
			data = engine.getCreationUtil().createStruct(Struct.TYPE_LINKED);

			Key _original = creator.createKey("original");
			Key _highlighted = creator.createKey("highlighted");
			Key _start = creator.createKey("start");
			Key _end = creator.createKey("end");
			Key _score = creator.createKey("score");
			Key _passages = creator.createKey("passages");
			Key _content = creator.createKey("content");

			// passages
			Query passages;
			try {
				passages = engine.getCreationUtil().createQuery(
						new Key[] { _original, _highlighted, _start, _end, _score }, bestParagraphs.size(), "passages");
			} catch (PageException e) {
				throw engine.getCastUtil().toPageRuntimeException(e);
			}
			int row = 0;
			for (ScoredParagraph sp : bestParagraphs) {
				row++;
				passages.setAtEL(_original, row, sp.original.text);
				passages.setAtEL(_highlighted, row, sp.highlighted);
				passages.setAtEL(_start, row, sp.original.start);
				passages.setAtEL(_end, row, sp.original.end);
				passages.setAtEL(_score, row, sp.score);
			}
			data.setEL(_passages, passages);
			data.setEL(_content, content);
		}

		return data;
	}

	@Override
	public String castToString() throws PageException {
		return castToString(null);
	}

	@Override
	public String castToString(String defaultValue) {
		if (text == null) {
			StringBuilder sb = new StringBuilder();
			for (ScoredParagraph sp : bestParagraphs) {
				if (sb.length() > 0)
					sb.append(delimeter);
				sb.append(sp.highlighted);
				if (sb.length() >= length)
					break;
			}
			text = sb.toString();
		}
		return text;
	}

	@Override
	public String toString() {
		return castToString(null);
	}

	@Override
	public boolean castToBooleanValue() throws PageException {
		return CFMLEngineFactory.getInstance().getCastUtil().toBooleanValue(castToString());
	}

	@Override
	public Boolean castToBoolean(Boolean defaultValue) {
		return defaultValue;
	}

	@Override
	public double castToDoubleValue() throws PageException {
		return CFMLEngineFactory.getInstance().getCastUtil().toDoubleValue(castToString());
	}

	@Override
	public double castToDoubleValue(double defaultValue) {
		return defaultValue;
	}

	@Override
	public DateTime castToDateTime() throws PageException {
		return CFMLEngineFactory.getInstance().getCastUtil().toDateTime(castToString(), (TimeZone) null);
	}

	@Override
	public DateTime castToDateTime(DateTime defaultValue) {
		return defaultValue;
	}

	@Override
	public int compareTo(String str) throws PageException {
		return castToString().compareTo(str);
	}

	@Override
	public int compareTo(boolean b) throws PageException {
		return CFMLEngineFactory.getInstance().getOperatonUtil().compare(castToBooleanValue(), b);
	}

	@Override
	public int compareTo(double d) throws PageException {
		return CFMLEngineFactory.getInstance().getOperatonUtil().compare(castToDoubleValue(), d);
	}

	@Override
	public int compareTo(DateTime dt) throws PageException {
		return CFMLEngineFactory.getInstance().getOperatonUtil().compare(castToDateTime(), dt);
	}

	@Override
	public DumpData toDumpData(PageContext pageContext, int maxlevel, DumpProperties properties) {
		DumpData dd = data().toDumpData(pageContext, maxlevel, properties);
		if (dd instanceof DumpTable) {
			DumpTable dt = (DumpTable) dd;
			dt.setTitle("Search Context Struct");
			dt.setComment(
					"Legacy-compatible Struct for cfsearch context column. Maintains backward compatibility by functioning as both Struct and String value.");
		}
		return dd;
	}

	@Override
	public Iterator<Key> keyIterator() {
		return data().keyIterator();
	}

	@Override
	public Iterator<String> keysAsStringIterator() {
		return data().keysAsStringIterator();
	}

	@Override
	public Iterator<Object> valueIterator() {
		return data().valueIterator();
	}

	@Override
	public Iterator<Entry<Key, Object>> entryIterator() {
		return data().entryIterator();
	}

	@Override
	public Iterator<?> getIterator() {
		return data().getIterator();
	}

	@Override
	public int size() {
		return data().size();
	}

	@Override
	public Key[] keys() {
		return data().keys();
	}

	@Override
	public Object remove(Key key) throws PageException {
		return data().remove(key);
	}

	@Override
	public Object removeEL(Key key) {
		return data().removeEL(key);
	}

	@Override
	public Object remove(Key key, Object defaultValue) {
		return data().remove(key, defaultValue);
	}

	@Override
	public void clear() {
		data().clear();
	}

	@Override
	public Object get(String key) throws PageException {
		return data().get(key);
	}

	@Override
	public Object get(Key key) throws PageException {
		return data().get(key);
	}

	@Override
	public Object get(String key, Object defaultValue) {
		return data().get(key, defaultValue);
	}

	@Override
	public Object get(Key key, Object defaultValue) {
		return data().setEL(key, defaultValue);
	}

	@Override
	public Object set(String key, Object value) throws PageException {
		return data().set(key, value);
	}

	@Override
	public Object set(Key key, Object value) throws PageException {
		return data().set(key, value);
	}

	@Override
	public Object setEL(String key, Object value) {
		return data().setEL(key, value);
	}

	@Override
	public Object setEL(Key key, Object value) {
		return data().setEL(key, value);
	}

	@Override
	public Collection duplicate(boolean deepCopy) {
		return data().duplicate(deepCopy);
	}

	@Override
	public boolean containsKey(String key) {
		return data().containsKey(key);
	}

	@Override
	public boolean containsKey(Key key) {
		return data().containsKey(key);
	}

	@Override
	public Object clone() {
		// TODO
		return this;
	}

	@Override
	public Object get(PageContext pc, Key key, Object defaultValue) {
		return data().get(pc, key, defaultValue);
	}

	@Override
	public Object get(PageContext pc, Key key) throws PageException {
		return data().get(pc, key);
	}

	@Override
	public Object set(PageContext pc, Key propertyName, Object value) throws PageException {
		return data().set(pc, propertyName, value);
	}

	@Override
	public Object setEL(PageContext pc, Key propertyName, Object value) {
		return data().setEL(pc, propertyName, value);
	}

	@Override
	public Object call(PageContext pc, Key methodName, Object[] arguments) throws PageException {
		return data().call(pc, methodName, arguments);
	}

	@Override
	public Object callWithNamedValues(PageContext pc, Key methodName, Struct args) throws PageException {
		return data().callWithNamedValues(pc, methodName, args);
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsKey(Object key) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsValue(Object value) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object get(Object key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object put(Object key, Object value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object remove(Object key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void putAll(Map m) {
		// TODO Auto-generated method stub

	}

	@Override
	public Set keySet() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public java.util.Collection values() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set entrySet() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean containsKey(PageContext pc, Key key) {
		return containsKey(key);
	}
}
