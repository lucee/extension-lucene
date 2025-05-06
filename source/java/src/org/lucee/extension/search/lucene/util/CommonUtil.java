package org.lucee.extension.search.lucene.util;

import java.io.Closeable;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lucee.runtime.search.SearchException;

public class CommonUtil {
	public static final Charset UTF8 = Charset.forName("UTF-8");
	private static final Map<String, String> tokens = new ConcurrentHashMap<>();

	public static void closeSilently(Closeable closable) {
		if (closable != null) {
			try {
				closable.close();

			} catch (Exception e) {

			}
		}

	}

	public static SearchException toSearchException(Exception cause) {
		if (cause instanceof SearchException)
			return (SearchException) cause;
		SearchException se = new SearchException(cause.getMessage());
		initCauseEL(se, cause);
		return se;
	}

	public static SearchException toSearchException(String message, Exception cause) {
		SearchException se = new SearchException(message);
		initCauseEL(se, cause);
		return se;
	}

	public static void initCauseEL(Throwable e, Throwable cause) {
		if (cause == null || e == cause)
			return;

		// get current root cause
		Throwable tmp;
		int count = 100;
		do {
			if (--count <= 0)
				break; // in case cause point to a child
			tmp = e.getCause();
			if (tmp == null)
				break;
			if (tmp == cause)
				return;
			e = tmp;
		} while (true);

		if (e == cause)
			return;
		// attach to root cause
		try {
			e.initCause(cause);
		} catch (Exception ex) {
		}
	}

	public static String createToken(String prefix, String name) {
		String str = prefix + ":" + name;
		String lock = tokens.putIfAbsent(str, str);
		if (lock == null) {
			lock = str;
		}
		return lock;
	}
}
