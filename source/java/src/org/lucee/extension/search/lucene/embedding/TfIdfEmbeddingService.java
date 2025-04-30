package org.lucee.extension.search.lucene.embedding;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import lucee.commons.io.log.Log;
import lucee.loader.util.Util;
import lucee.runtime.config.Config;
import lucee.runtime.type.Struct;

/**
 * A TF-IDF embedding service implementation that doesn't rely on any external
 * mathematical libraries.
 */
public final class TfIdfEmbeddingService extends EmbeddingServiceSupport {

	private static int EMBEDDING_DIMENSION = 500;

	private Map<String, Integer> vocabulary;
	private Map<String, Double> idf;
	private int embeddingDimension;
	private Map<String, float[]> embeddingCache;
	private int documentCount;

	@Override
	public void init(Config config, Struct parameters) throws IOException {
		super.init(config, parameters);

		// load config
		// TODO it somehow
		String embeddingPath = eng.getCastUtil().toString(parameters.get("embeddingPath", null), null);
		embeddingDimension = eng.getCastUtil().toIntValue(parameters.get("embeddingPath", null), EMBEDDING_DIMENSION);

		this.vocabulary = new HashMap<>();
		this.idf = new HashMap<>();
		this.embeddingCache = new ConcurrentHashMap<>();
		this.documentCount = 1; // Default to 1 to avoid division by zero

		// Try to load vocabulary and IDF values from the specified path
		if (!Util.isEmpty(embeddingPath, true)) {
			try {
				loadVocabulary(embeddingPath);
			} catch (Exception e) {
				getLog().log(Log.LEVEL_ERROR, "search-embedding", "search-embedding", e);
				// Fall back to default initialization
				initializeDefaultVocabulary();
			}
		} else {
			initializeDefaultVocabulary();
		}
	}

	/**
	 * Initialize with a default vocabulary.
	 */
	private void initializeDefaultVocabulary() {
		getLog().log(Log.LEVEL_INFO, "search-embedding", "Initializing default TF-IDF vocabulary");

		// Add common English words as default vocabulary
		String[] commonWords = { "the", "of", "and", "a", "to", "in", "is", "you", "that", "it", "he", "was", "for",
				"on", "are", "as", "with", "his", "they", "at", "be", "this", "have", "from", "or", "one", "had", "by",
				"word", "but", "not", "what", "all", "were", "we", "when", "your", "can", "said", "there", "use", "an",
				"each", "which", "she", "do", "how", "their", "if", "will", "up", "other", "about", "out", "many",
				"then", "them", "these", "so", "some", "her", "would", "make", "like", "him", "into", "time", "has",
				"look", "two", "more", "write", "go", "see", "number", "no", "way", "could", "people", "my", "than",
				"first", "water", "been", "call", "who", "oil", "its", "now", "find", "long", "down", "day", "did",
				"get", "come", "made", "may", "part" };

		// Limit to embedding dimension or array length, whichever is smaller
		int limit = Math.min(commonWords.length, embeddingDimension);

		for (int i = 0; i < limit; i++) {
			vocabulary.put(commonWords[i], i);
			// Assign artificial IDF values (more common words get lower IDF)
			double artificialIdf = 1.0 + (0.5 * i / limit);
			idf.put(commonWords[i], artificialIdf);
		}

		documentCount = 1000; // Artificial document count
	}

	/**
	 * Load vocabulary and IDF values from a file or directory.
	 */
	private void loadVocabulary(String path) throws IOException {
		Path embeddingPath = Paths.get(path);

		if (Files.isDirectory(embeddingPath)) {
			// If it's a directory, analyze files to build vocabulary
			buildVocabularyFromCorpus(embeddingPath);
		} else if (Files.isRegularFile(embeddingPath)) {
			// If it's a file, assume it's a vocabulary file
			loadVocabularyFromFile(embeddingPath);
		} else {
			throw new IOException("Embedding path is neither a valid directory nor a file");
		}
	}

