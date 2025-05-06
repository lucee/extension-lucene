package org.lucee.extension.search.lucene.embedding;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import lucee.commons.io.log.Log;
import lucee.commons.io.res.Resource;
import lucee.loader.engine.CFMLEngineFactory;
import lucee.loader.util.Util;
import lucee.runtime.config.Config;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Struct;

// https://raw.githubusercontent.com/dwyl/english-words/refs/heads/master/words_alpha.txt
// https://raw.githubusercontent.com/dwyl/english-words/refs/heads/master/words.zip
// https://github.com/oprogramador/most-common-words-by-language/tree/master/src/resources
/**
 * A TF-IDF embedding service implementation that doesn't rely on any external
 * mathematical libraries.
 */
public final class TfIdfEmbeddingService extends EmbeddingServiceSupport {

	private static final int DEFAULT_EMBEDDING_DIMENSION = 100;
	// private static final String DEFAULT_EMBEDDING_PATH =
	// "https://raw.githubusercontent.com/dwyl/english-words/refs/heads/master/words_alpha.txt";

	private Map<String, Integer> vocabulary;
	private Map<String, Double> idf;
	private int embeddingDimension;
	private Map<String, float[]> embeddingCache;
	private int documentCount;
	private String language;

	@Override
	public void init(Config config, Struct parameters) throws IOException {
		super.init(config, parameters);

		this.vocabulary = new HashMap<>();
		this.idf = new HashMap<>();
		this.embeddingCache = new ConcurrentHashMap<>();
		this.documentCount = 1;

		// TODO read from env var

		// language
		language = eng.getCastUtil().toString(parameters.get("language", "english"), "english").toLowerCase().trim();
		if (Util.isEmpty(language))
			language = "english";

		// embeddingDimension
		embeddingDimension = eng.getCastUtil().toIntValue(parameters.get("embeddingDimension", null),
				DEFAULT_EMBEDDING_DIMENSION);

		String embeddingPath = eng.getCastUtil().toString(parameters.get("embeddingPath", null), null);

		try {
			Resource embeddingResource;

			// nothing defined, we used the bundled version
			if (Util.isEmpty(embeddingPath, true)) {
				String lang = "english";
				embeddingResource = config.getConfigDir().getRealResource("search/embedding/words/" + lang + ".txt");
				extractEmbeddingPath(embeddingResource);

				if (!"english".equals(language)) {
					throw new IOException(
							"TF-IDF embedding service only provides words for english, for other languages you need to provide words,"
									+ "to do so you can for example download them from here [https://github.com/oprogramador/most-common-words-by-language/tree/master/src/resources]"
									+ "and then copy to [lucee-server/context/search/embedding/words/" + language
									+ ".txt], you will find an example for english in that directory.");
				}

			}
			// we use the version defined
			else {
				embeddingResource = CFMLEngineFactory.getInstance().getCastUtil().toResource(embeddingPath);
			}

			// is it a remote file (s3,http,...) we copy it lokal
			boolean compressed = embeddingResource.getName().endsWith(".gz")
					|| embeddingResource.getName().endsWith(".gzip");
			if (!"file".equalsIgnoreCase(embeddingResource.getResourceProvider().getScheme()) || compressed) {
				Resource local = config.getConfigDir().getRealResource("search/embedding/words/" + language + ".txt");
				local.getParentResource().mkdirs();
				// copy it lokal, in case it is a gzip, we uncompress it
				// TODO support other compress formats
				CFMLEngineFactory.getInstance().getIOUtil()
						.copy(compressed ? new GZIPInputStream(embeddingResource.getInputStream())
								: embeddingResource.getInputStream(), local, true);
				embeddingResource = local;
			}

			File embeddingFile = CFMLEngineFactory.getInstance().getCastUtil().toFile(embeddingResource);

			loadVocabularyFromFile(embeddingFile.toPath());

		} catch (PageException e) {
			throw CFMLEngineFactory.getInstance().getExceptionUtil().toIOException(e);
		}

	}

	private Resource extractEmbeddingPath(Resource destination) throws IOException {
		// Get the input stream from the JAR resource
		InputStream is = null;
		try {
			is = getClass().getResourceAsStream("resources/embedding/words/english.txt.gz");
			if (is == null) {
				is = getClass().getResourceAsStream("/resources/embedding/words/english.txt.gz");
			}
			if (is == null) {
				throw new IOException("Could not find embedded resource: /resources/embedding/words/english.txt.gz");
			}

			destination.getParentResource().mkdirs();

			// Decompress directly to the destination
			CFMLEngineFactory.getInstance().getIOUtil().copy(new GZIPInputStream(is), destination, true);

			return destination;
		} finally {
			Util.closeEL(is);
		}
	}

