package org.lucee.extension.search.lucene.embedding;

import java.util.HashMap;
import java.util.Map;

import lucee.runtime.config.Config;
import lucee.runtime.type.Struct;

public final class SimpleTfIdfEmbeddingService extends EmbeddingServiceSupport {

	private static final int EMBEDDING_DIMENSION = 12;
	private final Map<String, Integer> vocabulary = new HashMap<>();
	private final Map<String, float[]> cache = new HashMap<>();

	@Override
	public void init(Config config, Struct parameters) {
		// Simple vocabulary with hardcoded indices
		String[] words = { "the", "quick", "brown", "fox", "jumps", "over", "lazy", "dog", "hello", "world", "lucee",
				"ai" };
		for (int i = 0; i < words.length; i++) {
			vocabulary.put(words[i], i);
		}
	}

	@Override
	public float[] generate(String text) {
		// Return cached embedding if exists
		if (cache.containsKey(text))
			return cache.get(text);

		float[] embedding = new float[EMBEDDING_DIMENSION];

		if (text != null && !text.isEmpty()) {
			String[] tokens = text.toLowerCase().split("\\s+");
			for (String token : tokens) {
				Integer index = vocabulary.get(token);
				if (index != null) {
					embedding[index] += 1.0f;
				}
			}
		}

		normalize(embedding);
		cache.put(text, embedding);
		return embedding;
	}

	private void normalize(float[] vec) {
		float norm = 0f;
		for (float v : vec)
			norm += v * v;
		norm = (float) Math.sqrt(norm);
		if (norm == 0f)
			return;
		for (int i = 0; i < vec.length; i++) {
			vec[i] /= norm;
		}
	}

	@Override
	public int getDimension() {
		return EMBEDDING_DIMENSION;
	}

	@Override
	public void close() {
		cache.clear();
	}
}
