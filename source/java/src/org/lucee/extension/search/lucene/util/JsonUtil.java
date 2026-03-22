package org.lucee.extension.search.lucene.util;

import lucee.loader.engine.CFMLEngine;
import lucee.runtime.exp.PageException;
import lucee.runtime.ext.function.BIF;
import lucee.runtime.type.Struct;

/**
 * JSON utility that uses the Cast API on Lucee 7+ and falls back to BIF calls on Lucee 6.2.
 */
public class JsonUtil {

	private static volatile boolean detected;
	private static volatile boolean hasCastJson;
	private static volatile BIF serializeBIF;
	private static volatile BIF deserializeBIF;

	private static void detect(CFMLEngine engine) {
		if (detected) return;
		hasCastJson = engine.getInfo().getVersion().getMajor() >= 7;
		if (!hasCastJson) {
			try {
				serializeBIF = engine.getClassUtil().loadBIF(engine.getThreadPageContext(), "SerializeJSON");
				deserializeBIF = engine.getClassUtil().loadBIF(engine.getThreadPageContext(), "DeserializeJSON");
			} catch (InstantiationException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
		detected = true;
	}

	public static String serialize(CFMLEngine engine, Struct sct) throws PageException {
		detect(engine);
		if (hasCastJson) {
			return engine.getCastUtil().fromStructToJsonString(sct, false);
		}
		return (String) serializeBIF.invoke(engine.getThreadPageContext(), new Object[] { sct });
	}

	public static Struct deserialize(CFMLEngine engine, String json) throws PageException {
		detect(engine);
		if (hasCastJson) {
			return engine.getCastUtil().fromJsonStringToStruct(json);
		}
		return (Struct) deserializeBIF.invoke(engine.getThreadPageContext(), new Object[] { json });
	}
}
