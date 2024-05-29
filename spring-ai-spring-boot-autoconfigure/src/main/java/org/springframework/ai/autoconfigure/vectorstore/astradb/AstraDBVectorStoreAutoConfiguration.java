package org.springframework.ai.autoconfigure.vectorstore.astradb;

import com.datastax.astra.client.DataAPIClient;
import com.datastax.astra.client.model.CollectionIdTypes;
import com.datastax.astra.client.model.CollectionOptions;
import com.datastax.astra.client.model.SimilarityMetric;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.AstraDBVectorStore;
import org.springframework.ai.vectorstore.AstraDBVectorStoreConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass({ DataAPIClient.class })
@EnableConfigurationProperties({ AstraDBVectorStoreProperties.class })
public class AstraDBVectorStoreAutoConfiguration {

	/**
	 * Initialization of the client
	 * @param connectionProperties configuration
	 * @return client
	 */
	@Bean
	@ConditionalOnMissingBean
	public AstraDBVectorStoreConfig loadConfiguration(AstraDBVectorStoreProperties connectionProperties) {
		AstraDBVectorStoreConfig.Builder config = AstraDBVectorStoreConfig.builder();
		config.withToken(connectionProperties.getToken());
		config.withApiEndpoint(connectionProperties.getApiEndpoint());
		config.withNamespace(connectionProperties.getNamespace());
		if (connectionProperties.isInitializeSchema()) {
			config.withInitializeSchema();
		}
		else {
			config.withoutInitializeSchema();
		}
		if (connectionProperties.isVerbose()) {
			config.withEnableLogging();
		}
		else {
			config.withoutEnableLogging();
		}

		// Collection Information
		CollectionOptions collectionOptions = new CollectionOptions();
		if (connectionProperties.getCollection() != null) {
			CollectionOptions.CollectionOptionsBuilder collectionsOptionsBuilder = CollectionOptions.builder();
			AstraDBVectorStoreProperties.CollectionConfig colConf = connectionProperties.getCollection();
			config.withCollectionName(colConf.getName());
			config.withNamespace(connectionProperties.getNamespace());
			// Indexing Options
			if (colConf.getIndexingAllow() != null && colConf.getIndexingAllow().length > 0) {
				collectionsOptionsBuilder.indexingAllow(colConf.getIndexingAllow());
			}
			if (colConf.getIndexingDeny() != null && colConf.getIndexingDeny().length > 0) {
				collectionsOptionsBuilder.indexingDeny(colConf.getIndexingDeny());
			}
			// Default id options
			if (colConf.getDefaultId() != null) {
				collectionsOptionsBuilder.defaultIdType(CollectionIdTypes.fromValue(colConf.getDefaultId()));
			}
			// Vector Options
			if (colConf.getDimension() != null) {
				collectionsOptionsBuilder.vectorDimension(colConf.getDimension());
			}
			if (colConf.getSimilarity() != null) {
				collectionsOptionsBuilder.vectorSimilarity(SimilarityMetric.fromValue(colConf.getSimilarity()));
			}
			// Vectorized Options
			if (colConf.getEmbeddingModel() != null && colConf.getEmbeddingProvider() != null) {
				CollectionOptions.Service service = new CollectionOptions.Service();
				service.setProvider(colConf.getEmbeddingProvider());
				service.setModelName(colConf.getEmbeddingModel());
			}
			config.withCollectionOptions(collectionsOptionsBuilder.build());

		}
		return config.build();
	}

	/**
	 * Initialization of the Store.
	 * @param config connection properties
	 * @param embeddingModel embedding client
	 * @return our embedding store
	 */
	@Bean
	@ConditionalOnMissingBean
	public AstraDBVectorStore vectorStore(AstraDBVectorStoreConfig config, EmbeddingModel embeddingModel) {
		// Vectorize is in use => we do not provide the embedding Model.
		if (config.getCollectionOptions() != null && config.getCollectionOptions().getVector() != null
				&& config.getCollectionOptions().getVector().getService() != null
				&& config.getCollectionOptions().getVector().getService().getModelName() != null) {
			return new AstraDBVectorStore(config, null);
		}
		return new AstraDBVectorStore(config, embeddingModel);
	}

}
