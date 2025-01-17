package org.lucee.extension.search.lucene.util;

import java.io.Closeable;

public class CommonUtil {

	public static void closeSilently(Closeable closable) {
		if (closable != null) {
			try {
				closable.close();

			} catch (Exception e) {

			}
		}

	}

}
