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
import com.datastax.astra.client.Database;
import com.datastax.astra.client.exception.DataApiException;
import com.datastax.astra.client.model.CollectionOptions;
import com.datastax.astra.client.model.Document;
import com.datastax.astra.client.model.Filter;
import com.datastax.astra.client.model.FindOptions;
import com.datastax.astra.client.model.InsertManyOptions;
import com.datastax.astra.client.model.SimilarityMetric;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingClient;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.datastax.astra.client.exception.DataApiException.ERROR_CODE_INTERRUPTED;
import static com.datastax.astra.client.exception.DataApiException.ERROR_CODE_TIMEOUT;
import static com.datastax.astra.client.model.Filters.eq;
import static com.datastax.astra.internal.utils.AnsiUtils.magenta;
import static com.datastax.astra.internal.utils.AnsiUtils.yellow;

/**
 * Implementation of {@link VectorStore} that uses AstraDB as the underlying storage.
 *
 * @author Cedrick Lunven
 */
@Slf4j
public class AstraDBVectorStore implements VectorStore {

	/**
	 * Saving the text chunk as an attribute.
	 */
	public static final String CONTENT = "content";

	/**
	 * Client to work with an Astra Collection
	 */
	private final Collection<Document> astraDBCollection;

	private final EmbeddingClient embeddingClient;

	/**
	 * Bulk loading are processed in chunks, size of 1 chunk in between 1 and 20
	 */
	private final InsertManyOptions insertManyOptions;

	private final int concurrentThreads;

	private final int operationTimeout;

	/**
	 * Initialization of the store with an EXISTING collection.
	 * @param db Astra Database (load manually of from autoconfiguration)
	 * @param config Configuration of the vector store
	 * @param client Embedding client
	 */
	public AstraDBVectorStore(Database db, AstraDBVectorStoreConfig config, EmbeddingClient client) {
		// Create the collection if it does not exist
		this.astraDBCollection = db.createCollection(config.getName(),
				CollectionOptions.builder()
					.vector(config.getDimension(), SimilarityMetric.valueOf(config.getSimilarity()))
					.indexingDeny(config.getIndexingDeny())
					.build());

		this.concurrentThreads = config.getConcurrency();
		this.operationTimeout = config.getTimeoutMillis();

		// Set the insertManyOptions
		this.insertManyOptions = new InsertManyOptions().concurrency(concurrentThreads)
			.timeout(operationTimeout)
			.ordered(false);

		// Set the embedding client
		this.embeddingClient = client;
	}

	/**
	 * Mapping Spring => Astra
	 * @param doc spring document
	 * @return astra document
	 */
	private Document mapSpring2AstraDocument(org.springframework.ai.document.Document doc) {
		Document astraDoc = new Document();
		astraDoc.id(doc.getId());
		astraDoc.putAll(doc.getMetadata());
		// Map Vector
		if (doc.getEmbedding() != null) {
			float[] floatArray = new float[doc.getEmbedding().size()];
			for (int i = 0; i < doc.getEmbedding().size(); i++) {
				floatArray[i] = doc.getEmbedding().get(i).floatValue();
			}
			astraDoc.vector(floatArray);
		}
		astraDoc.append(CONTENT, doc.getContent());

		return astraDoc;
	}

	/**
	 * Mapping Astra => Spring
	 * @param doc astra document
	 * @return spring document
	 */
	private org.springframework.ai.document.Document mapAstra2SpringDocument(Document doc) {
		return null;
	}

	/** {@inheritDoc} */
	@Override
	public void add(List<org.springframework.ai.document.Document> documents) {
		astraDBCollection.insertMany(documents.stream().map(this::mapSpring2AstraDocument).toList(), insertManyOptions);
	}

	/** {@inheritDoc} */
	@Override
	public Optional<Boolean> delete(List<String> idList) {
		long start = System.currentTimeMillis();
		ExecutorService executor = Executors.newFixedThreadPool(concurrentThreads);
		try {
			idList.forEach(id -> executor.submit(() -> astraDBCollection.deleteOne(eq(id))));
			if (executor.awaitTermination(operationTimeout, TimeUnit.MILLISECONDS)) {
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
		return Optional.empty();
	}

	/** {@inheritDoc} */
	@Override
	public List<org.springframework.ai.document.Document> similaritySearch(SearchRequest request) {
		// request.getFilterExpression();
		Filter filter = null;
		;

		return astraDBCollection.find(filter, FindOptions.Builder.limit(request.getTopK()).includeSimilarity())
			.all()
			.stream()
			.filter(r -> r.getSimilarity().isPresent() && r.getSimilarity().get() >= request.getSimilarityThreshold())
			.map(this::mapAstra2SpringDocument)
			.toList();
	}

}