	/**
	 * Build vocabulary by analyzing a corpus of documents.
	 */
	private void buildVocabularyFromCorpus(Path corpusDir) throws IOException {
		getLog().log(Log.LEVEL_INFO, "search-embedding", "Building vocabulary from corpus: " + corpusDir);

		// Collect document frequencies
		Map<String, Integer> docFrequencies = new HashMap<>();

		// Process each text file in the directory
		try (var files = Files.walk(corpusDir)) {
			List<Path> textFiles = files.filter(Files::isRegularFile)
					.filter(p -> p.toString().endsWith(".txt") || p.toString().endsWith(".text"))
					.collect(Collectors.toList());
			documentCount = textFiles.size();

			for (Path file : textFiles) {
				String content = Files.readString(file);
				Set<String> uniqueWords = new HashSet<>(tokenize(content));

				// Update document frequencies
				for (String word : uniqueWords) {
					docFrequencies.put(word, docFrequencies.getOrDefault(word, 0) + 1);
				}
			}
		}

		// Select top terms by frequency to build vocabulary (up to embeddingDimension)
		List<Map.Entry<String, Integer>> sortedTerms = new ArrayList<>(docFrequencies.entrySet());
		sortedTerms.sort((a, b) -> b.getValue().compareTo(a.getValue()));

		// Build vocabulary with indices
		int limit = Math.min(sortedTerms.size(), embeddingDimension);
		for (int i = 0; i < limit; i++) {
			String term = sortedTerms.get(i).getKey();
			vocabulary.put(term, i);

			// Calculate IDF for each term
			int df = docFrequencies.get(term);
			double idfValue = Math.log((double) documentCount / df) + 1.0;
			idf.put(term, idfValue);
		}
	}

	/**
	 * Load vocabulary and IDF values from a file.
	 */
	private void loadVocabularyFromFile(Path vocabFile) throws IOException {
		getLog().log(Log.LEVEL_INFO, "search-embedding", "Loading vocabulary from file: " + vocabFile);

		try (BufferedReader reader = new BufferedReader(new FileReader(vocabFile.toFile()))) {
			String line;
			int index = 0;

			// First line might be a header with document count
			line = reader.readLine();
			if (line.startsWith("DOCUMENT_COUNT:")) {
				documentCount = Integer.parseInt(line.substring(15).trim());
				line = reader.readLine();
			}

			// Read vocabulary terms and their IDF values
			while (line != null && index < embeddingDimension) {
				String[] parts = line.split("\\t");
				if (parts.length >= 2) {
					String term = parts[0];
					double idfValue = Double.parseDouble(parts[1]);

					vocabulary.put(term, index);
					idf.put(term, idfValue);
					index++;
				}

				line = reader.readLine();
			}
		}
	}

	@Override
	public float[] generate(String text) {
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

		// Create the TF-IDF vector
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

		// Normalize the vector (L2 normalization)
		normalizeVector(embedding);

		// Cache the result
		embeddingCache.put(text, embedding);

		return embedding;
	}

	/**
	 * Tokenize text into words.
	 */
	private List<String> tokenize(String text) {
		if (text == null || text.isEmpty()) {
			return new ArrayList<>();
		}

		// Simple tokenization - lowercase, remove punctuation, split on whitespace
		String preprocessed = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();

		if (preprocessed.isEmpty()) {
			return new ArrayList<>();
		}

		return Arrays.asList(preprocessed.split(" "));
	}

	/**
	 * Normalize a vector to unit length (L2 normalization).
	 */
	private void normalizeVector(float[] vector) {
		// Calculate L2 norm (Euclidean length)
		float norm = 0;
		for (float v : vector) {
			norm += v * v;
		}

		// Only normalize if norm is non-zero
		if (norm > 0) {
			norm = (float) Math.sqrt(norm);
			for (int i = 0; i < vector.length; i++) {
				vector[i] /= norm;
			}
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