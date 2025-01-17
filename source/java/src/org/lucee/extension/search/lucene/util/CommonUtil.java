package org.lucee.extension.search.lucene.util;

import java.io.Closeable;

import org.apache.lucene.util.Version;

public class CommonUtil {
	public static final Version VERSION = Version.LUCENE_5_5_5;

	public static void closeSilently(Closeable closable) {
		if (closable != null) {
			try {
				closable.close();

			} catch (Exception e) {

			}
		}

	}

}
