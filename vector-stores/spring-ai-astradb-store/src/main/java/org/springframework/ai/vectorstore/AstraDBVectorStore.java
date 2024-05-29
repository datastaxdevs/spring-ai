/*
 * Copyright 2023 - 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.ai.vectorstore;

import com.datastax.astra.client.Collection;
import com.datastax.astra.client.DataAPIClient;
import com.datastax.astra.client.Database;
import com.datastax.astra.client.exception.DataApiException;
import com.datastax.astra.client.model.DataAPIKeywords;
import com.datastax.astra.client.model.Document;
import com.datastax.astra.client.model.Filter;
import com.datastax.astra.client.model.FindOptions;
import com.datastax.astra.internal.command.LoggingCommandObserver;
import com.dtsx.astra.sdk.utils.Assert;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.datastax.astra.client.exception.DataApiException.ERROR_CODE_INTERRUPTED;
import static com.datastax.astra.client.exception.DataApiException.ERROR_CODE_TIMEOUT;
import static com.datastax.astra.client.model.Filters.eq;
import static com.datastax.astra.internal.utils.AnsiUtils.magenta;
import static com.datastax.astra.internal.utils.AnsiUtils.yellow;
import static org.springframework.ai.vectorstore.AstraDBVectorStoreConfig.DEFAULT_OPERATION_TIMEOUT_IN_SECONDS;

/**
 * Implementation of {@link VectorStore} that uses AstraDB as the underlying storage.
 *
 * @author Cedrick Lunven
 */
@Slf4j
@Getter
public class AstraDBVectorStore implements VectorStore, InitializingBean {

	/**
	 * Hold a reference to the configuration used to initialized the Store
	 */
	private final AstraDBVectorStoreConfig config;

	/**
	 * Model used to convert document to vectors. Astra propose a default implementation
	 * where it can compute the embedding itself.
	 */
	private final EmbeddingModel embeddingModel;

	/**
	 * Main client to interact with AstraDB
	 */
	private final DataAPIClient dataAPIClient;

	/**
	 * Client to work with an Astra Collection
	 */
	private Collection<Document> astraDBCollection;

	/**
	 * Initialization of the store with an EXISTING collection.
	 * @param config configuration of the vector store
	 * @param embeddingModel embedding model
	 */
	public AstraDBVectorStore(AstraDBVectorStoreConfig config, EmbeddingModel embeddingModel) {
		this.config = config;
		this.embeddingModel = embeddingModel;
		this.dataAPIClient = new DataAPIClient(config.getToken(), config.getDataAPIOptions());
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// Stateless Http Client
		Database db = dataAPIClient.getDatabase(config.getApiEndpoint());

		// Enable Tracing if flag set in configuration
		if (config.isEnableLogging()) {
			config.getDataAPIOptions()
				.getObservers()
				.put("logger", new LoggingCommandObserver(AstraDBVectorStore.class));
		}

		// Validate credentials and settings + assess if collection exists.
		Set<String> collections = db.listCollectionNames().collect(Collectors.toSet());
		log.info("Connected to AstraDB. Available collections are {}", collections);
		if (!collections.contains(config.getCollectionName())) {
			if (this.config.isInitializeSchema()) {
				log.info("Collection {} does not exist and initializeSchema flag is set to true, creating it.",
						this.config.getCollectionName());
				astraDBCollection = db.createCollection(config.getCollectionName(), config.getCollectionOptions());
			}
			else {
				throw new IllegalArgumentException("Collection " + config.getCollectionName()
						+ " does not exist and flag 'initializeSchema' is false.");
			}
		}

		// Retrieving working collection
		astraDBCollection = db.getCollection(config.getCollectionName());
		log.info("Connected to AstraDB. Collection initialized {}", config.getCollectionName());
	}

	/**
	 * Mapping Spring => Astra.
	 * @param springDoc spring document representing embedding and Meta-data
	 * @return astra document astra document representing embedding and Meta-data
	 */
	private Document mapSpring2AstraDocument(org.springframework.ai.document.Document springDoc) {
		Assert.notNull(springDoc, "Astra Document must not be null");
		Document astraDoc = new Document();
		astraDoc.id(springDoc.getId());
		astraDoc.putAll(springDoc.getMetadata());
		// Map Vector
		if (springDoc.getEmbedding() != null) {
			float[] floatArray = new float[springDoc.getEmbedding().size()];
			for (int i = 0; i < springDoc.getEmbedding().size(); i++) {
				floatArray[i] = springDoc.getEmbedding().get(i).floatValue();
			}
			astraDoc.vector(floatArray);
		}
		if (springDoc.getContent() != null) {
			astraDoc.append(config.getEmbed(), springDoc.getContent());
			if (embeddingModel == null) {
				if (isVectorizedCollection()) {
					astraDoc.append(DataAPIKeywords.VECTORIZE.getKeyword(), springDoc.getContent());
				}
			}
			else {
				// Converting the content to a vector of float
				List<Double> doubles = embeddingModel.embed(springDoc.getContent());
				float[] queryVector = new float[doubles.size()];
				for (int i = 0; i < doubles.size(); i++) {
					queryVector[i] = doubles.get(i).floatValue();
				}
				astraDoc.append(DataAPIKeywords.VECTOR.getKeyword(), queryVector);
			}
		}
		return astraDoc;
	}

