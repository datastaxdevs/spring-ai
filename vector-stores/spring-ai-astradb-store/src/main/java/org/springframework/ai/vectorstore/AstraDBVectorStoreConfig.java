package org.springframework.ai.vectorstore;

import com.datastax.astra.client.DataAPIOptions;
import com.datastax.astra.client.model.CollectionOptions;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;

/**
 * Configuration for the AstraDB Vector Store.
 *
 * AstraDB is a cloud-native database built on Apache Cassandra. It is a
 * database-as-a-service that is secure, highly available, and elastically scalable. On a
 * tenant you can have one-to-many databases, and on a database you can have one-to-many
 * collections. A SpringAI VectorStore is a collection that stores embeddings.
 */
@Getter
@Setter
public class AstraDBVectorStoreConfig {

	/**
	 * Operation timeout
	 */
	public static final Integer DEFAULT_OPERATION_TIMEOUT_IN_SECONDS = 30;

	/**
	 * Attribute used to store the chunk of data
	 */
	public static final String DEFAULT_ATTRIBUTE_EMBED = "embed";

	/**
	 * Token used as credentials to connect
	 */
	private final String token;

	/**
	 * Attribute of the collection that will store the chunk used to vectorized.
	 */
	private final String embed;

	/**
	 * Namespace used to connect to a particular DB.
	 */
	private final String namespace;

	/**
	 * Http url fo the API to connect to a particular DB.
	 */
	private final String apiEndpoint;

	/**
	 * Flag to enable log for low level request to the database
	 */
	private final boolean enableLogging;

	/**
	 * Options to configure the Data API client
	 */
	private final DataAPIOptions dataAPIOptions;

	/**
	 * Flag to enforce the creation of the collection is not exist
	 */
	private final boolean initializeSchema;

	/**
	 * Name of the collection in the database
	 */
	private final String collectionName;

	/**
	 * Fine-Tuning of the collection with vector and indexation.
	 */
	private final CollectionOptions collectionOptions;

	/**
	 * Help to create an Embedding Store for AstraDB.
	 * @return builder for AstraDB
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Default constructor.
	 * @param builder to have field as read-only
	 */
	private AstraDBVectorStoreConfig(Builder builder) {
		// Connection
		this.token = builder.token;
		this.apiEndpoint = builder.apiEndpoint;
		this.dataAPIOptions = builder.dataAPIOptions;
		this.enableLogging = builder.enableLogging;
		this.namespace = builder.namespace;

		// Access to the collection
		this.collectionName = builder.name;
		this.embed = builder.embed;
		this.initializeSchema = builder.initializeSchema;
		this.collectionOptions = builder.collectionOptions;
	}

	/**
	 * Builder for the configuration
	 */
	public static class Builder {

		public static final String DEFAULT_NAME = "vector_store";

		public static final String DEFAULT_NAMESPACE = "default_keyspace";

		public static final String[] DEFAULT_DENY = new String[] { "content" };

		private String token;

		private String embed = DEFAULT_ATTRIBUTE_EMBED;

		private String apiEndpoint;

		private String name = DEFAULT_NAME;

		private String namespace = DEFAULT_NAMESPACE;

		private boolean enableLogging = false;

		private boolean initializeSchema = true;

		private DataAPIOptions dataAPIOptions;

		private CollectionOptions collectionOptions;

		public AstraDBVectorStoreConfig build() {
			if (dataAPIOptions == null) {
				dataAPIOptions = DataAPIOptions.builder().build();
			}
			if (collectionOptions == null) {
				collectionOptions = CollectionOptions.builder().indexingDeny(embed).build();
			}
			if (this.token == null || this.apiEndpoint == null) {
				throw new IllegalArgumentException(
						"Token and API Endpoint must not be null, please review your configuration.");
			}
			return new AstraDBVectorStoreConfig(this);
		}

		public Builder withCollectionName(String name) {
			Assert.hasLength(name, "Collection name must not be empty");
			this.name = name;
			return this;
		}

		public Builder withDataAPIOptions(DataAPIOptions dataAPIOptions) {
			Assert.notNull(dataAPIOptions, "Data API Options must not be null");
			this.dataAPIOptions = dataAPIOptions;
			return this;
		}

		public Builder withCollectionOptions(CollectionOptions collectionOptions) {
			Assert.notNull(collectionOptions, "Collection Options must not be null");
			this.collectionOptions = collectionOptions;
			return this;
		}

		public Builder withInitializeSchema() {
			this.initializeSchema = true;
			return this;
		}

		public Builder withoutInitializeSchema() {
			this.initializeSchema = false;
			return this;
		}

		public Builder withEnableLogging() {
			this.enableLogging = true;
			return this;
		}

		public Builder withoutEnableLogging() {
			this.enableLogging = false;
			return this;
		}

		public Builder withEmbed(String embed) {
			Assert.hasLength(embed, "Embedding attribute must not be empty");
			this.embed = embed;
			return this;
		}

		public Builder withToken(String token) {
			Assert.hasLength(token, "token Endpoint must not be empty");
			this.token = token;
			return this;
		}

		public Builder withApiEndpoint(String apiEndpoint) {
			Assert.hasLength(apiEndpoint, "API Endpoint must not be empty");
			this.apiEndpoint = apiEndpoint;
			return this;
		}

		public Builder withNamespace(String namespace) {
			Assert.hasLength(namespace, "Namespace must not be empty");
			this.namespace = namespace;
			return this;
		}

	}

}
