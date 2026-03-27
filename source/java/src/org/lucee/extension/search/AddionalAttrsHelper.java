package org.lucee.extension.search;

import java.lang.reflect.Method;

/**
 * Reads context highlighting attributes from core's AddionalAttrs thread-local
 * via reflection, bridging the OSGi classloader boundary.
 *
 * Cannot cache Method handles at class-load time because the core classloader
 * may not be available yet. Instead, resolves on each call using the thread's
 * context classloader (which core sets before calling into the extension).
 */
public final class AddionalAttrsHelper {

	private static final String CLASS_NAME = "lucee.runtime.search.AddionalAttrs";

	private static Object getAddionalAttrs() throws Exception {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Class<?> clazz = Class.forName(CLASS_NAME, true, cl);
		Method m = clazz.getMethod("getAddionlAttrs");
		return m.invoke(null);
	}

	public static int getContextBytes( int defaultValue ) {
		try {
			Object aa = getAddionalAttrs();
			return (Integer) aa.getClass().getMethod("getContextBytes").invoke(aa);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static int getContextPassages( int defaultValue ) {
		try {
			Object aa = getAddionalAttrs();
			return (Integer) aa.getClass().getMethod("getContextPassages").invoke(aa);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static int getContextPassageLength( int defaultValue ) {
		try {
			Object aa = getAddionalAttrs();
			return (Integer) aa.getClass().getMethod("getContextPassageLength").invoke(aa);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static String getContextHighlightBegin( String defaultValue ) {
		try {
			Object aa = getAddionalAttrs();
			String val = (String) aa.getClass().getMethod("getContextHighlightBegin").invoke(aa);
			return (val != null && !val.isEmpty()) ? val : defaultValue;
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static String getContextHighlightEnd( String defaultValue ) {
		try {
			Object aa = getAddionalAttrs();
			String val = (String) aa.getClass().getMethod("getContextHighlightEnd").invoke(aa);
			return (val != null && !val.isEmpty()) ? val : defaultValue;
		} catch (Exception e) {
			return defaultValue;
		}
	}
}
