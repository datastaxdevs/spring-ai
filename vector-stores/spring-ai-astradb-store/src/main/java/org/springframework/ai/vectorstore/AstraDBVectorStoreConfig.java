package org.springframework.ai.vectorstore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AstraDBVectorStoreConfig {

	/** how many threads to bul insert. */
	public static final int DEFAULT_CONCURRENCY = 8;

	/** 30s for bulk inserts */
	public static final int DEFAULT_TIMEOUT = 30000;

	public static final String[] DEFAULT_DENY = new String[] { "content" };

	public static final String DEFAULT_SIMILARITY = "COSINE";

	public static final String DEFAULT_NAME = "vector_store";

	public static final int DEFAULT_DIMENSION = 1536;

	private String name;

	private int dimension;

	private String similarity;

	private String[] indexingDeny;

	private int timeoutMillis;

	private int concurrency;

	/**
	 * Default constructor (autoconfiguration)
	 */
	public AstraDBVectorStoreConfig() {
	}

	/**
	 * Default constructor
	 */
	private AstraDBVectorStoreConfig(Builder builder) {
		this.name = builder.name;
		this.dimension = builder.dimension;
		this.similarity = builder.similarity;
		this.indexingDeny = builder.indexingDeny;
		this.timeoutMillis = builder.timeoutMillis;
		this.concurrency = builder.concurrency;
	}

	/**
	 * Builder for the configuration
	 */
	public static class Builder {

		private String name = DEFAULT_NAME;

		private int dimension = DEFAULT_DIMENSION;

		private String similarity = DEFAULT_SIMILARITY;

		private String[] indexingDeny = DEFAULT_DENY;

		private int timeoutMillis = DEFAULT_TIMEOUT;

		private int concurrency = DEFAULT_CONCURRENCY;

		public Builder() {
		}

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder dimension(int dimension) {
			this.dimension = dimension;
			return this;
		}

		public Builder similarity(String similarity) {
			this.similarity = similarity;
			return this;
		}

		public Builder indexingDeny(String[] indexingDeny) {
			this.indexingDeny = indexingDeny;
			return this;
		}

		public Builder concurrency(int concurrency) {
			this.concurrency = concurrency;
			return this;
		}

		public Builder timeoutMillis(int timeoutMillis) {
			this.timeoutMillis = timeoutMillis;
			return this;
		}

		public AstraDBVectorStoreConfig build() {
			return new AstraDBVectorStoreConfig(this);
		}

	}

}