	private boolean isVectorizedCollection() {
		return astraDBCollection.getDefinition().getOptions() != null
				&& astraDBCollection.getDefinition().getOptions().getVector() != null
				&& astraDBCollection.getDefinition().getOptions().getVector().getService() != null
				&& astraDBCollection.getDefinition().getOptions().getVector().getService().getProvider() != null;
	}

	/**
	 * Mapping Astra => Spring .
	 * @param astraDoc astra document representing embedding and Meta-data
	 * @return spring document spring document representing embedding and Meta-data
	 */
	private org.springframework.ai.document.Document mapAstra2SpringDocument(Document astraDoc) {
		Assert.notNull(astraDoc, "Astra Document must not be null");
		org.springframework.ai.document.Document springDoc = new org.springframework.ai.document.Document(
				config.getEmbed(), astraDoc);
		astraDoc.getVector().ifPresent(v -> {
			List<Double> doubles = new ArrayList<>();
			for (float f : v)
				doubles.add((double) f);
			springDoc.setEmbedding(doubles);
		});
		return springDoc;
	}

	/** {@inheritDoc} */
	@Override
	public void add(List<org.springframework.ai.document.Document> documents) {
		if (documents != null && !documents.isEmpty()) {
			astraDBCollection.insertMany(documents.stream().map(this::mapSpring2AstraDocument).toList());
		}
	}

	/** {@inheritDoc} */
	@Override
	public Optional<Boolean> delete(List<String> idList) {
		if (idList != null && !idList.isEmpty()) {
			long start = System.currentTimeMillis();
			ExecutorService executor = Executors.newFixedThreadPool(8);
			try {
				idList.forEach(id -> executor.submit(() -> astraDBCollection.deleteOne(eq(id))));
				if (executor.awaitTermination(DEFAULT_OPERATION_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)) {
					log.debug(magenta(".[delete.responseTime]") + "=" + yellow("{}") + " millis.",
							System.currentTimeMillis() - start);
				}
				else {
					throw new DataApiException(ERROR_CODE_TIMEOUT,
							"Timeout when deleting " + idList.size() + " document(s).");
				}
			}
			catch (InterruptedException e) {
				log.error("Error while deleting to complete", e);
				throw new DataApiException(ERROR_CODE_INTERRUPTED, "Document deletion was interrupted");
			}
			finally {
				executor.shutdown();
			}
		}
		return Optional.of(false);
	}

	/**
	 * Clear the collection in an efficient way.
	 */
	public void clear() {
		getAstraDBCollection().deleteAll();
	}

	/** {@inheritDoc} */
	@Override
	public List<org.springframework.ai.document.Document> similaritySearch(SearchRequest request) {

		// Build the Filter
		Filter filter = AstraDBFilterMapper.mapFilter(request.getFilterExpression());

		// Add top_k and the similarity to all the similarity threshold filter
		FindOptions options = new FindOptions().limit(request.getTopK()).includeSimilarity();

		// If a query if provided, sort the results by similarity
		if (StringUtils.hasLength(request.getQuery())) {
			if (this.embeddingModel != null) {
				List<Double> queryEmbedding = this.embeddingModel.embed(request.getQuery());
				float[] queryVector = new float[queryEmbedding.size()];
				for (int i = 0; i < queryEmbedding.size(); i++) {
					queryVector[i] = queryEmbedding.get(i).floatValue();
				}
				options.sort(queryVector);
			}
			else {
				// Vectorize !
				if (isVectorizedCollection()) {
					options.sort(request.getQuery());
				}
				options.sort(request.getQuery());
			}
		}

		return astraDBCollection.find(filter, options)
			.all()
			.stream()
			// Implement Thresholds
			.filter(r -> r.getSimilarity().isPresent() && r.getSimilarity().get() >= request.getSimilarityThreshold())
			.map(this::mapAstra2SpringDocument)
			.toList();
	}

}