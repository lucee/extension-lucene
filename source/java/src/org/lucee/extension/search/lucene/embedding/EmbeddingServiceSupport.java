package org.lucee.extension.search.lucene.embedding;

import java.io.IOException;

import lucee.commons.io.log.Log;
import lucee.loader.engine.CFMLEngine;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.runtime.config.Config;
import lucee.runtime.type.Struct;

public abstract class EmbeddingServiceSupport implements EmbeddingService {

	protected CFMLEngine eng;
	private Log log;

	@Override
	public void init(Config config, Struct parameters) throws IOException {
		eng = CFMLEngineFactory.getInstance();
		log = config.getLog(eng.getCastUtil().toString(parameters.get("log", null), "search"));
	}

	@Override
	public void close() {

	}

	public Log getLog() {
		return log;
	}

}
