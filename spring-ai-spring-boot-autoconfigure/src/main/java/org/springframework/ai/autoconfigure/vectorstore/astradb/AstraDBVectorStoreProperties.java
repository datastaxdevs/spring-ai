package org.springframework.ai.autoconfigure.vectorstore.astradb;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.ai.vectorstore.AstraDBVectorStoreConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration for AstraDB.
 *
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * {@code
 * spring.ai.astradb:
 *   token: token
 *   api-endpoint: http://dbid-dbregion.apps.astra.datastax.com
 *   namespace: default_keyspace
 *   verbose: true
 *   request-timeout: 10s
 *   collection:
 *     name: my_collection
 *     dimension: 1536
 *     similarity: cosine
 *     initialize-schema: true
 *     indexing-deny: []
 *     indexing-allow: []
 *     embedding-provider: openai
 *     embedding-model: text-embedding-ada-002
 *     default-id: uuid
 * }
 * </pre>
 *
 */
@ConfigurationProperties(AstraDBVectorStoreProperties.CONFIG_PREFIX)
@Getter
@Setter
@NoArgsConstructor
public class AstraDBVectorStoreProperties {

	public static final String CONFIG_PREFIX = "spring.ai.vectorstore.astradb";

	/**
	 * Token act as your credentials
	 */
	private String token;

	/**
	 * The database endpoint
	 */
	private String apiEndpoint;

	/**
	 * The namespace is use
	 */
	private String namespace;

	/**
	 * Enabling logging
	 */
	private boolean verbose = false;

	/**
	 * Initialize Schema
	 */
	private boolean initializeSchema = true;

	/**
	 * Configuration for the collection.
	 */
	private CollectionConfig collection;

	@Getter
	@Setter
	@NoArgsConstructor
	public static class CollectionConfig {

		private String name;

		private Integer dimension;

		private String similarity;

		private String defaultId;

		private String[] indexingDeny;

		private String[] indexingAllow;

		private String embeddingProvider;

		private String embeddingModel;

	}

}
