package org.lucee.extension.search.lucene.embedding;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lucee.commons.io.res.Resource;
import lucee.commons.io.res.filter.ResourceNameFilter;
import lucee.loader.util.Util;
import lucee.runtime.config.Config;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Struct;

/**
 * Word2Vec embedding service that loads GloVe or Word2Vec pre-trained vectors.
 * This service runs completely locally.
 */
public class Word2VecEmbeddingService extends EmbeddingServiceSupport {
	private Map<String, float[]> wordVectors;
	private int dimension;

	// https://nlp.stanford.edu/data/glove.6B.zip
	// https://nlp.stanford.edu/data/glove.42B.300d.zip
	// https://nlp.stanford.edu/data/glove.840B.300d.zip
	// https://nlp.stanford.edu/data/glove.840B.300d.zip
	// https://nlp.stanford.edu/data/glove.twitter.27B.zip

	// glove.6B.300d glove.6B.50d glove.6B.200d glove.6B.100d

	@Override
	public void init(Config config, Struct parameters) throws IOException {
		super.init(config, parameters);
		String charset = eng.getCastUtil().toString(parameters.get("charset", null), null);
		if (Util.isEmpty(charset, true)) {
			charset = "UTF-8";
		}

		String strVectorsFile = eng.getCastUtil().toString(parameters.get("vectorsFile", null), null);
		// strVectorsFile =
		// "/Users/mic/Test/test-cfconfig/lucee-server/context/search/embedding/glove.6B.100d.txt";

		Resource vectorsResource = null;
		if (Util.isEmpty(strVectorsFile, true)) {
			Resource vectorsResourceDir = config.getConfigDir().getRealResource("search/embedding/");
			Resource[] children = vectorsResourceDir.listResources(new ResourceNameFilter() {

				@Override
				public boolean accept(Resource parent, String name) {
					return name.endsWith(".txt");
				}
			});
			if (children != null) {
				for (Resource c : children) {
					if (vectorsResource == null || vectorsResource.length() < c.length())
						vectorsResource = c;
				}
			}
			if (vectorsResource == null) {
				throw new IOException("Word2Vec embedding service error: No vector files found. "
						+ "To use this service, you need to provide pre-trained word embedding vectors. "
						+ "Options to resolve this issue: "
						+ "1. Download pre-trained GloVe vectors from: https://nlp.stanford.edu/projects/glove/ "
						+ "2. Place the extracted .txt files in: lucee-server/context/search/embedding/ ");
				// + "3. Alternatively, specify a direct path to vectors using the 'vectorsFile'
				// parameter");
			}

		} else {
			try {
				vectorsResource = eng.getResourceUtil().toResourceExisting(eng.getThreadPageContext(), strVectorsFile);
			} catch (PageException e) {
				throw eng.getExceptionUtil().toIOException(e);
			}
		}

		this.wordVectors = new HashMap<>();

		int dims = 0;

		try (BufferedReader reader = (BufferedReader) eng.getIOUtil()
				.toBufferedReader(new InputStreamReader(vectorsResource.getInputStream(), charset))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] parts = line.trim().split("\\s+");
				if (parts.length < 2)
					continue;

				String word = parts[0];
				float[] vector = new float[parts.length - 1];

				for (int i = 1; i < parts.length; i++) {
					vector[i - 1] = Float.parseFloat(parts[i]);
				}

				wordVectors.put(word, vector);
				if (dims == 0) {
					dims = vector.length;
				}
			}
		}

		this.dimension = dims;
	}

	@Override
	public float[] generate(String text) {
		if (text == null || text.isEmpty()) {
			return new float[dimension];
		}

		// Tokenize
		String[] tokens = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").trim().split("\\s+");

		if (tokens.length == 0) {
			return new float[dimension];
		}

		// Calculate average of word vectors
		float[] embedding = new float[dimension];
		int count = 0;

		for (String token : tokens) {
			float[] vector = wordVectors.get(token);
			if (vector != null) {
				for (int i = 0; i < dimension; i++) {
					embedding[i] += vector[i];
				}
				count++;
			}
		}

		// Average
		if (count > 0) {
			for (int i = 0; i < dimension; i++) {
				embedding[i] /= count;
			}
		}

		return embedding;
	}

	/**
	 * Calculate cosine similarity between two vectors.
	 */
	public float cosineSimilarity(float[] vec1, float[] vec2) {
		float dotProduct = 0.0f;
		float norm1 = 0.0f;
		float norm2 = 0.0f;

		for (int i = 0; i < vec1.length; i++) {
			dotProduct += vec1[i] * vec2[i];
			norm1 += vec1[i] * vec1[i];
			norm2 += vec2[i] * vec2[i];
		}

		return (float) (dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2)));
	}

	/**
	 * Find most similar words to a query.
	 */
	public List<Map.Entry<String, Float>> findSimilarWords(String word, int topN) {
		float[] queryVector = wordVectors.get(word.toLowerCase());
		if (queryVector == null) {
			return new ArrayList<>();
		}

		List<Map.Entry<String, Float>> similarities = new ArrayList<>();

		for (Map.Entry<String, float[]> entry : wordVectors.entrySet()) {
			if (!entry.getKey().equals(word)) {
				float similarity = cosineSimilarity(queryVector, entry.getValue());
				similarities.add(Map.entry(entry.getKey(), similarity));
			}
		}

		similarities.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
		return similarities.subList(0, Math.min(topN, similarities.size()));
	}

	/**
	 * Get the dimension of embeddings.
	 */
	@Override
	public int getDimension() {
		return dimension;
	}

	/**
	 * Get the number of words in the vocabulary.
	 */
	public int getVocabularySize() {
		return wordVectors.size();
	}

	@Override
	public void close() {

	}
}
