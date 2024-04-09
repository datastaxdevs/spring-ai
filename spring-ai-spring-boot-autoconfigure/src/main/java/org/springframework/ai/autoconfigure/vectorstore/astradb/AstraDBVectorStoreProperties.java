package org.springframework.ai.autoconfigure.vectorstore.astradb;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.ai.vectorstore.AstraDBVectorStoreConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
 *   api-endpoint: apiEndpoint
 *   namespace: default_keyspace
 *   collection:
 *     name: collectionName
 *     dimension: 512
 *     similarity: COSINE
 *     indexing-deny: [content]
 *     timeout-millis: 100000
 *   http-client:
 *     http-version: 1.1
 *     http-redirect: NORMAL
 *     connect-timeout-seconds: 10
 *     read-timeout-second: 10
 *     proxy:
 *       host: localhost
 *       port: 8080
 *     retries:
 *       count: 3
 *       delays-millis: 100
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

	/** Configuration for the client. */
	private HttpClientConfig httpClient;

	/** Configuration for the collection. */
	private AstraDBVectorStoreConfig collection;

	@Getter
	@Setter
	@NoArgsConstructor
	public static class HttpClientConfig {

		private String httpVersion;

		private String httpRedirect;

		private int connectTimeoutSeconds;

		private int readTimeoutSeconds;

		private Proxy proxy;

		private Retries retries;

	}

	@Getter
	@Setter
	@NoArgsConstructor
	public static class Proxy {

		private String host;

		private int port;

	}

	@Getter
	@Setter
	@NoArgsConstructor
	public static class Retries {

		private int count;

		private int delaysMillis;

	}

}
