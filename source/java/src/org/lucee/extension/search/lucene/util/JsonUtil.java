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
		hasCastJson = getCoreMajorVersion(engine) >= 7;
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

	/**
	 * Reads the major version of the running Lucee core via reflection. Lucee 9.0 changed the return
	 * type of {@code Info.getVersion()} (LDEV-6627), so a direct call compiled against an older loader
	 * would fail with NoSuchMethodError. Both version types provide {@code getMajor()}.
	 */
	private static int getCoreMajorVersion(CFMLEngine engine) {
		try {
			// resolve against the public API types, not the (possibly non-public) implementation classes
			java.lang.reflect.Method getVersion = lucee.Info.class.getMethod("getVersion");
			Object version = getVersion.invoke(engine.getInfo());
			return ((Number) getVersion.getReturnType().getMethod("getMajor").invoke(version)).intValue();
		}
		catch (ReflectiveOperationException e) {
			throw new RuntimeException("unable to read the Lucee core version", e);
		}
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