	@Override
	public float[] generate(String text) {
		// Added safety check for null text
		if (text == null) {
			text = "";
		}

		// Check cache first
		if (embeddingCache.containsKey(text)) {
			return embeddingCache.get(text);
		}

		// Tokenize the text
		List<String> tokens = tokenize(text);

		// Calculate term frequencies
		Map<String, Integer> termFrequencies = new HashMap<>();
		for (String token : tokens) {
			termFrequencies.put(token, termFrequencies.getOrDefault(token, 0) + 1);
		}

		// Create the TF-IDF vector with safety check for dimension
		float[] embedding = new float[embeddingDimension];

		// Fill embedding vector
		for (Map.Entry<String, Integer> entry : termFrequencies.entrySet()) {
			String term = entry.getKey();
			int tf = entry.getValue();

			Integer index = vocabulary.get(term);
			if (index != null && index < embeddingDimension) {
				// Get IDF or use default if not found
				double idfValue = idf.getOrDefault(term, 1.0);
				embedding[index] = (float) (tf * idfValue);
			}
		}

		// Enhanced normalization
		normalizeVector(embedding);

		// Check for NaN or Infinity after normalization
		for (int i = 0; i < embedding.length; i++) {
			if (Float.isNaN(embedding[i]) || Float.isInfinite(embedding[i])) {
				embedding[i] = 0.0f;
			}
		}

		// Add minimal non-zero values to ensure vector isn't all zeros
		boolean allZeros = true;
		for (float val : embedding) {
			if (val != 0.0f) {
				allZeros = false;
				break;
			}
		}

		if (allZeros) {
			// Add a small value to first element to avoid all-zero vectors
			embedding[0] = 0.00001f;
		}

		// Cache the result
		embeddingCache.put(text, embedding);

		return embedding;
	}

	/**
	 * Enhanced normalization with better handling of edge cases
	 */
	private void normalizeVector(float[] vector) {
		// Calculate L2 norm (Euclidean length)
		float norm = 0;
		for (float v : vector) {
			norm += v * v;
		}

		// Only normalize if norm is significant (avoid division by very small numbers)
		if (norm > 0.000001f) {
			norm = (float) Math.sqrt(norm);
			for (int i = 0; i < vector.length; i++) {
				vector[i] /= norm;
			}
		} else if (norm > 0) {
			// For very small norms, use a more stable approach
			for (int i = 0; i < vector.length; i++) {
				if (vector[i] != 0) {
					vector[i] = vector[i] > 0 ? 1.0f : -1.0f;
					break; // Just set one non-zero element
				}
			}
		}
	}

	/**
	 * Load vocabulary and IDF values from a file.
	 */
	private void loadVocabularyFromFile(Path vocabFile) throws IOException {
		getLog().log(Log.LEVEL_INFO, "search-embedding", "Loading vocabulary from file: " + vocabFile);

		List<String> allWords = Files.readAllLines(vocabFile);

		// Filter out empty lines
		allWords = allWords.stream().map(String::trim).filter(line -> !line.isEmpty()).collect(Collectors.toList());

		getLog().log(Log.LEVEL_INFO, "search-embedding", "Found " + allWords.size() + " words in vocabulary file");

		// Limit to embeddingDimension
		int limit = Math.min(allWords.size(), embeddingDimension);

		for (int i = 0; i < limit; i++) {
			String term = allWords.get(i);
			vocabulary.put(term, i);

			// Calculate IDF value based on position in list
			// Words earlier in the list are presumed more common and get lower IDF values
			double idfValue = 1.0 + Math.log(1.0 + (double) i / limit * 10.0);
			idf.put(term, idfValue);
		}

		// Set document count based on vocabulary size
		documentCount = Math.max(allWords.size() * 10, 1000);

		getLog().log(Log.LEVEL_INFO, "search-embedding",
				"Initialized vocabulary with " + limit + " words out of " + allWords.size() + " total words");
	}

	/**
	 * Tokenize text into words.
	 */
	private List<String> tokenize(String text) {
		if (text == null || text.isEmpty()) {
			return new ArrayList<>();
		}

		if ("english".equals(language)) {
			// Simple tokenization for English - lowercase, remove punctuation, split on
			// whitespace
			String preprocessed = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();

			if (preprocessed.isEmpty()) {
				return new ArrayList<>();
			}

			return Arrays.asList(preprocessed.split(" "));
		} else {
			// For future multilingual support
			// Basic fallback for all languages
			String preprocessed = text.toLowerCase().replaceAll("[^\\p{L}\\p{N}\\s]", " ") // Unicode-aware pattern for
																							// letters and numbers
					.replaceAll("\\s+", " ").trim();

			if (preprocessed.isEmpty()) {
				return new ArrayList<>();
			}

			return Arrays.asList(preprocessed.split(" "));
		}
	}

	/**
	 * Save vocabulary and IDF values to a file for future use.
	 */
	public void saveVocabulary(String filePath) throws IOException {
		Path output = Paths.get(filePath);
		StringBuilder content = new StringBuilder();

		// Write document count
		content.append("DOCUMENT_COUNT: ").append(documentCount).append("\n");

		// Write vocabulary terms and IDF values
		for (Map.Entry<String, Integer> entry : vocabulary.entrySet()) {
			String term = entry.getKey();
			double idfValue = idf.getOrDefault(term, 1.0);
			content.append(term).append("\t").append(idfValue).append("\n");
		}

		Files.writeString(output, content.toString());
	}

	@Override
	public int getDimension() {
		return embeddingDimension;
	}

	@Override
	public void close() {
		// Clear caches
		embeddingCache.clear();
	}

	/**
	 * Calculate cosine similarity between two vectors.
	 */
	public float cosineSimilarity(float[] vec1, float[] vec2) {
		if (vec1 == null || vec2 == null || vec1.length != vec2.length) {
			return 0;
		}

		float dotProduct = 0;
		float norm1 = 0;
		float norm2 = 0;

		for (int i = 0; i < vec1.length; i++) {
			dotProduct += vec1[i] * vec2[i];
			norm1 += vec1[i] * vec1[i];
			norm2 += vec2[i] * vec2[i];
		}

		// Avoid division by zero
		if (norm1 == 0 || norm2 == 0) {
			return 0;
		}

		return (float) (dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2)));
	}
}